package com.savorsight.savorsight

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.savorsight.camera.rememberCameraBound
import com.savorsight.input.BareKeyEvent
import com.savorsight.input.RegisterBareKeyHandler
import com.savorsight.ui.design.BareKeyGuide
import com.savorsight.ui.design.BareScreenLayout
import com.savorsight.ui.theme.NeonGreen
import java.io.File

/**
 * 遵循眼镜UI规范：使用 rememberCameraBound + ImageCapture.takePicture
 * 无取景预览，TouchPad单击触发快门
 */
@Composable
fun CheckCameraScreen(
    onBack: () -> Unit,
    onResult: () -> Unit,
    viewModel: CheckViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val state by viewModel.uiState.collectAsState()

    var hasCamera by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCamera = granted
    }

    DisposableEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        onDispose { }
    }

    // 遵循Sample模式：使用rememberCameraBound异步绑定
    rememberCameraBound(
        context = context,
        lifecycleOwner = lifecycleOwner,
        enabled = hasCamera,
        onReady = { },
        onError = { },
        onUnbind = {
            imageCapture = null
        },
        onBound = { cases ->
            val cap = cases.filterIsInstance<ImageCapture>().firstOrNull()
            imageCapture = cap
        },
        useCases = {
            arrayOf(
                ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setJpegQuality(100)
                    .build(),
            )
        },
    )

    RegisterBareKeyHandler { event ->
        when (event) {
            BareKeyEvent.Click -> {
                val cap = imageCapture ?: return@RegisterBareKeyHandler true
                if (state.status != CheckStatus.Ready) return@RegisterBareKeyHandler true

                viewModel.onCaptureStarted()
                val outputFile = createTempImageFile(context)
                val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                cap.takePicture(
                    options,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val bytes = outputFile.readBytes()
                            outputFile.delete()
                            viewModel.onImageCaptured(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            viewModel.onError(exception.message ?: "拍照失败")
                        }
                    },
                )
                true
            }
            BareKeyEvent.DoubleClick -> {
                onBack()
                true
            }
            BareKeyEvent.LongPress -> false
        }
    }

    DisposableEffect(state.status) {
        if (state.status == CheckStatus.Done) {
            onResult()
        }
        onDispose { }
    }

    val title = "拍照检查"
    val statusText = when (state.status) {
        CheckStatus.Ready -> "单击拍照"
        CheckStatus.Capturing -> "拍摄中"
        CheckStatus.Analyzing -> "分析中..."
        CheckStatus.Done -> "完成"
        CheckStatus.Error -> state.errorMessage ?: "出错"
    }

    val clickLabel = when (state.status) {
        CheckStatus.Ready -> "拍照"
        CheckStatus.Capturing -> "拍摄中"
        CheckStatus.Analyzing -> "分析中"
        CheckStatus.Error -> "重试"
        CheckStatus.Done -> "完成"
    }

    val statusColor = if (state.status == CheckStatus.Error) Color.Red else NeonGreen

    BareScreenLayout(
        title = title,
        keyGuide = BareKeyGuide(
            click = clickLabel,
            doubleClick = "返回",
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.stepTitle,
                color = NeonGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "目标: ${state.targetState}",
                color = NeonGreen.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 14.sp,
            )
        }
    }
}

private fun createTempImageFile(context: Context): File {
    val cacheDir = context.cacheDir
    return File.createTempFile("check_", ".jpg", cacheDir)
}
