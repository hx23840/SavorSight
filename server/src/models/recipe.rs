use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Recipe {
    pub recipe_id: String,
    pub source_session_id: Uuid,
    pub dish_name: String,
    pub confidence: f32,
    pub servings: Option<u32>,
    pub estimated_time_minutes: Option<u32>,
    pub ingredients: Vec<Ingredient>,
    pub seasonings: Vec<Seasoning>,
    pub steps: Vec<RecipeStep>,
    pub uncertain_fields: Vec<UncertainField>,
    #[serde(default)]
    pub confirmed: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Ingredient {
    pub name: String,
    pub amount: String,
    pub prep: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Seasoning {
    pub name: String,
    pub amount: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RecipeStep {
    pub id: String,
    pub title: String,
    pub instruction: String,
    pub heat: Option<String>,
    pub target_state: Option<String>,
    pub checkable: bool,
    pub confidence: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UncertainField {
    pub field: String,
    pub question: String,
}
