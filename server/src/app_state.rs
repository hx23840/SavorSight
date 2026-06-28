use std::{
    collections::HashMap,
    path::PathBuf,
    sync::{Arc, RwLock},
};

use uuid::Uuid;

use crate::{
    models::{recipe::Recipe, session::LearningSession},
    services::qwen_vl::{MockQwenVlClient, QwenVlClient},
};

#[derive(Clone)]
pub struct AppState {
    pub sessions: Arc<RwLock<HashMap<Uuid, LearningSession>>>,
    pub recipes: Arc<RwLock<HashMap<String, Recipe>>>,
    pub qwen_vl: Arc<dyn QwenVlClient>,
    pub data_dir: PathBuf,
}

impl AppState {
    pub fn new(data_dir: impl Into<PathBuf>) -> Self {
        Self {
            sessions: Arc::new(RwLock::new(HashMap::new())),
            recipes: Arc::new(RwLock::new(HashMap::new())),
            qwen_vl: Arc::new(MockQwenVlClient),
            data_dir: data_dir.into(),
        }
    }

    pub fn session_dir(&self, session_id: Uuid) -> PathBuf {
        self.data_dir.join("sessions").join(session_id.to_string())
    }
}
