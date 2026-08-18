package com.walcker.games.features.data.repository

import com.walcker.games.features.data.source.ReportSource
import com.walcker.games.features.domain.model.ReportReason
import com.walcker.games.features.domain.model.SubmitReportOutcome
import com.walcker.games.features.domain.repository.ReportRepository

internal class ReportRepositoryImpl(
    private val source: ReportSource,
) : ReportRepository {

    override suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome> =
        source.submitReport(matchId, reportedUserId, reason, details)
}
