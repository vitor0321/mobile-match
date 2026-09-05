package com.walcker.games.features.ui.home.map.mapper

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.ui.home.map.model.MapPin

internal fun Game.toMapPin(): MapPin =
    MapPin(
        matchId = id,
        lat = lat,
        lng = lng,
        title = "${sport.label} · $venueName",
        snippet = "$confirmedPlayers/$totalPlayers · ${pricePerPlayer ?: "Grátis"}",
        status = status,
    )
