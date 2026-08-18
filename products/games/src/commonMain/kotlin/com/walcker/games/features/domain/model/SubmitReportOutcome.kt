package com.walcker.games.features.domain.model

/**
 * Result of calling the `submitReport` function.
 *
 * Reporting the same person for the same match twice is idempotent: the server
 * answers [AlreadyReported] instead of failing, because from the reporter's
 * side the complaint is registered either way.
 */
internal sealed interface SubmitReportOutcome {
    data object Recorded : SubmitReportOutcome
    data object AlreadyReported : SubmitReportOutcome
}
