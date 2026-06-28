package com.savorsight.savorsight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savorsight.input.BareKeyEvent
import com.savorsight.input.RegisterBareKeyHandler
import com.savorsight.input.cycleIndex
import com.savorsight.ui.design.BareInfoBlock
import com.savorsight.ui.design.BareKeyGuide
import com.savorsight.ui.design.BarePagedViewport
import com.savorsight.ui.design.BareScreenLayout
import com.savorsight.ui.theme.NeonGreen

@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onStartCooking: () -> Unit,
    recipeId: String,
    viewModel: RecipeViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()
    var pageIndex by remember { mutableIntStateOf(0) }

    DisposableEffect(recipeId) {
        viewModel.selectRecipe(recipeId) {}
        onDispose { }
    }

    val recipe = detailState.recipe
    val steps = recipe?.steps ?: emptyList()
    val ingredients = recipe?.ingredients ?: emptyList()

    val overviewPage = 0
    val ingredientsPage = 1
    val stepsStartPage = 2
    val pageCount = if (recipe != null && !detailState.isLoading) {
        stepsStartPage + steps.size.coerceAtLeast(1)
    } else {
        1
    }

    RegisterBareKeyHandler { event ->
        when (event) {
            BareKeyEvent.Click -> {
                if (recipe != null && !detailState.isLoading) {
                    pageIndex = cycleIndex(pageIndex, pageCount, 1)
                }
                true
            }
            BareKeyEvent.DoubleClick -> {
                onBack()
                true
            }
            BareKeyEvent.LongPress -> {
                if (recipe != null && !detailState.isLoading) {
                    onStartCooking()
                }
                true
            }
        }
    }

    val currentPage = pageIndex.coerceIn(0, pageCount - 1)
    val isOverview = currentPage == overviewPage
    val isIngredients = currentPage == ingredientsPage
    val isStepPage = currentPage >= stepsStartPage
    val stepIndex = currentPage - stepsStartPage

    val subtitle = when {
        detailState.isLoading -> "加载中..."
        recipe == null -> "无数据"
        isOverview -> "概览"
        isIngredients -> "食材 (${ingredients.size})"
        isStepPage && stepIndex < steps.size -> "步骤 ${stepIndex + 1}/${steps.size}"
        else -> ""
    }

    val clickLabel = if (recipe != null && !detailState.isLoading) {
        "下一屏"
    } else {
        ""
    }

    BareScreenLayout(
        title = "菜谱详情",
        subtitle = subtitle,
        pageIndex = currentPage,
        pageCount = pageCount,
        keyGuide = BareKeyGuide(
            click = clickLabel,
            doubleClick = "返回",
            longPress = "开始做菜",
        ),
    ) {
        BarePagedViewport(pageIndex = currentPage, pageCount = pageCount) { page ->
            when {
                detailState.isLoading -> {
                    LoadingContent()
                }
                recipe == null -> {
                    ErrorContent(detailState.errorMessage ?: "加载失败")
                }
                page == overviewPage -> {
                    OverviewContent(
                        dishName = recipe.dishName,
                        servings = recipe.servings,
                        time = recipe.estimatedTimeMinutes,
                        ingredientCount = ingredients.size,
                        stepCount = steps.size,
                    )
                }
                page == ingredientsPage -> {
                    IngredientsContent(ingredients = ingredients)
                }
                page >= stepsStartPage -> {
                    val idx = page - stepsStartPage
                    if (idx < steps.size) {
                        StepContent(
                            index = idx + 1,
                            total = steps.size,
                            title = steps[idx].title,
                            instruction = steps[idx].instruction,
                            heat = steps[idx].heat,
                            targetState = steps[idx].targetState,
                            checkable = steps[idx].checkable,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "加载菜谱...",
            color = NeonGreen,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = message,
            color = androidx.compose.ui.graphics.Color.Red,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun OverviewContent(
    dishName: String,
    servings: Int,
    time: Int,
    ingredientCount: Int,
    stepCount: Int,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = dishName,
            color = NeonGreen,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        BareInfoBlock(
            label = "基本信息",
            lines = listOf(
                "${servings} 人份",
                "约 ${time} 分钟",
                "${ingredientCount} 种食材",
                "${stepCount} 个步骤",
            ),
        )
        BareInfoBlock(
            label = "操作提示",
            lines = listOf(
                "单击：翻页查看食材和步骤",
                "长按：开始做菜",
                "双击：返回菜谱列表",
            ),
        )
    }
}

@Composable
private fun IngredientsContent(ingredients: List<String>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "食材清单",
            color = NeonGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ingredients.take(8).forEach { ingredient ->
                Text(
                    text = "• $ingredient",
                    color = NeonGreen,
                    fontSize = 14.sp,
                )
            }
            if (ingredients.size > 8) {
                Text(
                    text = "等 ${ingredients.size} 种食材",
                    color = NeonGreen.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun StepContent(
    index: Int,
    total: Int,
    title: String,
    instruction: String,
    heat: String?,
    targetState: String?,
    checkable: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "第 $index / $total 步",
            color = NeonGreen.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        Text(
            text = title,
            color = NeonGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = instruction,
            color = NeonGreen,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        heat?.let {
            BareInfoBlock(
                label = "火候",
                lines = listOf(it),
            )
        }
        targetState?.let {
            BareInfoBlock(
                label = "目标状态" + if (checkable) "（可检查）" else "",
                lines = listOf(it),
            )
        }
    }
}
