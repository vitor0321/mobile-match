package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.SubmitRatingOutcome
import com.walcker.games.features.domain.repository.RatingRepository

/**
 * Use case para submeter avaliação de um jogador após partida.
 */
internal class SubmitRatingUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
    ): Result<SubmitRatingOutcome> =
        ratingRepository.submitPlayerRating(matchId, ratedUserId, rating, comment)
}

/**
 * Use case para obter avaliações recebidas por um usuário.
 */
internal class GetUserRatingsUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(userId: String, limit: Int = 50) =
        ratingRepository.getUserRatings(userId, limit)
}
