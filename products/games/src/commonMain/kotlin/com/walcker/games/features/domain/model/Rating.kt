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
 * @param dimensions Respostas às dimensões opcionais. Vazio quando o avaliador
 *   só deu estrelas — e também em toda avaliação gravada antes das dimensões
 *   existirem, que é a maioria do histórico.
 */
internal data class Rating(
    val id: String,
    val matchId: String,
    val ratedUserId: String,
    val raterUserId: String,
    val rating: Int, // 1-5 stars
    val comment: String,
    val createdAtMs: Long,
    val dimensions: RatingDimensions = RatingDimensions.None,
)
