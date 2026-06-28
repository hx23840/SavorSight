package com.savorsight.savorsight

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavorSightCaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(SavorSightCaptureState())
    val state: StateFlow<SavorSightCaptureState> = _state.asStateFlow()

    private val apiClient = SavorSightApiClient(BASE_URL)
    private val uploader = SavorSightRawStreamUploader()

    private var statusReportJob: Job? = null

    fun toggleCapture() {
        val current = _state.value
        if (current.canStart) {
            startCapture()
        } else if (current.canStop) {
            stopCapture()
        }
    }

    fun startCapture() {
        if (!_state.value.canStart) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                status = SavorSightCaptureStatus.CreatingSession,
                errorMessage = null,
                videoFramesSent = 0,
                audioBytesSent = 0,
                recipeDishName = null,
            )

            val sessionResult = apiClient.createLearningSession()
            if (sessionResult.isFailure) {
                _state.value = _state.value.copy(
                    status = SavorSightCaptureStatus.Error,
                    errorMessage = sessionResult.exceptionOrNull()?.message ?: "创建会话失败",
                )
                return@launch
            }

            val session = sessionResult.getOrThrow()
            _state.value = _state.value.copy(
                sessionId = session.sessionId,
                status = SavorSightCaptureStatus.Connecting,
            )

            val wsUrl = apiClient.wsUploadUrl(session.uploadEndpoint)
            val connectResult = uploader.connect(wsUrl)
            if (connectResult.isFailure) {
                _state.value = _state.value.copy(
                    status = SavorSightCaptureStatus.Error,
                    errorMessage = connectResult.exceptionOrNull()?.message ?: "连接上传服务失败",
                )
                return@launch
            }

            _state.value = _state.value.copy(
                status = SavorSightCaptureStatus.Capturing,
            )

            startStatusReport()
            Log.i(TAG, "Capture started: session=${session.sessionId}")
        }
    }

    fun stopCapture() {
        if (!_state.value.canStop) return

        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(status = SavorSightCaptureStatus.Stopping)

            stopStatusReport()

            val sessionId = current.sessionId
            uploader.disconnect()

            if (sessionId != null) {
                _state.value = _state.value.copy(status = SavorSightCaptureStatus.Processing)
                val finishResult = apiClient.finishLearningSession(sessionId)
                if (finishResult.isFailure) {
                    _state.value = _state.value.copy(
                        status = SavorSightCaptureStatus.Error,
                        errorMessage = finishResult.exceptionOrNull()?.message ?: "结束会话失败",
                    )
                    return@launch
                }

                delay(500)
                val draftResult = apiClient.getRecipeDraft(sessionId)
                if (draftResult.isSuccess) {
                    val draft = draftResult.getOrThrow()
                    _state.value = _state.value.copy(
                        status = SavorSightCaptureStatus.DraftReady,
                        recipeDishName = draft.dishName,
                        recipeId = draft.recipeId,
                    )
                } else {
                    _state.value = _state.value.copy(
                        status = SavorSightCaptureStatus.Error,
                        errorMessage = draftResult.exceptionOrNull()?.message ?: "获取菜谱草稿失败",
                    )
                }
            } else {
                _state.value = _state.value.copy(status = SavorSightCaptureStatus.Idle)
            }
        }
    }

    fun onVideoFrame(frameBytes: ByteArray) {
        if (_state.value.status != SavorSightCaptureStatus.Capturing) return
        uploader.sendVideoFrame(frameBytes)
        _state.value = _state.value.copy(
            videoFramesSent = uploader.getVideoFramesSent(),
        )
    }

    fun onAudioChunk(audioBytes: ByteArray) {
        if (_state.value.status != SavorSightCaptureStatus.Capturing) return
        uploader.sendAudioChunk(audioBytes)
        _state.value = _state.value.copy(
            audioBytesSent = uploader.getAudioBytesSent(),
        )
    }

    private fun startStatusReport() {
        statusReportJob?.cancel()
        statusReportJob = viewModelScope.launch {
            while (true) {
                delay(STATUS_REPORT_INTERVAL_MS)
                val current = _state.value
                if (current.status != SavorSightCaptureStatus.Capturing) break
                uploader.sendDeviceStatus(
                    "{\"camera\":\"active\",\"microphone\":\"active\",\"network\":\"connected\"}"
                )
            }
        }
    }

    private fun stopStatusReport() {
        statusReportJob?.cancel()
        statusReportJob = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            uploader.disconnect()
        }
    }

    companion object {
        private const val TAG = "SavorSightCaptureVM"
        private const val BASE_URL = "http://10.0.2.2:8080"
        private const val STATUS_REPORT_INTERVAL_MS = 5000L
    }
}
