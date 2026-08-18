package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.ReportReason
import com.walcker.games.features.domain.model.SubmitReportOutcome

/**
 * Reporting a player for behaviour in a match.
 */
internal interface ReportRepository {
    /**
     * @param matchId the match both players took part in — the server rejects
     *        a report between people who never played together
     * @param details optional free text, trimmed and length-capped server-side
     */
    suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome>
}
