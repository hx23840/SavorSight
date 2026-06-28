use axum::{Json, Router, extract::{Path, State}, routing::{get, post}};

use crate::{
    app_state::AppState,
    error::{AppError, AppResult},
};

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/api/recipes", get(list_recipes))
        .route("/api/recipes/:recipe_id", get(get_recipe))
        .route("/api/recipes/:recipe_id/confirm", post(confirm_recipe))
}

async fn list_recipes(
    State(state): State<AppState>,
) -> AppResult<Json<Vec<crate::models::recipe::Recipe>>> {
    let recipes = state.recipes.read().expect("recipes lock poisoned");
    let list: Vec<_> = recipes
        .values()
        .filter(|r| r.confirmed)
        .cloned()
        .collect();

    Ok(Json(list))
}

async fn get_recipe(
    State(state): State<AppState>,
    Path(recipe_id): Path<String>,
) -> AppResult<Json<crate::models::recipe::Recipe>> {
    let recipes = state.recipes.read().expect("recipes lock poisoned");
    let recipe = recipes
        .get(&recipe_id)
        .cloned()
        .ok_or(AppError::RecipeNotFound)?;

    Ok(Json(recipe))
}

async fn confirm_recipe(
    State(state): State<AppState>,
    Path(recipe_id): Path<String>,
) -> AppResult<Json<ConfirmRecipeResponse>> {
    let mut recipes = state.recipes.write().expect("recipes lock poisoned");
    let recipe = recipes
        .get_mut(&recipe_id)
        .ok_or(AppError::RecipeNotFound)?;

    recipe.confirmed = true;

    Ok(Json(ConfirmRecipeResponse {
        recipe_id: recipe_id,
        confirmed: true,
    }))
}

#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
struct ConfirmRecipeResponse {
    recipe_id: String,
    confirmed: bool,
}
