package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.ReportReason
import com.walcker.games.features.domain.model.SubmitReportOutcome

internal interface ReportRepository {
    suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome>
}
