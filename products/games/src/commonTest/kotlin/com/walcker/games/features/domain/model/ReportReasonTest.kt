package com.walcker.games.features.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportReasonTest {

    @Test
    fun `ids are the exact wire contract shared with the functions`() {
        // The same list lives in functions/src/moderation.ts, guarded by its own
        // test. Renaming an id orphans reports already stored in Firestore, so
        // both sides assert it explicitly instead of trusting a comment.
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
        // Without a generic option people pick the closest wrong reason just to
        // be able to report, and the statistics become noise.
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
        // A newer client may send a reason this build does not know about.
        assertNull(ReportReason.fromId("NO_SHOW"))
        assertNull(ReportReason.fromId("brand_new_reason"))
        assertNull(ReportReason.fromId(""))
    }
}
