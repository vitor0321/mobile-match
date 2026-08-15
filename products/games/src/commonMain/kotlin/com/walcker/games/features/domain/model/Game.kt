package com.walcker.games.features.domain.model

/**
 * Uma partida que já existe: quadra reservada, organizador definido e um número
 * de vagas em aberto. O produto conecta essas vagas a jogadores disponíveis.
 *
 * Fonte: matches/{matchId} collection no Firestore com denormalização de organizador.
 */
internal data class Game(
    val id: String,
    val sport: Sport,
    val venueName: String,
    val neighborhood: String,
    val city: String,
    val address: String,
    // Location data for geohash queries
    val lat: Double,
    val lng: Double,
    val geohash: String,
    // Time in seconds since epoch (Firestore Timestamp)
    val startsAtSeconds: Long,
    val durationMin: Int,
    val confirmedPlayers: Int,
    val totalPlayers: Int,
    val pricePerPlayer: String?, // nullable: free matches have null
    val organizerName: String,
    val organizerId: String,
    val organizerRating: Double,
    val status: MatchStatus = MatchStatus.OPEN,
    /**
     * User IDs that have joined the match. Includes the organizer.
     * Empty list means only the organizer is in.
     */
    val participants: List<String> = emptyList(),
) {
    val openSlots: Int
        get() = (totalPlayers - confirmedPlayers).coerceAtLeast(0)

    val hasOpenSlots: Boolean
        get() = openSlots > 0
}

/**
 * Role do usuário logado em relação a uma partida.
 */
internal enum class MatchRole {
    ORGANIZER,
    PARTICIPANT,
}

/**
 * Match status in Firestore.
 */
internal enum class MatchStatus {
    OPEN,
    FULL,
    CANCELLED,
    FINISHED,
}
