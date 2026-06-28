package com.savorsight.savorsight

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.savorsight.ui.design.BareScreenLayout
import com.savorsight.ui.theme.NeonGreen

@Composable
fun CookingGuideScreen(
    onBack: () -> Unit,
    onCheck: (stepIndex: Int, stepId: String, stepTitle: String, targetState: String) -> Unit,
    viewModel: RecipeViewModel,
) {
    val state by viewModel.detailState.collectAsState()
    val context = LocalContext.current
    val speechHelper = remember { SpeechHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.release()
        }
    }

    val recipe = state.recipe
    val steps = recipe?.steps ?: emptyList()
    val currentIndex = state.currentStepIndex
    val currentStep = steps.getOrNull(currentIndex)
    val isLastStep = currentIndex >= steps.size - 1

    RegisterBareKeyHandler { event ->
        when (event) {
            BareKeyEvent.Click -> {
                if (currentStep != null) {
                    speechHelper.speak(currentStep.instruction)
                }
                true
            }
            BareKeyEvent.LongPress -> {
                if (recipe == null || currentStep == null) return@RegisterBareKeyHandler true

                if (currentStep.checkable && currentStep.targetState != null) {
                    onCheck(currentIndex, currentStep.id, currentStep.title, currentStep.targetState)
                } else {
                    if (isLastStep) {
                        viewModel.finishCooking()
                        onBack()
                    } else {
                        viewModel.nextStep()
                    }
                }
                true
            }
            BareKeyEvent.DoubleClick -> {
                viewModel.finishCooking()
                onBack()
                true
            }
        }
    }

    val stepCount = steps.size
    val progressPercent = if (stepCount > 0) {
        ((currentIndex + 1).toFloat() / stepCount * 100).toInt()
    } else 0

    val longPressLabel = when {
        currentStep?.checkable == true -> "检查"
        isLastStep -> "完成"
        else -> "下一步"
    }

    BareScreenLayout(
        title = "做菜导航",
        subtitle = if (currentStep != null) "第 ${currentIndex + 1}/$stepCount 步" else "",
        pageIndex = currentIndex.coerceAtLeast(0),
        pageCount = stepCount.coerceAtLeast(1),
        keyGuide = BareKeyGuide(
            click = "朗读",
            doubleClick = "结束",
            longPress = longPressLabel,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StepProgressBar(
                current = currentIndex + 1,
                total = stepCount,
                progress = progressPercent,
            )

            if (recipe != null && currentStep != null) {
                Text(
                    text = currentStep.title,
                    color = NeonGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = currentStep.instruction,
                    color = NeonGreen,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 6,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    currentStep.heat?.let {
                        HeatTag(heat = it)
                    }
                    if (currentStep.checkable) {
                        CheckableTag()
                    }
                }

                currentStep.targetState?.let {
                    Text(
                        text = "目标: $it",
                        color = NeonGreen.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = "菜谱加载中...",
                    color = NeonGreen,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun StepProgressBar(
    current: Int,
    total: Int,
    progress: Int,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "$current/$total",
                color = NeonGreen,
                fontSize = 11.sp,
            )
            Text(
                text = "${progress}%",
                color = NeonGreen.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(top = 2.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color(0xFF1A1A1A),
                    cornerRadius = CornerRadius(2f, 2f),
                )
                drawRoundRect(
                    color = NeonGreen,
                    size = Size(size.width * progress / 100f, size.height),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            }
        }
    }
}

@Composable
private fun HeatTag(heat: String) {
    Box(modifier = Modifier.padding(end = 4.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = NeonGreen.copy(alpha = 0.15f),
                size = size,
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
                color = NeonGreen,
                size = size,
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1f),
            )
        }
        Text(
            text = heat,
            color = NeonGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CheckableTag() {
    Box(modifier = Modifier.padding(end = 4.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = NeonGreen.copy(alpha = 0.15f),
                size = size,
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawRoundRect(
                color = NeonGreen,
                size = size,
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1f),
            )
        }
        Text(
            text = "可检查",
            color = NeonGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
