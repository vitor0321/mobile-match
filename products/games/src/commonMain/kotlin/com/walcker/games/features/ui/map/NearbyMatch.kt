package com.walcker.games.features.ui.map

import com.walcker.games.features.domain.model.Game

internal data class NearbyMatch(
    val game: Game,
    val distanceKm: Double,
)
