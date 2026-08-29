package com.walcker.games.features.domain.model

internal sealed interface SubmitReportOutcome {
    data object Recorded : SubmitReportOutcome
    data object AlreadyReported : SubmitReportOutcome
}
