package com.savorsight.savorsight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savorsight.input.BareKeyEvent
import com.savorsight.input.RegisterBareKeyHandler
import com.savorsight.input.cycleIndex
import com.savorsight.ui.design.BareHeroText
import com.savorsight.ui.design.BareInfoBlock
import com.savorsight.ui.design.BareKeyGuide
import com.savorsight.ui.design.BarePagedViewport
import com.savorsight.ui.design.BareScreenLayout

@Composable
fun RecipeListScreen(
    onBack: () -> Unit,
    onStartLearning: () -> Unit,
    onSelectRecipe: (String) -> Unit,
    viewModel: RecipeViewModel = viewModel(),
) {
    val listState by viewModel.listState.collectAsState()
    var pageIndex by remember { mutableIntStateOf(0) }

    val recipes = listState.recipes
    val pageCount = if (recipes.isEmpty()) 1 else recipes.size + 1

    DisposableEffect(Unit) {
        viewModel.loadRecipeList()
        onDispose { }
    }

    RegisterBareKeyHandler { event ->
        when (event) {
            BareKeyEvent.Click -> {
                pageIndex = cycleIndex(pageIndex, pageCount, 1)
                true
            }
            BareKeyEvent.DoubleClick -> {
                false
            }
            BareKeyEvent.LongPress -> {
                if (recipes.isEmpty()) {
                    onStartLearning()
                } else if (pageIndex < recipes.size) {
                    val recipeId = recipes[pageIndex].recipeId
                    onSelectRecipe(recipeId)
                } else {
                    onStartLearning()
                }
                true
            }
        }
    }

    val isLearningPage = recipes.isEmpty() || pageIndex == recipes.size
    val currentRecipe = if (recipes.isNotEmpty() && pageIndex < recipes.size) recipes[pageIndex] else null

    BareScreenLayout(
        title = "见味",
        subtitle = if (isLearningPage) "学习新菜谱" else currentRecipe?.dishName ?: "",
        pageIndex = pageIndex,
        pageCount = pageCount,
        keyGuide = BareKeyGuide(
            click = "下一屏",
            longPress = if (isLearningPage) "开始学习" else "查看详情",
        ),
    ) {
        BarePagedViewport(pageIndex = pageIndex, pageCount = pageCount) { page ->
            if (recipes.isEmpty()) {
                BareHeroText(
                    text = "还没有菜谱",
                    hint = "长按开始学习新菜谱",
                )
            } else if (page < recipes.size) {
                val recipe = recipes[page]
                BareInfoBlock(
                    label = recipe.dishName,
                    lines = listOf(
                        "${recipe.servings} 人份 · 约 ${recipe.estimatedTimeMinutes} 分钟",
                        "置信度: ${"%.0f".format(recipe.confidence * 100)}%",
                        "",
                        "长按查看详情",
                    ),
                )
            } else {
                BareHeroText(
                    text = "+ 学习新菜谱",
                    hint = "长按开始",
                )
            }
        }
    }
}
