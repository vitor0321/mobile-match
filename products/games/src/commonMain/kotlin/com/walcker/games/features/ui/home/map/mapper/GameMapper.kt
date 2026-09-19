package com.walcker.games.features.ui.home.map.mapper

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.ui.home.map.model.MapPin
import com.walcker.games.strings.SportStrings

internal fun Game.toMapPin(
    sports: SportStrings,
    freeLabel: String,
): MapPin =
    MapPin(
        matchId = id,
        lat = lat,
        lng = lng,
        title = "${sports.name(sport)} · $venueName",
        snippet = "$confirmedPlayers/$totalPlayers · ${pricePerPlayer ?: freeLabel}",
        status = status,
    )
