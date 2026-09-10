package com.walcker.games.features.data.shared.repository

import com.walcker.games.features.data.shared.source.ReportSource
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.repository.ReportRepository

internal class ReportRepositoryImpl(
    private val source: ReportSource,
) : ReportRepository {
    override suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome> = source.submitReport(matchId, reportedUserId, reason, details)
}
