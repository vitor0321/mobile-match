package com.walcker.games.features.domain.shared.model

internal sealed interface JoinMatchOutcome {
    val matchId: String

    data class Confirmed(
        override val matchId: String,
    ) : JoinMatchOutcome

    data class Waitlist(
        override val matchId: String,
        val position: Int,
    ) : JoinMatchOutcome

    data class AlreadyJoined(
        override val matchId: String,
    ) : JoinMatchOutcome
}
