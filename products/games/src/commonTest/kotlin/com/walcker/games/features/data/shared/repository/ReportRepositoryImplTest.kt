package com.walcker.games.features.data.shared.repository

import com.walcker.games.fake.FakeReportSource
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportRepositoryImplTest {
    @Test
    fun `returns the outcome the source reports`() =
        runTest {
            val source = FakeReportSource(submitResult = Result.success(SubmitReportOutcome.AlreadyReported))
            val repository = ReportRepositoryImpl(source)

            val result =
                repository.submitReport(
                    matchId = "match-1",
                    reportedUserId = "player-2",
                    reason = ReportReason.NO_SHOW,
                    details = "",
                )

            assertEquals(SubmitReportOutcome.AlreadyReported, result.getOrThrow())
            assertEquals(1, source.submitCallCount)
        }

    @Test
    fun `propagates a source failure as-is`() =
        runTest {
            val source = FakeReportSource(submitResult = Result.failure(IllegalStateException("offline")))
            val repository = ReportRepositoryImpl(source)

            val result =
                repository.submitReport(
                    matchId = "match-1",
                    reportedUserId = "player-2",
                    reason = ReportReason.HARASSMENT,
                    details = "",
                )

            assertTrue(result.isFailure)
        }
}
