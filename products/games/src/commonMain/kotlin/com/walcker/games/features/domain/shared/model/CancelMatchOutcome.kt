package com.walcker.games.features.domain.shared.model

internal sealed interface CancelMatchOutcome {
    val matchId: String

    data class Cancelled(
        override val matchId: String,
    ) : CancelMatchOutcome

    data class AlreadyCancelled(
        override val matchId: String,
    ) : CancelMatchOutcome
}
