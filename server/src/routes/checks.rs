use axum::{
    Json, Router,
    extract::{Multipart, Path, State},
    routing::post,
};

use crate::{
    app_state::AppState,
    error::{AppError, AppResult},
    models::check::CheckResult,
};

pub fn router() -> Router<AppState> {
    Router::new().route(
        "/api/recipes/:recipe_id/steps/:step_id/check",
        post(check_step),
    )
}

async fn check_step(
    State(state): State<AppState>,
    Path((recipe_id, step_id)): Path<(String, String)>,
    mut multipart: Multipart,
) -> AppResult<Json<CheckResult>> {
    ensure_recipe_exists(&state, &recipe_id)?;

    let mut image_bytes = Vec::new();
    let mut target_state = None;

    while let Some(field) = multipart.next_field().await.map_err(|error| {
        AppError::InvalidRequest(format!("failed to parse multipart field: {error}"))
    })? {
        let name = field.name().unwrap_or_default().to_string();

        match name.as_str() {
            "image" => {
                let bytes = field.bytes().await.map_err(|error| {
                    AppError::InvalidRequest(format!("failed to read image field: {error}"))
                })?;
                image_bytes = bytes.to_vec();
            }
            "targetState" => {
                target_state = Some(field.text().await.map_err(|error| {
                    AppError::InvalidRequest(format!("failed to read targetState field: {error}"))
                })?);
            }
            _ => {}
        }
    }

    if image_bytes.is_empty() {
        return Err(AppError::InvalidRequest(
            "multipart field `image` is required".to_string(),
        ));
    }

    let target_state = target_state.unwrap_or_else(|| "当前步骤目标状态未提供".to_string());

    let result = state
        .qwen_vl
        .check_step_image(&recipe_id, &step_id, image_bytes, target_state)
        .await?;

    Ok(Json(result))
}

fn ensure_recipe_exists(state: &AppState, recipe_id: &str) -> AppResult<()> {
    let recipes = state.recipes.read().expect("recipes lock poisoned");
    if recipes.contains_key(recipe_id) {
        Ok(())
    } else {
        Err(AppError::InvalidRequest(format!(
            "recipe `{recipe_id}` not found; request recipe draft first"
        )))
    }
}
