package com.walcker.games.features.data.shared.source

import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.match.firestore.FirestoreClient

internal class FirestoreReportSource(
    private val firestore: FirestoreClient,
) : ReportSource {
    override suspend fun submitReport(
        matchId: String,
        reportedUserId: String,
        reason: ReportReason,
        details: String,
    ): Result<SubmitReportOutcome> =
        firestore
            .callFunction(
                SUBMIT_REPORT_FUNCTION,
                mapOf(
                    "matchId" to matchId,
                    "reportedUserId" to reportedUserId,
                    "reason" to reason.id,
                    "details" to details,
                ),
            ).mapCatching { payload -> payload.toOutcome() }

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
