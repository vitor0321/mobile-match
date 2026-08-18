package com.walcker.games.strings

/**
 * Mensagens do envio de avaliação pós-partida.
 *
 * Só três: o transporte de callable do Firebase não preserva o código do
 * HttpsError do outro lado, então inventar uma taxonomia de erro aqui daria
 * mensagens erradas. As pré-condições reais (partida não terminou, você não
 * jogou) já são barradas pela própria UI antes da chamada.
 */
internal data class RatingStrings(
    val submitSuccess: String,
    val alreadyRated: String,
    val submitError: String,
)

internal val ratingStringsEn = RatingStrings(
    submitSuccess = "Review sent. Thanks!",
    alreadyRated = "You already reviewed this player for this match.",
    submitError = "Could not send your review. Please try again.",
)

internal val ratingStringsPt = RatingStrings(
    submitSuccess = "Avaliação enviada. Valeu!",
    alreadyRated = "Você já avaliou esse jogador nessa partida.",
    submitError = "Não foi possível enviar sua avaliação. Tente novamente.",
)
