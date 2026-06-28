use async_trait::async_trait;
use uuid::Uuid;

use crate::{
    error::AppResult,
    models::{
        check::{CheckResult, CheckStatus},
        recipe::Recipe,
        session::LearningSession,
    },
    services::recipe_builder::build_draft_from_captured_session,
};

#[async_trait]
pub trait QwenVlClient: Send + Sync {
    async fn generate_recipe_from_session(&self, session: &LearningSession) -> AppResult<Recipe>;

    async fn check_step_image(
        &self,
        recipe_id: &str,
        step_id: &str,
        image_bytes: Vec<u8>,
        target_state: String,
    ) -> AppResult<CheckResult>;
}

#[derive(Debug, Default)]
pub struct MockQwenVlClient;

#[async_trait]
impl QwenVlClient for MockQwenVlClient {
    async fn generate_recipe_from_session(&self, session: &LearningSession) -> AppResult<Recipe> {
        build_draft_from_captured_session(session)
    }

    async fn check_step_image(
        &self,
        recipe_id: &str,
        step_id: &str,
        image_bytes: Vec<u8>,
        target_state: String,
    ) -> AppResult<CheckResult> {
        Ok(mock_check_result(
            recipe_id,
            step_id,
            image_bytes.len(),
            target_state,
        ))
    }
}

fn mock_check_result(
    recipe_id: &str,
    step_id: &str,
    image_bytes: usize,
    target_state: String,
) -> CheckResult {
    let confidence = if image_bytes > 0 { 0.62 } else { 0.0 };

    CheckResult {
        status: CheckStatus::Continue,
        confidence,
        summary: format!(
            "已收到菜谱 {recipe_id} / 步骤 {step_id} 的检查图片，目标状态是：{target_state}。"
        ),
        suggestion: "当前为 mock 判断：继续当前步骤 20 秒后再次检查。".to_string(),
        tts: "继续当前步骤二十秒后再次检查。".to_string(),
    }
}

#[allow(dead_code)]
#[derive(Debug, Clone)]
pub struct QwenVlRequestContext {
    pub session_id: Option<Uuid>,
    pub recipe_id: Option<String>,
    pub step_id: Option<String>,
}
