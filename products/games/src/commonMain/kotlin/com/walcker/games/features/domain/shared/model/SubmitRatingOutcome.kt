package com.walcker.games.features.domain.shared.model

internal sealed interface SubmitRatingOutcome {
    val averageRating: Float
    val ratingCount: Int

    data class Recorded(
        override val averageRating: Float,
        override val ratingCount: Int,
    ) : SubmitRatingOutcome

    data class Updated(
        override val averageRating: Float,
        override val ratingCount: Int,
    ) : SubmitRatingOutcome

    // submitMatchRating (avaliação do local/evento, use case separado) ainda
    // retorna "already_rated" — só submitPlayerRating passou a editar em vez
    // de ignorar reenvio.
    data class AlreadyRated(
        override val averageRating: Float,
        override val ratingCount: Int,
    ) : SubmitRatingOutcome
}
