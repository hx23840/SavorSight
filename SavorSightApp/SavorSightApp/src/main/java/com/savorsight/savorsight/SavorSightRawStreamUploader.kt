package com.savorsight.savorsight

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

enum class UploadConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Failed,
}

class SavorSightRawStreamUploader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build(),
) {
    private val _connectionState = MutableStateFlow(UploadConnectionState.Disconnected)
    val connectionState: StateFlow<UploadConnectionState> = _connectionState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val videoFramesSent = AtomicLong(0)
    private val audioBytesSent = AtomicLong(0)

    private var webSocket: WebSocket? = null
    private val mutex = Mutex()
    private val connected = AtomicBoolean(false)

    fun getVideoFramesSent(): Long = videoFramesSent.get()
    fun getAudioBytesSent(): Long = audioBytesSent.get()

    suspend fun connect(wsUrl: String): Result<Unit> = mutex.withLock {
        if (connected.get()) {
            return@withLock Result.success(Unit)
        }
        _connectionState.value = UploadConnectionState.Connecting
        _lastError.value = null

        val request = Request.Builder().url(wsUrl).build()
        val listener = UploadListener()

        return try {
            webSocket = client.newWebSocket(request, listener)
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = UploadConnectionState.Failed
            _lastError.value = e.message ?: "连接失败"
            Result.failure(e)
        }
    }

    fun sendVideoFrame(frameBytes: ByteArray) {
        if (!connected.get()) return
        val envelope = buildEnvelope("raw_video", frameBytes)
        webSocket?.send(ByteString.of(*envelope))
        videoFramesSent.incrementAndGet()
    }

    fun sendAudioChunk(audioBytes: ByteArray) {
        if (!connected.get()) return
        val envelope = buildEnvelope("raw_audio", audioBytes)
        webSocket?.send(ByteString.of(*envelope))
        audioBytesSent.addAndGet(audioBytes.size.toLong())
    }

    fun sendDeviceStatus(statusJson: String) {
        if (!connected.get()) return
        webSocket?.send(statusJson)
    }

    suspend fun disconnect() = mutex.withLock {
        connected.set(false)
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = UploadConnectionState.Disconnected
    }

    private fun buildEnvelope(type: String, payload: ByteArray): ByteArray {
        val prefix = "$type\n".toByteArray(Charsets.UTF_8)
        return prefix + payload
    }

    private inner class UploadListener : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            connected.set(true)
            _connectionState.value = UploadConnectionState.Connected
            Log.d(TAG, "WebSocket connected")
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            connected.set(false)
            Log.d(TAG, "WebSocket closing: $code $reason")
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            connected.set(false)
            _connectionState.value = UploadConnectionState.Disconnected
            Log.d(TAG, "WebSocket closed: $code $reason")
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            connected.set(false)
            _connectionState.value = UploadConnectionState.Failed
            _lastError.value = t.message ?: "连接错误"
            Log.e(TAG, "WebSocket failure", t)
        }
    }

    companion object {
        private const val TAG = "SavorSightUploader"
    }
}
