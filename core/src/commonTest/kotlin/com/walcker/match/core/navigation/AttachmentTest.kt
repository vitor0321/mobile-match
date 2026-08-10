package com.walcker.match.core.navigation

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class AttachmentTest {
    @Test
    fun `When value is attached should expose active value`() {
        val attachment = Attachment<Any>()
        val activeValue = Any()

        attachment.attach(activeValue)

        assertSame(activeValue, attachment.value)
    }

    @Test
    fun `When active value is detached should clear attachment`() {
        val attachment = Attachment<Any>()
        val activeValue = Any()
        attachment.attach(activeValue)

        attachment.detach(activeValue)

        assertNull(attachment.value)
    }

    @Test
    fun `When stale value detaches should preserve newer attachment`() {
        val attachment = Attachment<Any>()
        val staleValue = Any()
        val activeValue = Any()
        attachment.attach(staleValue)
        attachment.attach(activeValue)

        attachment.detach(staleValue)

        assertSame(activeValue, attachment.value)
    }
}
