package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.games.features.domain.model.SubmitRatingOutcome

/**
 * Data source interface para operações de rating.
 */
internal interface RatingSource {
    /**
     * Submete uma avaliação via Firestore Cloud Function.
     */
    suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
    ): Result<SubmitRatingOutcome>

    /**
     * Busca avaliações recebidas por um usuário (primeira página, mais recentes).
     *
     * Atalho para quem só precisa de uma amostra — o perfil próprio, por exemplo.
     * Para listas navegáveis use [getUserRatingsPage].
     */
    suspend fun getUserRatings(userId: String, limit: Int): Result<List<Rating>>

    /**
     * Busca uma página de avaliações recebidas por um usuário.
     *
     * @param sort ordenação aplicada no servidor
     * @param cursor cursor opaco da página anterior; `null` traz a primeira
     */
    suspend fun getUserRatingsPage(
        userId: String,
        limit: Int,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>

    /**
     * Busca avaliações de um local/partida.
     */
    suspend fun getMatchLocationRatings(matchId: String, limit: Int): Result<List<Rating>>
}
