package com.savorsight.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BareSceneRoutes 路由常量单元测试
 *
 * 测试重点：
 * 1. 路由字符串的正确性
 * 2. 路由的唯一性
 * 3. 路由命名规范
 */
class BareSceneRoutesTest {

    @Test
    fun `Hub 路由正确`() {
        assertEquals("hub", BareSceneRoutes.HUB)
    }

    @Test
    fun `裸机能力路由正确`() {
        assertEquals("keys_wear", BareSceneRoutes.KEYS_WEAR)
        assertEquals("audio", BareSceneRoutes.AUDIO)
        assertEquals("photo", BareSceneRoutes.PHOTO)
        assertEquals("video", BareSceneRoutes.VIDEO)
        assertEquals("imu", BareSceneRoutes.IMU)
    }

    @Test
    fun `菜谱相关路由正确`() {
        assertEquals("recipe_list", BareSceneRoutes.RECIPE_LIST)
        assertEquals("recipe_detail", BareSceneRoutes.RECIPE_DETAIL)
        assertEquals("recipe_draft", BareSceneRoutes.RECIPE_DRAFT)
    }

    @Test
    fun `做菜流程路由正确`() {
        assertEquals("cooking_guide", BareSceneRoutes.COOKING_GUIDE)
        assertEquals("check_camera", BareSceneRoutes.CHECK_CAMERA)
        assertEquals("check_result", BareSceneRoutes.CHECK_RESULT)
    }

    @Test
    fun `学习采集路由正确`() {
        assertEquals("savorsight_learning", BareSceneRoutes.SAVORSIGHT_LEARNING)
    }

    @Test
    fun `所有路由都是非空字符串`() {
        val routes = listOf(
            BareSceneRoutes.HUB,
            BareSceneRoutes.KEYS_WEAR,
            BareSceneRoutes.AUDIO,
            BareSceneRoutes.PHOTO,
            BareSceneRoutes.VIDEO,
            BareSceneRoutes.IMU,
            BareSceneRoutes.RECIPE_LIST,
            BareSceneRoutes.RECIPE_DETAIL,
            BareSceneRoutes.SAVORSIGHT_LEARNING,
            BareSceneRoutes.RECIPE_DRAFT,
            BareSceneRoutes.COOKING_GUIDE,
            BareSceneRoutes.CHECK_CAMERA,
            BareSceneRoutes.CHECK_RESULT,
        )

        routes.forEach { route ->
            assertTrue("Route should not be empty: $route", route.isNotEmpty())
        }
    }

    @Test
    fun `所有路由都是唯一的`() {
        val routes = listOf(
            BareSceneRoutes.HUB,
            BareSceneRoutes.KEYS_WEAR,
            BareSceneRoutes.AUDIO,
            BareSceneRoutes.PHOTO,
            BareSceneRoutes.VIDEO,
            BareSceneRoutes.IMU,
            BareSceneRoutes.RECIPE_LIST,
            BareSceneRoutes.RECIPE_DETAIL,
            BareSceneRoutes.SAVORSIGHT_LEARNING,
            BareSceneRoutes.RECIPE_DRAFT,
            BareSceneRoutes.COOKING_GUIDE,
            BareSceneRoutes.CHECK_CAMERA,
            BareSceneRoutes.CHECK_RESULT,
        )

        val uniqueRoutes = routes.toSet()
        assertEquals("All routes should be unique", routes.size, uniqueRoutes.size)
    }

    @Test
    fun `菜谱详情路由符合模式`() {
        // 详情页使用参数化路由，基础路由不应包含 /
        assertFalse(BareSceneRoutes.RECIPE_DETAIL.contains("/"))
    }

    @Test
    fun `菜谱路由命名一致`() {
        // 所有菜谱相关路由都应以 recipe_ 开头
        assertTrue(BareSceneRoutes.RECIPE_LIST.startsWith("recipe_"))
        assertTrue(BareSceneRoutes.RECIPE_DETAIL.startsWith("recipe_"))
        assertTrue(BareSceneRoutes.RECIPE_DRAFT.startsWith("recipe_"))
    }

    @Test
    fun `检查相关路由命名一致`() {
        // 所有检查相关路由都应以 check_ 开头
        assertTrue(BareSceneRoutes.CHECK_CAMERA.startsWith("check_"))
        assertTrue(BareSceneRoutes.CHECK_RESULT.startsWith("check_"))
    }
}
