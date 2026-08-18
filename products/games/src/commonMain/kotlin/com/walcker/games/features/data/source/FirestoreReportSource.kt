package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.ReportReason
import com.walcker.games.features.domain.model.SubmitReportOutcome
import com.walcker.match.firestore.FirestoreClient

/**
 * Calls the `submitReport` callable.
 *
 * There is no direct write path: `reports/{reportId}` denies client writes in
 * firestore.rules, because the same transaction that stores the report also
 * recomputes the reported player's moderation level.
 */
internal class FirestoreReportSource(
    private val firestore: FirestoreClient,
) : ReportSource {

    override suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome> = firestore
        .callFunction(
            SUBMIT_REPORT_FUNCTION,
            mapOf(
                "matchId" to matchId,
                "reportedUserId" to reportedUserId,
                "reason" to reason.id,
                "details" to details,
            ),
        )
        .mapCatching { payload -> payload.toOutcome() }

    private fun Map<String, Any?>.toOutcome(): SubmitReportOutcome =
        when (val status = this["status"]) {
            "recorded" -> SubmitReportOutcome.Recorded
            "already_reported" -> SubmitReportOutcome.AlreadyReported
            else -> throw IllegalStateException(
                "Unexpected submitReport response status: $status",
            )
        }

    private companion object {
        const val SUBMIT_REPORT_FUNCTION = "submitReport"
    }
}
