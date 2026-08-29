package com.walcker.games.features.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportReasonTest {

    @Test
    fun `ids are the exact wire contract shared with the functions`() {
        assertEquals(
            listOf(
                "no_show",
                "late",
                "no_payment",
                "aggressive_behavior",
                "verbal_abuse",
                "discrimination",
                "harassment",
                "dangerous_play",
                "fake_profile",
                "other",
            ),
            ReportReason.entries.map { it.id },
        )
    }

    @Test
    fun `there are ten reasons, including a generic one`() {
        assertEquals(10, ReportReason.entries.size)
        assertEquals(ReportReason.OTHER, ReportReason.fromId("other"))
    }

    @Test
    fun `fromId round-trips every reason`() {
        ReportReason.entries.forEach { reason ->
            assertEquals(reason, ReportReason.fromId(reason.id))
        }
    }

    @Test
    fun `fromId returns null for anything unknown`() {
        assertNull(ReportReason.fromId("NO_SHOW"))
        assertNull(ReportReason.fromId("brand_new_reason"))
        assertNull(ReportReason.fromId(""))
    }
}
