package com.savorsight.savorsight

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.savorsight.savorsight.SavorSightApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class CheckViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CheckUiState())
    val uiState: StateFlow<CheckUiState> = _uiState.asStateFlow()

    private val apiClient = SavorSightApiClient(BASE_URL)

    fun setContext(recipeId: String, stepId: String, stepTitle: String, targetState: String) {
        _uiState.value = CheckUiState(
            recipeId = recipeId,
            stepId = stepId,
            stepTitle = stepTitle,
            targetState = targetState,
        )
    }

    fun onCaptureStarted() {
        _uiState.value = _uiState.value.copy(status = CheckStatus.Capturing)
    }

    fun onError(message: String) {
        _uiState.value = _uiState.value.copy(
            status = CheckStatus.Error,
            errorMessage = message,
        )
    }

    fun onImageCaptured(imageBytes: ByteArray) {
        _uiState.value = _uiState.value.copy(
            capturedImage = imageBytes,
            status = CheckStatus.Analyzing,
        )
        analyzeImage(imageBytes)
    }

    fun retry() {
        _uiState.value = _uiState.value.copy(
            capturedImage = null,
            status = CheckStatus.Ready,
            result = null,
            errorMessage = null,
        )
    }

    private fun analyzeImage(imageBytes: ByteArray) {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = CheckStatus.Analyzing)

            val result = withContext(Dispatchers.IO) {
                apiClient.checkStep(
                    recipeId = state.recipeId,
                    stepId = state.stepId,
                    imageBytes = imageBytes,
                    targetState = state.targetState,
                )
            }

            if (result.isSuccess) {
                val checkResult = result.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    status = CheckStatus.Done,
                    result = CheckResultData(
                        status = checkResult.status,
                        summary = checkResult.summary,
                        suggestion = checkResult.suggestion,
                        tts = checkResult.tts,
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    status = CheckStatus.Error,
                    errorMessage = result.exceptionOrNull()?.message ?: "检查失败"
                )
            }
        }
    }

    companion object {
        private const val TAG = "CheckViewModel"
        private const val BASE_URL = "http://10.0.2.2:8080"
    }
}

data class CheckUiState(
    val recipeId: String = "",
    val stepId: String = "",
    val stepTitle: String = "",
    val targetState: String = "",
    val capturedImage: ByteArray? = null,
    val status: CheckStatus = CheckStatus.Ready,
    val result: CheckResultData? = null,
    val errorMessage: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) true
        if (javaClass != other?.javaClass) return false
        other as CheckUiState
        if (recipeId != other.recipeId) return false
        if (stepId != other.stepId) return false
        if (stepTitle != other.stepTitle) return false
        if (targetState != other.targetState) return false
        if (capturedImage != null) {
            if (other.capturedImage == null) return false
            if (!capturedImage.contentEquals(other.capturedImage)) return false
        } else if (other.capturedImage != null) return false
        if (status != other.status) return false
        if (result != other.result) return false
        if (errorMessage != other.errorMessage) return false
        return true
    }

    override fun hashCode(): Int {
        var result1 = recipeId.hashCode()
        result1 = 31 * result1 + stepId.hashCode()
        result1 = 31 * result1 + stepTitle.hashCode()
        result1 = 31 * result1 + targetState.hashCode()
        result1 = 31 * result1 + (capturedImage?.contentHashCode() ?: 0)
        result1 = 31 * result1 + status.hashCode()
        result1 = 31 * result1 + (result?.hashCode() ?: 0)
        result1 = 31 * result1 + (errorMessage?.hashCode() ?: 0)
        return result1
    }
}

data class CheckResultData(
    val status: String,
    val summary: String,
    val suggestion: String,
    val tts: String,
)

enum class CheckStatus {
    Ready,
    Capturing,
    Analyzing,
    Done,
    Error,
}
