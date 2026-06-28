use axum::{
    Json, Router,
    extract::{
        Path, State, WebSocketUpgrade,
        ws::{Message, WebSocket},
    },
    response::IntoResponse,
    routing::{get, post},
};
use chrono::Utc;
use futures_util::StreamExt;
use uuid::Uuid;

use crate::{
    app_state::AppState,
    error::{AppError, AppResult},
    models::session::{
        CreateLearningSessionRequest, CreateLearningSessionResponse, FinishLearningSessionResponse,
        LearningSession, LearningSessionStatus,
    },
    services::storage::{RawStreamKind, append_raw_chunk, ensure_session_dir},
};

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/api/learning-sessions", post(create_learning_session))
        .route(
            "/api/learning-sessions/:session_id",
            get(get_learning_session),
        )
        .route(
            "/api/learning-sessions/:session_id/raw-stream",
            get(raw_stream),
        )
        .route(
            "/api/learning-sessions/:session_id/finish",
            post(finish_learning_session),
        )
        .route(
            "/api/learning-sessions/:session_id/recipe-draft",
            get(get_recipe_draft),
        )
}

async fn create_learning_session(
    State(state): State<AppState>,
    Json(request): Json<CreateLearningSessionRequest>,
) -> AppResult<Json<CreateLearningSessionResponse>> {
    let session = LearningSession::new(request);
    let session_id = session.session_id;
    let session_dir = state.session_dir(session_id);
    ensure_session_dir(&session_dir).await?;

    {
        let mut sessions = state.sessions.write().expect("sessions lock poisoned");
        sessions.insert(session_id, session.clone());
    }

    Ok(Json(CreateLearningSessionResponse {
        session_id,
        upload_endpoint: format!("/api/learning-sessions/{session_id}/raw-stream"),
        status: session.status,
    }))
}

async fn get_learning_session(
    State(state): State<AppState>,
    Path(session_id): Path<Uuid>,
) -> AppResult<Json<LearningSession>> {
    let sessions = state.sessions.read().expect("sessions lock poisoned");
    let session = sessions
        .get(&session_id)
        .cloned()
        .ok_or(AppError::SessionNotFound)?;

    Ok(Json(session))
}

async fn raw_stream(
    State(state): State<AppState>,
    Path(session_id): Path<Uuid>,
    ws: WebSocketUpgrade,
) -> AppResult<impl IntoResponse> {
    ensure_session_exists(&state, session_id)?;

    Ok(ws.on_upgrade(move |socket| handle_socket(state, session_id, socket)))
}

async fn handle_socket(state: AppState, session_id: Uuid, mut socket: WebSocket) {
    mark_session_status(&state, session_id, LearningSessionStatus::Capturing);
    let mut failed = false;

    while let Some(message) = socket.next().await {
        match message {
            Ok(Message::Binary(bytes)) => {
                let session_dir = state.session_dir(session_id);
                match append_raw_chunk(&session_dir, &bytes).await {
                    Ok(stored) => {
                        tracing::info!(
                            session_id = %session_id,
                            bytes = stored.bytes,
                            path = %stored.path.display(),
                            "stored raw stream chunk"
                        );
                        increment_chunk_counter(&state, session_id, stored.kind);
                    }
                    Err(error) => {
                        tracing::error!(session_id = %session_id, error = %error, "failed to store chunk");
                        mark_session_status(&state, session_id, LearningSessionStatus::Failed);
                        failed = true;
                        break;
                    }
                }
            }
            Ok(Message::Text(text)) => {
                let session_dir = state.session_dir(session_id);
                let mut bytes = b"device_status\n".to_vec();
                bytes.extend_from_slice(text.as_bytes());
                if let Ok(stored) = append_raw_chunk(&session_dir, &bytes).await {
                    increment_chunk_counter(&state, session_id, stored.kind);
                }
            }
            Ok(Message::Close(_)) => break,
            Ok(_) => {}
            Err(error) => {
                tracing::error!(session_id = %session_id, error = %error, "websocket error");
                mark_session_status(&state, session_id, LearningSessionStatus::Failed);
                failed = true;
                break;
            }
        }
    }

    if !failed {
        mark_session_status(&state, session_id, LearningSessionStatus::Uploaded);
    }
}

async fn finish_learning_session(
    State(state): State<AppState>,
    Path(session_id): Path<Uuid>,
) -> AppResult<Json<FinishLearningSessionResponse>> {
    let mut sessions = state.sessions.write().expect("sessions lock poisoned");
    let session = sessions
        .get_mut(&session_id)
        .ok_or(AppError::SessionNotFound)?;

    if !session.has_capture_data() {
        return Err(AppError::NoCaptureData);
    }

    session.status = LearningSessionStatus::Processing;
    session.touch();

    Ok(Json(FinishLearningSessionResponse {
        session_id,
        status: session.status.clone(),
        raw_video_chunks: session.raw_video_chunks,
        raw_audio_chunks: session.raw_audio_chunks,
    }))
}

async fn get_recipe_draft(
    State(state): State<AppState>,
    Path(session_id): Path<Uuid>,
) -> AppResult<Json<crate::models::recipe::Recipe>> {
    let session = {
        let sessions = state.sessions.read().expect("sessions lock poisoned");
        sessions
            .get(&session_id)
            .cloned()
            .ok_or(AppError::SessionNotFound)?
    };

    if !matches!(
        session.status,
        LearningSessionStatus::Processing | LearningSessionStatus::DraftReady
    ) {
        return Err(AppError::SessionNotFinished);
    }

    let recipe = state.qwen_vl.generate_recipe_from_session(&session).await?;
    {
        let mut recipes = state.recipes.write().expect("recipes lock poisoned");
        recipes.insert(recipe.recipe_id.clone(), recipe.clone());
    }
    mark_session_status(&state, session_id, LearningSessionStatus::DraftReady);

    Ok(Json(recipe))
}

fn ensure_session_exists(state: &AppState, session_id: Uuid) -> AppResult<()> {
    let sessions = state.sessions.read().expect("sessions lock poisoned");
    if sessions.contains_key(&session_id) {
        Ok(())
    } else {
        Err(AppError::SessionNotFound)
    }
}

fn mark_session_status(state: &AppState, session_id: Uuid, status: LearningSessionStatus) {
    let mut sessions = state.sessions.write().expect("sessions lock poisoned");
    if let Some(session) = sessions.get_mut(&session_id) {
        session.status = status;
        session.updated_at = Utc::now();
    }
}

fn increment_chunk_counter(state: &AppState, session_id: Uuid, kind: RawStreamKind) {
    let mut sessions = state.sessions.write().expect("sessions lock poisoned");
    if let Some(session) = sessions.get_mut(&session_id) {
        match kind {
            RawStreamKind::Video => session.raw_video_chunks += 1,
            RawStreamKind::Audio => session.raw_audio_chunks += 1,
            RawStreamKind::Status => session.status_events += 1,
            RawStreamKind::Unknown => {}
        }
        session.touch();
    }
}
