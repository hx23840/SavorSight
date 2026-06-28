package com.savorsight.savorsight

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savorsight.input.BareKeyEvent
import com.savorsight.input.RegisterBareKeyHandler
import com.savorsight.ui.design.BareKeyGuide
import com.savorsight.ui.design.BarePagedViewport
import com.savorsight.ui.design.BareScreenLayout
import com.savorsight.ui.theme.NeonGreen

@Composable
fun CheckResultScreen(
    onBack: () -> Unit,
    viewModel: CheckViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val speechHelper = remember { SpeechHelper(context) }
    var pageIndex by remember { mutableIntStateOf(0) }

    val result = state.result
    val pageCount = if (result != null) 2 else 1

    DisposableEffect(Unit) {
        result?.let {
            speechHelper.speak(it.tts)
        }
        onDispose {
            speechHelper.release()
        }
    }

    RegisterBareKeyHandler { event ->
        when (event) {
            BareKeyEvent.Click -> {
                if (result != null) {
                    if (pageIndex == 1) {
                        speechHelper.speak(result.tts)
                    } else {
                        pageIndex = 1
                    }
                }
                true
            }
            BareKeyEvent.DoubleClick -> {
                viewModel.retry()
                onBack()
                true
            }
            BareKeyEvent.LongPress -> {
                if (result != null) {
                    speechHelper.speak(result.tts)
                }
                true
            }
        }
    }

    val statusLabel = when (result?.status) {
        "pass" -> "✓ 可以下一步"
        "continue" -> "○ 继续当前"
        "adjust" -> "△ 需要调整"
        "uncertain" -> "？无法判断"
        else -> "检查结果"
    }

    val statusColor = when (result?.status) {
        "pass" -> Color(0xFF40FF5E)
        "continue" -> Color(0xFFFFCC00)
        "adjust" -> Color(0xFFFF6B6B)
        "uncertain" -> Color(0xFF888888)
        else -> NeonGreen
    }

    val subtitle = when {
        result == null -> ""
        pageIndex == 0 -> "检查结论"
        else -> "详细建议"
    }

    val clickLabel = when {
        result == null -> ""
        pageIndex == 0 -> "查看建议"
        else -> "重读"
    }

    BareScreenLayout(
        title = "检查结果",
        subtitle = subtitle,
        pageIndex = pageIndex,
        pageCount = pageCount,
        keyGuide = BareKeyGuide(
            click = clickLabel,
            doubleClick = "返回",
            longPress = "重读",
        ),
    ) {
        BarePagedViewport(pageIndex = pageIndex, pageCount = pageCount) { page ->
            when {
                result == null -> {
                    LoadingErrorContent(
                        error = state.errorMessage ?: "检查中...",
                    )
                }
                page == 0 -> {
                    SummaryContent(
                        statusLabel = statusLabel,
                        statusColor = statusColor,
                        summary = result.summary,
                    )
                }
                else -> {
                    SuggestionContent(
                        suggestion = result.suggestion,
                        statusColor = statusColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingErrorContent(error: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = error,
            color = Color.Red,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun SummaryContent(
    statusLabel: String,
    statusColor: Color,
    summary: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusBadge(
            label = statusLabel,
            color = statusColor,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "判断",
                color = statusColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = summary,
                color = statusColor,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 6,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            text = "单击查看详细建议",
            color = NeonGreen.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SuggestionContent(
    suggestion: String,
    statusColor: Color,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "建议",
            color = statusColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = suggestion,
            color = NeonGreen,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            maxLines = 10,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            text = "单击/长按：重读结论",
            color = NeonGreen.copy(alpha = 0.6f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        drawRoundRect(
            color = color.copy(alpha = 0.15f),
            topLeft = Offset.Zero,
            size = Size(size.width, 48f),
            cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(size.width, 48f),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2f),
        )
    }

    Text(
        text = label,
        color = color,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}
