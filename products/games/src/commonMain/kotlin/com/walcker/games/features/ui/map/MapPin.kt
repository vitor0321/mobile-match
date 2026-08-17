package com.walcker.games.features.ui.map

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.MatchStatus

/**
 * Um pino no mapa representando uma partida.
 *
 * Modelo enxuto e independente de plataforma: a camada de UI nativa
 * (Android maps-compose / iOS MapKit) consome apenas estes campos.
 */
internal data class MapPin(
    val matchId: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    val snippet: String,
    val status: MatchStatus,
)

/**
 * Converte um [Game] em [MapPin] para renderização no mapa.
 */
internal fun Game.toMapPin(): MapPin = MapPin(
    matchId = id,
    lat = lat,
    lng = lng,
    title = "${sport.label} · $venueName",
    snippet = "$confirmedPlayers/$totalPlayers · ${pricePerPlayer ?: "Grátis"}",
    status = status,
)
