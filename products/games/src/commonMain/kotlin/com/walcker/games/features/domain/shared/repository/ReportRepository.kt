package com.walcker.games.features.domain.shared.repository

import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome

internal interface ReportRepository {
    suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome>
}
