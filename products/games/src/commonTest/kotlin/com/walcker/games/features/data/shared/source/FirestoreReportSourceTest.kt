package com.walcker.games.features.data.shared.source

import com.walcker.games.fake.FakeFirestoreClient
import com.walcker.games.fake.FunctionCall
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreReportSourceTest {
    private val serverReportReasons =
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
        )

    @Test
    fun `every reason the app offers is one the server accepts`() {
        assertEquals(serverReportReasons.toSet(), ReportReason.entries.map { it.id }.toSet())
    }

    @Test
    fun `a report sends the reason id and the details`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["submitReport"] = Result.success(mapOf("status" to "recorded")) }

            FirestoreReportSource(firestore).submitReport("m1", "p1", ReportReason.NO_SHOW, "não veio")

            assertEquals(
                FunctionCall(
                    "submitReport",
                    mapOf("matchId" to "m1", "reportedUserId" to "p1", "reason" to "no_show", "details" to "não veio"),
                ),
                firestore.functionCalls.single(),
            )
        }

    @Test
    fun `both answers of the server are understood`() =
        runTest {
            val firestore = FakeFirestoreClient()
            val source = FirestoreReportSource(firestore)

            firestore.functionResults["submitReport"] = Result.success(mapOf("status" to "recorded"))
            assertEquals(SubmitReportOutcome.Recorded, source.submitReport("m1", "p1", ReportReason.LATE, "").getOrThrow())

            firestore.functionResults["submitReport"] = Result.success(mapOf("status" to "already_reported"))
            assertEquals(SubmitReportOutcome.AlreadyReported, source.submitReport("m1", "p1", ReportReason.LATE, "").getOrThrow())
        }

    @Test
    fun `an unknown answer is a failure, not a silent success`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["submitReport"] = Result.success(mapOf("status" to "???")) }

            assertTrue(FirestoreReportSource(firestore).submitReport("m1", "p1", ReportReason.LATE, "").isFailure)
        }

    @Test
    fun `a failed call stays a failure`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["submitReport"] = Result.failure(IllegalStateException("denied")) }

            assertTrue(FirestoreReportSource(firestore).submitReport("m1", "p1", ReportReason.LATE, "").isFailure)
        }
}
