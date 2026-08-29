package com.walcker.games.features.ui.map

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.MatchStatus

internal data class MapPin(
    val matchId: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    val snippet: String,
    val status: MatchStatus,
)

internal fun Game.toMapPin(): MapPin = MapPin(
    matchId = id,
    lat = lat,
    lng = lng,
    title = "${sport.label} · $venueName",
    snippet = "$confirmedPlayers/$totalPlayers · ${pricePerPlayer ?: "Grátis"}",
    status = status,
)
