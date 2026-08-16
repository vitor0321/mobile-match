package com.walcker.games.features.domain.model

/**
 * Resultado de chamar o Cloud Function `cancelMatch`.
 *
 * Idempotente: cancelar uma partida já cancelada retorna [AlreadyCancelled]
 * em vez de lançar erro.
 */
internal sealed interface CancelMatchOutcome {
    val matchId: String

    data class Cancelled(override val matchId: String) : CancelMatchOutcome
    data class AlreadyCancelled(override val matchId: String) : CancelMatchOutcome
}
