pub mod checks;
pub mod learning_sessions;
pub mod mock_recipe;
pub mod recipes;

use axum::{Router, routing::get};

use crate::app_state::AppState;

pub fn router() -> Router<AppState> {
    Router::new()
        .merge(mock_recipe::router())
        .merge(learning_sessions::router())
        .merge(recipes::router())
        .merge(checks::router())
        .route("/health", get(health))
}

async fn health() -> &'static str {
    "ok"
}
