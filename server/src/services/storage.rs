use std::path::{Path, PathBuf};

use tokio::{
    fs::{self, OpenOptions},
    io::AsyncWriteExt,
};

use crate::error::AppResult;

#[derive(Debug, Clone, Copy)]
pub enum RawStreamKind {
    Video,
    Audio,
    Status,
    Unknown,
}

impl RawStreamKind {
    pub fn from_prefix(prefix: &[u8]) -> Self {
        match prefix {
            b"raw_video" => Self::Video,
            b"raw_audio" => Self::Audio,
            b"device_status" => Self::Status,
            _ => Self::Unknown,
        }
    }

    pub fn file_name(self) -> &'static str {
        match self {
            Self::Video => "raw_video.bin",
            Self::Audio => "raw_audio.bin",
            Self::Status => "device_status.jsonl",
            Self::Unknown => "raw_unknown.bin",
        }
    }
}

#[derive(Debug)]
pub struct StoredChunk {
    pub kind: RawStreamKind,
    pub bytes: usize,
    pub path: PathBuf,
}

pub async fn ensure_session_dir(session_dir: &Path) -> AppResult<()> {
    fs::create_dir_all(session_dir).await?;
    Ok(())
}

pub async fn append_raw_chunk(session_dir: &Path, bytes: &[u8]) -> AppResult<StoredChunk> {
    ensure_session_dir(session_dir).await?;

    let (kind, payload) = parse_enveloped_chunk(bytes);
    let path = session_dir.join(kind.file_name());

    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(&path)
        .await?;

    file.write_all(payload).await?;
    file.write_all(b"\n").await?;

    Ok(StoredChunk {
        kind,
        bytes: payload.len(),
        path,
    })
}

fn parse_enveloped_chunk(bytes: &[u8]) -> (RawStreamKind, &[u8]) {
    const SEPARATOR: u8 = b'\n';

    if let Some(index) = bytes.iter().position(|byte| *byte == SEPARATOR) {
        let prefix = &bytes[..index];
        let payload = &bytes[index + 1..];
        return (RawStreamKind::from_prefix(prefix), payload);
    }

    (RawStreamKind::Unknown, bytes)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_video_envelope() {
        let (kind, payload) = parse_enveloped_chunk(b"raw_video\nabc");
        assert!(matches!(kind, RawStreamKind::Video));
        assert_eq!(payload, b"abc");
    }

    #[test]
    fn treats_unwrapped_bytes_as_unknown() {
        let (kind, payload) = parse_enveloped_chunk(b"abc");
        assert!(matches!(kind, RawStreamKind::Unknown));
        assert_eq!(payload, b"abc");
    }
}
