package com.savorsight.savorsight

enum class SavorSightCaptureStatus {
    Idle,
    CreatingSession,
    Connecting,
    Capturing,
    Stopping,
    Processing,
    DraftReady,
    Error,
}

data class SavorSightCaptureState(
    val status: SavorSightCaptureStatus = SavorSightCaptureStatus.Idle,
    val sessionId: String? = null,
    val errorMessage: String? = null,
    val videoFramesSent: Long = 0,
    val audioBytesSent: Long = 0,
    val recipeDishName: String? = null,
    val recipeId: String? = null,
) {
    val isCapturing: Boolean
        get() = status == SavorSightCaptureStatus.Capturing

    val canStart: Boolean
        get() = status == SavorSightCaptureStatus.Idle || status == SavorSightCaptureStatus.Error || status == SavorSightCaptureStatus.DraftReady

    val canStop: Boolean
        get() = status == SavorSightCaptureStatus.Capturing || status == SavorSightCaptureStatus.Connecting
}
