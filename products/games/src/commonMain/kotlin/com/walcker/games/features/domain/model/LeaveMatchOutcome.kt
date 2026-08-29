package com.walcker.games.features.domain.model

internal data class LeaveMatchOutcome(
    val matchId: String,
    val promotedUserId: String? = null,
)
