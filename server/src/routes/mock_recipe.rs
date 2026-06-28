use axum::{Json, Router, extract::State, routing::get};

use crate::{
    app_state::AppState, models::recipe::Recipe, services::recipe_builder::build_mock_recipe,
};

pub fn router() -> Router<AppState> {
    Router::new().route("/api/mock/recipe-draft", get(get_mock_recipe))
}

async fn get_mock_recipe(State(state): State<AppState>) -> Json<Recipe> {
    let recipe = build_mock_recipe();

    {
        let mut recipes = state.recipes.write().expect("recipes lock poisoned");
        recipes.insert(recipe.recipe_id.clone(), recipe.clone());
    }

    Json(recipe)
}
