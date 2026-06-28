use uuid::Uuid;

use crate::{
    error::{AppError, AppResult},
    models::{
        recipe::{Ingredient, Recipe, RecipeStep, Seasoning, UncertainField},
        session::LearningSession,
    },
};

pub fn build_draft_from_captured_session(session: &LearningSession) -> AppResult<Recipe> {
    if !session.has_capture_data() {
        return Err(AppError::NoCaptureData);
    }

    Ok(mock_recipe(session.session_id))
}

pub fn build_mock_recipe() -> Recipe {
    let mut recipe = mock_recipe(Uuid::nil());
    recipe.recipe_id = "mock-recipe".to_string();
    recipe
}

fn mock_recipe(session_id: Uuid) -> Recipe {
    Recipe {
        recipe_id: format!("recipe-{}", session_id),
        source_session_id: session_id,
        dish_name: "番茄炒蛋".to_string(),
        confidence: 0.72,
        servings: Some(2),
        estimated_time_minutes: Some(15),
        ingredients: vec![
            Ingredient {
                name: "番茄".to_string(),
                amount: "2 个".to_string(),
                prep: Some("切块".to_string()),
            },
            Ingredient {
                name: "鸡蛋".to_string(),
                amount: "3 个".to_string(),
                prep: Some("打散".to_string()),
            },
        ],
        seasonings: vec![
            Seasoning {
                name: "盐".to_string(),
                amount: "少许".to_string(),
            },
            Seasoning {
                name: "生抽".to_string(),
                amount: "待确认".to_string(),
            },
        ],
        steps: vec![
            RecipeStep {
                id: "step-01".to_string(),
                title: "炒鸡蛋".to_string(),
                instruction: "鸡蛋液下锅，炒到半凝固后盛出。".to_string(),
                heat: Some("中火".to_string()),
                target_state: Some("鸡蛋半凝固，表面仍略湿润。".to_string()),
                checkable: true,
                confidence: 0.78,
            },
            RecipeStep {
                id: "step-02".to_string(),
                title: "炒番茄".to_string(),
                instruction: "番茄下锅炒出汁，再加入鸡蛋翻炒。".to_string(),
                heat: Some("中火".to_string()),
                target_state: Some("番茄明显出汁，锅底有红色汤汁。".to_string()),
                checkable: true,
                confidence: 0.74,
            },
        ],
        uncertain_fields: vec![UncertainField {
            field: "生抽用量".to_string(),
            question: "视频中用量不清楚，是否按 1 勺保存？".to_string(),
        }],
    }
}
