package com.savorsight.savorsight

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.UseCase
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savorsight.app.CONSTANT
import com.savorsight.camera.rememberCameraBound
import com.savorsight.input.BareKeyEvent
import com.savorsight.input.RegisterBareKeyHandler
import com.savorsight.input.rememberSubPageEnterDebounce
import com.savorsight.ui.design.BareHeroText
import com.savorsight.ui.design.BareInfoBlock
import com.savorsight.ui.design.BareKeyGuide
import com.savorsight.ui.design.BareScreenLayout
import com.savorsight.utils.BarePermissions
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun LearningScreen(
    onBack: () -> Unit,
    onRecipeReady: (recipeId: String) -> Unit,
    viewModel: SavorSightCaptureViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val audioExecutor = remember { Executors.newSingleThreadExecutor() }
    val ignoreDoubleClick = rememberSubPageEnterDebounce()

    var hasCamera by remember { mutableStateOf(BarePermissions.hasCamera(context)) }
    var hasAudio by remember { mutableStateOf(BarePermissions.hasRecordAudio(context)) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var audioThread by remember { mutableStateOf<Thread?>(null) }
    var audioRecording by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { map ->
        hasCamera = map[Manifest.permission.CAMERA] == true
        hasAudio = map[Manifest.permission.RECORD_AUDIO] == true
    }

    DisposableEffect(Unit) {
        if (!hasCamera || !hasAudio) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                )
            )
        }
        onDispose {
            stopAudio(audioRecord, audioThread)
            audioRecording = false
            cameraExecutor.shutdown()
            audioExecutor.shutdown()
        }
    }

    val cameraReady = rememberCameraBound(
        context = context,
        lifecycleOwner = lifecycleOwner,
        enabled = hasCamera && state.status != SavorSightCaptureStatus.Idle,
        onReady = {},
        onError = {},
        onUnbind = {
            imageAnalysis = null
        },
        onBound = { cases ->
            val analysis = cases.filterIsInstance<ImageAnalysis>().firstOrNull()
            imageAnalysis = analysis
        },
        useCases = {
            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetResolution(android.util.Size(320, 240))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (state.isCapturing) {
                            val bytes = yuv420ToNv21Bytes(imageProxy)
                            viewModel.onVideoFrame(bytes)
                        }
                        imageProxy.close()
                    }
                }
            arrayOf<UseCase>(analysis)
        },
    )

    DisposableEffect(state.isCapturing) {
        if (state.isCapturing && hasAudio) {
            startAudio(
                onChunk = { bytes -> viewModel.onAudioChunk(bytes) },
                onStarted = { rec, thread ->
                    audioRecord = rec
                    audioThread = thread
                    audioRecording = true
                },
                onError = { /* no-op for now */ },
            )
        } else {
            stopAudio(audioRecord, audioThread)
            audioRecord = null
            audioThread = null
            audioRecording = false
        }
        onDispose {
            if (audioRecording) {
                stopAudio(audioRecord, audioThread)
                audioRecording = false
            }
        }
    }

    RegisterBareKeyHandler { event ->
        when (event) {
            BareKeyEvent.Click -> {
                if (!hasCamera || !hasAudio) {
                    launcher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                        )
                    )
                    return@RegisterBareKeyHandler true
                }
                if (state.status == SavorSightCaptureStatus.DraftReady) {
                    state.recipeId?.let { onRecipeReady(it) }
                    return@RegisterBareKeyHandler true
                }
                viewModel.toggleCapture()
                true
            }

            BareKeyEvent.DoubleClick -> {
                if (ignoreDoubleClick()) return@RegisterBareKeyHandler true
                if (state.isCapturing) {
                    viewModel.stopCapture()
                }
                onBack()
                true
            }

            BareKeyEvent.LongPress -> false
        }
    }

    val statusText = when (state.status) {
        SavorSightCaptureStatus.Idle -> "待机"
        SavorSightCaptureStatus.CreatingSession -> "创建会话…"
        SavorSightCaptureStatus.Connecting -> "连接上传…"
        SavorSightCaptureStatus.Capturing -> "采集中"
        SavorSightCaptureStatus.Stopping -> "停止中…"
        SavorSightCaptureStatus.Processing -> "解析中…"
        SavorSightCaptureStatus.DraftReady -> "菜谱已生成"
        SavorSightCaptureStatus.Error -> "出错了"
    }

    val hintText = when (state.status) {
        SavorSightCaptureStatus.Idle -> "单击开始学习"
        SavorSightCaptureStatus.CreatingSession -> "请稍候"
        SavorSightCaptureStatus.Connecting -> "请稍候"
        SavorSightCaptureStatus.Capturing -> "单击停止"
        SavorSightCaptureStatus.Stopping -> "请稍候"
        SavorSightCaptureStatus.Processing -> "正在生成菜谱"
        SavorSightCaptureStatus.DraftReady -> state.recipeDishName ?: "菜谱草稿就绪"
        SavorSightCaptureStatus.Error -> state.errorMessage ?: "未知错误"
    }

    val clickLabel = when {
        !hasCamera || !hasAudio -> "授权"
        state.status == SavorSightCaptureStatus.DraftReady -> "查看菜谱"
        state.canStart -> "开始学习"
        state.canStop -> "停止学习"
        else -> "处理中"
    }

    BareScreenLayout(
        title = "见味·学习模式",
        subtitle = statusText,
        keyGuide = BareKeyGuide(
            click = clickLabel,
            doubleClick = "返回",
        ),
    ) {
        BareHeroText(
            text = statusText,
            hint = hintText,
        )
        if (state.isCapturing) {
            BareInfoBlock(
                label = "采集状态",
                lines = listOf(
                    "视频帧: ${state.videoFramesSent}",
                    "音频: ${formatBytes(state.audioBytesSent)}",
                ),
            )
        }
        state.errorMessage?.let { err ->
            if (state.status == SavorSightCaptureStatus.Error) {
                BareInfoBlock(label = "错误", lines = listOf(err))
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun startAudio(
    onChunk: (ByteArray) -> Unit,
    onStarted: (AudioRecord, Thread) -> Unit,
    onError: (Exception) -> Unit,
) {
    try {
        val rec = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(CONSTANT.AUDIO_SAMPLE_RATE)
                    .setChannelMask(CONSTANT.AUDIO_CHANNEL_MASK)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .build()
        rec.startRecording()
        val thread = Thread {
            val buf = ByteArray(2048)
            try {
                while (!Thread.interrupted()) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n > 0) {
                        onChunk(buf.copyOf(n))
                    }
                }
            } catch (_: Exception) {
            }
        }.apply { start() }
        onStarted(rec, thread)
    } catch (e: Exception) {
        onError(e)
    }
}

private fun stopAudio(rec: AudioRecord?, thread: Thread?) {
    try {
        thread?.interrupt()
        thread?.join(500)
    } catch (_: Exception) {
    }
    try {
        rec?.stop()
        rec?.release()
    } catch (_: Exception) {
    }
}

private fun yuv420ToNv21Bytes(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[2].buffer
    val vBuffer = image.planes[1].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    return nv21
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${(bytes / 1024.0).roundToInt()} KB"
        else -> "${(bytes / (1024.0 * 1024.0)).roundToInt()} MB"
    }
}
