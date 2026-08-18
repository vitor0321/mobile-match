package com.walcker.games.features.domain.model

/**
 * Resultado de chamar o Cloud Function `submitPlayerRating`.
 *
 * Reenviar a mesma avaliação é idempotente — a função devolve
 * [AlreadyRated] em vez de erro, no mesmo espírito de
 * [JoinMatchOutcome.AlreadyJoined]. A UI trata isso como sucesso: do ponto de
 * vista de quem tocou o botão, a avaliação está registrada.
 *
 * @property averageRating média do avaliado depois desta avaliação
 * @property ratingCount total de avaliações do avaliado
 */
internal sealed interface SubmitRatingOutcome {
    val averageRating: Float
    val ratingCount: Int

    data class Recorded(
        override val averageRating: Float,
        override val ratingCount: Int,
    ) : SubmitRatingOutcome

    data class AlreadyRated(
        override val averageRating: Float,
        override val ratingCount: Int,
    ) : SubmitRatingOutcome
}
