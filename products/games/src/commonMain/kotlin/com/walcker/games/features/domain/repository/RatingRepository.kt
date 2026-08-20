package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.features.domain.model.SubmitRatingOutcome

/**
 * Repository para gerenciar avaliações de jogadores e locais.
 */
internal interface RatingRepository {
    /**
     * Submete uma avaliação de um jogador após partida.
     *
     * @param matchId ID da partida
     * @param ratedUserId ID do jogador sendo avaliado
     * @param rating Estrelas (1-5)
     * @param comment Comentário opcional
     * @param dimensions As quatro dimensões, todas preenchidas — o servidor
     *   recusa payload incompleto
     */
    suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome>

    /**
     * Obtém todas as avaliações recebidas por um usuário.
     *
     * @param userId ID do usuário
     * @param limit Número máximo de avaliações (padrão: 50)
     */
    suspend fun getUserRatings(userId: String, limit: Int = 50): Result<List<Rating>>

    /**
     * Obtém avaliações de um local/quadra.
     *
     * @param matchId ID da partida (location proxy)
     * @param limit Número máximo de avaliações
     */
    suspend fun getMatchLocationRatings(matchId: String, limit: Int = 10): Result<List<Rating>>
}
