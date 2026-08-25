package com.walcker.games.strings

/**
 * Mensagens do envio de avaliação pós-partida.
 *
 * Só três de erro: o transporte de callable do Firebase não preserva o código do
 * HttpsError do outro lado, então inventar uma taxonomia de erro aqui daria
 * mensagens erradas. As pré-condições reais (partida não terminou, você não
 * jogou) já são barradas pela própria UI antes da chamada.
 */
internal data class RatingStrings(
    val submitSuccess: String,
    val alreadyRated: String,
    val submitError: String,
    val formTitle: (playerName: String) -> String,
    val commentLabel: String,
    val commentPlaceholder: String,
    val submitAction: String,
    val submitting: String,
    val dimensionsTitle: String,
    /**
     * Diz que as quatro são necessárias. O servidor recusa payload incompleto,
     * então esconder isso só produziria um erro genérico depois do toque.
     */
    val dimensionsHint: String,
    val dimensionPunctuality: String,
    val dimensionRespect: String,
    val dimensionFairPlay: String,
    val dimensionBehavior: String,
    /** Rótulo da fileira principal de estrelas — ela não tinha nenhum. */
    val overallLabel: String,
    /** Nome acessível de cada estrela do seletor: "3 de 5 estrelas". */
    val starContentDescription: (Int) -> String,
    /** Contador do comentário: "120/500". */
    val commentCounter: (current: Int, max: Int) -> String,
)

internal val ratingStringsEn = RatingStrings(
    submitSuccess = "Review sent. Thanks!",
    alreadyRated = "You already reviewed this player for this match.",
    submitError = "Could not send your review. Please try again.",
    formTitle = { playerName -> "Rate $playerName" },
    commentLabel = "Comment (optional)",
    commentPlaceholder = "Share how the game went...",
    submitAction = "Send review",
    submitting = "Sending...",
    dimensionsTitle = "How was it, in detail?",
    dimensionsHint = "Rate all four to send your review.",
    dimensionPunctuality = "Punctuality",
    dimensionRespect = "Respect",
    dimensionFairPlay = "Fair play",
    dimensionBehavior = "Behaviour",
    overallLabel = "Overall",
    starContentDescription = { n -> "$n out of 5 stars" },
    commentCounter = { current, max -> "$current/$max" },
)

internal val ratingStringsPt = RatingStrings(
    submitSuccess = "Avaliação enviada. Valeu!",
    alreadyRated = "Você já avaliou esse jogador nessa partida.",
    submitError = "Não foi possível enviar sua avaliação. Tente novamente.",
    formTitle = { playerName -> "Avaliar $playerName" },
    commentLabel = "Comentário (opcional)",
    commentPlaceholder = "Conta como foi o jogo...",
    submitAction = "Enviar avaliação",
    submitting = "Enviando...",
    dimensionsTitle = "Como foi, em detalhe?",
    dimensionsHint = "Responda as quatro para enviar a avaliação.",
    dimensionPunctuality = "Pontualidade",
    dimensionRespect = "Respeito",
    dimensionFairPlay = "Fair play",
    dimensionBehavior = "Comportamento",
    overallLabel = "Nota geral",
    starContentDescription = { n -> "$n de 5 estrelas" },
    commentCounter = { current, max -> "$current/$max" },
)
