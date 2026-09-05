package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.repository.ReportRepository

internal interface SubmitReportUseCase {
    suspend operator fun invoke(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome>
}

internal class SubmitReportUseCaseImpl(
    private val repository: ReportRepository,
) : SubmitReportUseCase {
    override suspend fun invoke(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome> = repository.submitReport(matchId, reportedUserId, reason, details)
}
