package com.walcker.match.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppBootstrapStateTest {

    @Test
    fun `When state is created Should start with bootstrap loading visible Then callback is not invoked`() {
        var callbackCount = 0

        val state = AppBootstrapState(
            onFirstFrameRendered = { callbackCount += 1 },
        )

        assertTrue(state.showBootstrapLoading)
        assertEquals(0, callbackCount)
    }

    @Test
    fun `When first frame is marked Should hide bootstrap loading Then callback is invoked once`() {
        var callbackCount = 0
        val state = AppBootstrapState(
            onFirstFrameRendered = { callbackCount += 1 },
        )

        state.markFirstFrameRendered()

        assertFalse(state.showBootstrapLoading)
        assertEquals(1, callbackCount)
    }

    @Test
    fun `When first frame is marked more than once Should keep callback idempotent Then bootstrap stays hidden`() {
        var callbackCount = 0
        val state = AppBootstrapState(
            onFirstFrameRendered = { callbackCount += 1 },
        )

        state.markFirstFrameRendered()
        state.markFirstFrameRendered()

        assertFalse(state.showBootstrapLoading)
        assertEquals(1, callbackCount)
    }
}
