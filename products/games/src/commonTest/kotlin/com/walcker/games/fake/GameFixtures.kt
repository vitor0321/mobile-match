package com.walcker.games.fake

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.model.Sport

/**
 * Partida de teste com valores neutros. Só o que o teste declara importa; o
 * resto existe para o construtor fechar.
 */
internal fun game(
    id: String = "match-1",
    startsAtSeconds: Long = 0L,
    durationMin: Int = 60,
    status: MatchStatus = MatchStatus.OPEN,
    participants: List<String> = emptyList(),
    confirmedPlayers: Int = 1,
    totalPlayers: Int = 10,
): Game = Game(
    id = id,
    sport = Sport.FUTSAL,
    venueName = "Quadra Central",
    neighborhood = "Centro",
    city = "São Paulo",
    address = "Rua Um, 100",
    lat = -23.55,
    lng = -46.63,
    geohash = "6gyf4",
    startsAtSeconds = startsAtSeconds,
    durationMin = durationMin,
    confirmedPlayers = confirmedPlayers,
    totalPlayers = totalPlayers,
    pricePerPlayer = null,
    organizerName = "Organizador",
    organizerId = "organizer-1",
    organizerRating = 5.0,
    status = status,
    participants = participants,
)
