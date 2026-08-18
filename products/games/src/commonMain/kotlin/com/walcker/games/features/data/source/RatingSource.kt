package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Rating

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
    ): Result<Unit>

    /**
     * Busca avaliações recebidas por um usuário.
     */
    suspend fun getUserRatings(userId: String, limit: Int): Result<List<Rating>>

    /**
     * Busca avaliações de um local/partida.
     */
    suspend fun getMatchLocationRatings(matchId: String, limit: Int): Result<List<Rating>>
}
