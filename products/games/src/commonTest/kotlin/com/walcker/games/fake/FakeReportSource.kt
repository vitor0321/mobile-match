package com.walcker.games.fake

import com.walcker.games.features.data.shared.source.ReportSource
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome

internal class FakeReportSource(
    var submitResult: Result<SubmitReportOutcome> = Result.success(SubmitReportOutcome.Recorded),
) : ReportSource {
    var submitCallCount: Int = 0
        private set

    override suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome> {
        submitCallCount++
        return submitResult
    }
}
