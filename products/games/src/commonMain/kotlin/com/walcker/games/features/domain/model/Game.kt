package com.walcker.games.features.domain.model

/**
 * Uma partida que já existe: quadra reservada, organizador definido e um número
 * de vagas em aberto. O produto conecta essas vagas a jogadores disponíveis.
 */
internal data class Game(
    val id: String,
    val sport: Sport,
    val venueName: String,
    val neighborhood: String,
    // TODO: trocar por kotlinx-datetime quando o backend existir
    val startsAt: String,
    val confirmedPlayers: Int,
    val totalPlayers: Int,
    val pricePerPlayer: String?,
    val organizerName: String,
) {
    val openSlots: Int
        get() = (totalPlayers - confirmedPlayers).coerceAtLeast(0)

    val hasOpenSlots: Boolean
        get() = openSlots > 0
}
