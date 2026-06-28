use axum::{
    Json,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde::Serialize;

#[derive(Debug, thiserror::Error)]
pub enum AppError {
    #[error("session not found")]
    SessionNotFound,
    #[error("session has no captured raw stream data")]
    NoCaptureData,
    #[error("session is not finished")]
    SessionNotFinished,
    #[error("recipe not found")]
    RecipeNotFound,
    #[error("invalid request: {0}")]
    InvalidRequest(String),
    #[error("io error: {0}")]
    Io(#[from] std::io::Error),
}

#[derive(Serialize)]
struct ErrorBody {
    code: &'static str,
    message: String,
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, code) = match self {
            AppError::SessionNotFound => (StatusCode::NOT_FOUND, "session_not_found"),
            AppError::NoCaptureData => (StatusCode::CONFLICT, "no_capture_data"),
            AppError::SessionNotFinished => (StatusCode::CONFLICT, "session_not_finished"),
            AppError::InvalidRequest(_) => (StatusCode::BAD_REQUEST, "invalid_request"),
            AppError::Io(_) => (StatusCode::INTERNAL_SERVER_ERROR, "io_error"),
        };

        let body = Json(ErrorBody {
            code,
            message: self.to_string(),
        });

        (status, body).into_response()
    }
}

pub type AppResult<T> = Result<T, AppError>;
