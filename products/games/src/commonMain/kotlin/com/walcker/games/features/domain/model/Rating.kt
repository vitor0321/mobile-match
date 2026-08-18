package com.walcker.games.features.domain.model

/**
 * A avaliação de um jogador após participar de uma partida.
 *
 * @param id ID único da avaliação (gerado pelo Firestore)
 * @param matchId ID da partida avaliada
 * @param ratedUserId ID do jogador sendo avaliado
 * @param raterUserId ID do jogador que faz a avaliação
 * @param rating Estrelas (1-5)
 * @param comment Texto opcional da avaliação
 * @param createdAtMs Timestamp em milissegundos
 */
internal data class Rating(
    val id: String,
    val matchId: String,
    val ratedUserId: String,
    val raterUserId: String,
    val rating: Int, // 1-5 stars
    val comment: String,
    val createdAtMs: Long,
)

/**
 * Resposta do servidor após submeter uma avaliação.
 */
internal data class SubmitRatingResponse(
    val success: Boolean,
    val message: String,
)
