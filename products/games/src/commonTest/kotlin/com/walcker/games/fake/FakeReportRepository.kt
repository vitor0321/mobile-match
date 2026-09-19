package com.walcker.games.fake

import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.repository.ReportRepository

internal class FakeReportRepository(
    var submitResult: Result<SubmitReportOutcome> = Result.success(SubmitReportOutcome.Recorded),
) : ReportRepository {
    val submitCalls: MutableList<String> = mutableListOf()

    override suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome> {
        submitCalls += reportedUserId
        return submitResult
    }
}
