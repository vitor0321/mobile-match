package com.walcker.games.features.domain.model

/**
 * Resultado de chamar o Cloud Function `joinMatch`.
 *
 * A função retorna o status final do usuário em relação à partida:
 * - CONFIRMED: entrou como confirmado (havia vaga)
 * - WAITLIST: entrou na fila de espera (partida cheia), com a posição na fila
 * - ALREADY_JOINED: já era participante (idempotente)
 */
internal sealed interface JoinMatchOutcome {
    val matchId: String

    data class Confirmed(override val matchId: String) : JoinMatchOutcome
    data class Waitlist(override val matchId: String, val position: Int) : JoinMatchOutcome
    data class AlreadyJoined(override val matchId: String) : JoinMatchOutcome
}
