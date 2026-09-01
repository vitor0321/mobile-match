package com.walcker.games.features.ui.home.map.model

import com.walcker.games.features.domain.shared.model.MatchStatus

internal data class MapPin(
    val matchId: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    val snippet: String,
    val status: MatchStatus,
)
