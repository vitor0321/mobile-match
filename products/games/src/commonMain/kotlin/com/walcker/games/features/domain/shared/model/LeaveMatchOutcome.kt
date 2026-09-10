package com.walcker.games.features.domain.shared.model

internal data class LeaveMatchOutcome(
    val matchId: String,
    val promotedUserId: String? = null,
)
