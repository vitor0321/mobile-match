package com.walcker.games.features.ui.home.map.model

import com.walcker.games.features.domain.shared.model.Game

internal data class NearbyMatch(
    val game: Game,
    val distanceKm: Double,
)
