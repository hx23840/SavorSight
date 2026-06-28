package com.savorsight.savorsight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SavorSightCaptureState 采集状态单元测试
 *
 * 测试重点：
 * 1. 状态转换的正确性
 * 2. 错误状态的处理
 * 3. 状态属性的完整性
 */
class SavorSightCaptureStateTest {

    @Test
    fun `SavorSightCaptureState 初始状态为 Idle`() {
        val state = SavorSightCaptureState()

        assertEquals(SavorSightCaptureStatus.Idle, state.status)
        assertNull(state.sessionId)
        assertNull(state.recipeId)
        assertTrue(state.videoFramesSent == 0L)
        assertTrue(state.audioBytesSent == 0L)
        assertNull(state.errorMessage)
    }

    @Test
    fun `CreatingSession 状态包含 sessionId`() {
        val state = SavorSightCaptureState(
            status = SavorSightCaptureStatus.CreatingSession,
            sessionId = "session-123",
        )

        assertEquals(SavorSightCaptureStatus.CreatingSession, state.status)
        assertEquals("session-123", state.sessionId)
    }

    @Test
    fun `Connecting 状态表示正在连接`() {
        val state = SavorSightCaptureState(
            status = SavorSightCaptureStatus.Connecting,
            sessionId = "session-123",
        )

        assertEquals(SavorSightCaptureStatus.Connecting, state.status)
    }

    @Test
    fun `Capturing 状态表示正在采集`() {
        val state = SavorSightCaptureState(
            status = SavorSightCaptureStatus.Capturing,
            sessionId = "session-123",
        )

        assertEquals(SavorSightCaptureStatus.Capturing, state.status)
        assertTrue(state.isCapturing)
    }

    @Test
    fun `Processing 状态表示后端处理中`() {
        val state = SavorSightCaptureState(
            status = SavorSightCaptureStatus.Processing,
            sessionId = "session-123",
        )

        assertEquals(SavorSightCaptureStatus.Processing, state.status)
        assertFalse(state.isCapturing)
    }

    @Test
    fun `DraftReady 状态包含 recipeId`() {
        val state = SavorSightCaptureState(
            status = SavorSightCaptureStatus.DraftReady,
            sessionId = "session-123",
            recipeId = "recipe-001",
            recipeDishName = "番茄炒蛋",
        )

        assertEquals(SavorSightCaptureStatus.DraftReady, state.status)
        assertEquals("session-123", state.sessionId)
        assertEquals("recipe-001", state.recipeId)
        assertEquals("番茄炒蛋", state.recipeDishName)
    }

    @Test
    fun `Error 状态包含错误信息`() {
        val state = SavorSightCaptureState(
            status = SavorSightCaptureStatus.Error,
            sessionId = "session-123",
            errorMessage = "Network connection failed",
        )

        assertEquals(SavorSightCaptureStatus.Error, state.status)
        assertEquals("Network connection failed", state.errorMessage)
    }

    @Test
    fun `计数属性在采集过程中递增`() {
        val state = SavorSightCaptureState(
            status = SavorSightCaptureStatus.Capturing,
            sessionId = "session-123",
            videoFramesSent = 100L,
            audioBytesSent = 500L,
        )

        assertEquals(100L, state.videoFramesSent)
        assertEquals(500L, state.audioBytesSent)
    }

    @Test
    fun `SavorSightCaptureStatus 枚举包含关键阶段`() {
        val statuses = SavorSightCaptureStatus.entries.toSet()

        // 验证关键状态存在
        assertTrue(statuses.contains(SavorSightCaptureStatus.Idle))
        assertTrue(statuses.contains(SavorSightCaptureStatus.Capturing))
        assertTrue(statuses.contains(SavorSightCaptureStatus.Processing))
        assertTrue(statuses.contains(SavorSightCaptureStatus.DraftReady))
        assertTrue(statuses.contains(SavorSightCaptureStatus.Error))
    }

    @Test
    fun `canStart 在 Idle 状态返回 true`() {
        val idleState = SavorSightCaptureState(status = SavorSightCaptureStatus.Idle)
        val errorState = SavorSightCaptureState(status = SavorSightCaptureStatus.Error)
        val draftReadyState = SavorSightCaptureState(status = SavorSightCaptureStatus.DraftReady)
        val capturingState = SavorSightCaptureState(status = SavorSightCaptureStatus.Capturing)

        assertTrue(idleState.canStart)
        assertTrue(errorState.canStart)
        assertTrue(draftReadyState.canStart)
        assertFalse(capturingState.canStart)
    }

    @Test
    fun `canStop 在 Capturing 或 Connecting 状态返回 true`() {
        val capturingState = SavorSightCaptureState(status = SavorSightCaptureStatus.Capturing)
        val connectingState = SavorSightCaptureState(status = SavorSightCaptureStatus.Connecting)
        val idleState = SavorSightCaptureState(status = SavorSightCaptureStatus.Idle)

        assertTrue(capturingState.canStop)
        assertTrue(connectingState.canStop)
        assertFalse(idleState.canStop)
    }

    @Test
    fun `isCapturing 仅在 Capturing 状态返回 true`() {
        val capturingState = SavorSightCaptureState(status = SavorSightCaptureStatus.Capturing)
        val connectingState = SavorSightCaptureState(status = SavorSightCaptureStatus.Connecting)
        val idleState = SavorSightCaptureState(status = SavorSightCaptureStatus.Idle)

        assertTrue(capturingState.isCapturing)
        assertFalse(connectingState.isCapturing)
        assertFalse(idleState.isCapturing)
    }
}
