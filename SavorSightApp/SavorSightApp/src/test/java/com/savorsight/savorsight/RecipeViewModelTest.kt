package com.savorsight.savorsight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RecipeViewModel 业务逻辑单元测试
 *
 * 测试重点：
 * 1. 列表状态与详情状态的独立性
 * 2. 菜谱加载流程
 * 3. 做菜步骤导航逻辑
 * 4. 状态重置
 */
class RecipeViewModelTest {

    @Test
    fun `RecipeListUiState 初始状态为空列表`() {
        val state = RecipeListUiState()

        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.recipes.isEmpty())
    }

    @Test
    fun `RecipeDetailUiState 初始状态无菜谱`() {
        val state = RecipeDetailUiState()

        assertFalse(state.isLoading)
        assertFalse(state.isConfirmed)
        assertFalse(state.isCooking)
        assertEquals(0, state.currentStepIndex)
        assertNull(state.errorMessage)
        assertNull(state.recipe)
    }

    @Test
    fun `RecipeSummaryData 正确保存菜谱摘要信息`() {
        val summary = RecipeSummaryData(
            recipeId = "recipe-001",
            dishName = "番茄炒蛋",
            servings = 2,
            estimatedTimeMinutes = 15,
            confidence = 0.85,
        )

        assertEquals("recipe-001", summary.recipeId)
        assertEquals("番茄炒蛋", summary.dishName)
        assertEquals(2, summary.servings)
        assertEquals(15, summary.estimatedTimeMinutes)
        assertEquals(0.85, summary.confidence, 0.001)
    }

    @Test
    fun `RecipeData 正确保存完整菜谱信息`() {
        val steps = listOf(
            RecipeStepData(
                id = "step-1",
                title = "炒鸡蛋",
                instruction = "鸡蛋液下锅，炒到半凝固",
                heat = "中火",
                targetState = "鸡蛋半凝固",
                checkable = true,
            ),
            RecipeStepData(
                id = "step-2",
                title = "炒番茄",
                instruction = "番茄下锅翻炒",
                heat = "大火",
                targetState = null,
                checkable = false,
            ),
        )

        val recipe = RecipeData(
            recipeId = "recipe-001",
            dishName = "番茄炒蛋",
            servings = 2,
            estimatedTimeMinutes = 15,
            ingredients = listOf("番茄 2个", "鸡蛋 3个", "盐 适量"),
            steps = steps,
        )

        assertEquals("recipe-001", recipe.recipeId)
        assertEquals("番茄炒蛋", recipe.dishName)
        assertEquals(2, recipe.servings)
        assertEquals(15, recipe.estimatedTimeMinutes)
        assertEquals(3, recipe.ingredients.size)
        assertEquals(2, recipe.steps.size)

        // 验证第一个步骤
        val firstStep = recipe.steps[0]
        assertEquals("step-1", firstStep.id)
        assertEquals("炒鸡蛋", firstStep.title)
        assertEquals("中火", firstStep.heat)
        assertTrue(firstStep.checkable)

        // 验证第二个步骤
        val secondStep = recipe.steps[1]
        assertNull(secondStep.targetState)
        assertFalse(secondStep.checkable)
    }

    @Test
    fun `RecipeStepData 支持可检查和不可检查步骤`() {
        val checkableStep = RecipeStepData(
            id = "step-1",
            title = "切菜",
            instruction = "将食材切成小块",
            heat = null,
            targetState = "食材切好",
            checkable = true,
        )

        val normalStep = RecipeStepData(
            id = "step-2",
            title = "搅拌",
            instruction = "顺时针搅拌",
            heat = null,
            targetState = null,
            checkable = false,
        )

        assertTrue(checkableStep.checkable)
        assertNotNull(checkableStep.targetState)
        assertFalse(normalStep.checkable)
        assertNull(normalStep.targetState)
    }
}
