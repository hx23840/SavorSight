use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum LearningSessionStatus {
    Created,
    Capturing,
    Uploaded,
    Processing,
    DraftReady,
    Failed,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LearningSession {
    pub session_id: Uuid,
    pub device_id: Option<String>,
    pub source_mode: String,
    pub capture_policy: String,
    pub status: LearningSessionStatus,
    pub raw_video_chunks: u64,
    pub raw_audio_chunks: u64,
    pub status_events: u64,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateLearningSessionRequest {
    pub device_id: Option<String>,
    pub source_mode: Option<String>,
    pub capture_policy: Option<String>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateLearningSessionResponse {
    pub session_id: Uuid,
    pub upload_endpoint: String,
    pub status: LearningSessionStatus,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FinishLearningSessionResponse {
    pub session_id: Uuid,
    pub status: LearningSessionStatus,
    pub raw_video_chunks: u64,
    pub raw_audio_chunks: u64,
}

impl LearningSession {
    pub fn new(request: CreateLearningSessionRequest) -> Self {
        let now = Utc::now();

        Self {
            session_id: Uuid::new_v4(),
            device_id: request.device_id,
            source_mode: request
                .source_mode
                .unwrap_or_else(|| "glasses_first_person_stream".to_string()),
            capture_policy: request
                .capture_policy
                .unwrap_or_else(|| "raw_stream_upload_only".to_string()),
            status: LearningSessionStatus::Created,
            raw_video_chunks: 0,
            raw_audio_chunks: 0,
            status_events: 0,
            created_at: now,
            updated_at: now,
        }
    }

    pub fn has_capture_data(&self) -> bool {
        self.raw_video_chunks > 0 || self.raw_audio_chunks > 0
    }

    pub fn touch(&mut self) {
        self.updated_at = Utc::now();
    }
}
