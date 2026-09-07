package com.walcker.games.strings

internal data class RatingStrings(
    val submitSuccess: String,
    val alreadyRated: String,
    val submitError: String,
    val formTitle: (playerName: String) -> String,
    val commentLabel: String,
    val commentPlaceholder: String,
    val submitAction: String,
    val submitting: String,
    val overallLabel: String,
    val starContentDescription: (Int) -> String,
    val commentCounter: (current: Int, max: Int) -> String,
)

internal val ratingStringsEn =
    RatingStrings(
        submitSuccess = "Review sent. Thanks!",
        alreadyRated = "You already reviewed this player for this match.",
        submitError = "Could not send your review. Please try again.",
        formTitle = { playerName -> "Rate $playerName" },
        commentLabel = "Comment (optional)",
        commentPlaceholder = "Share how the game went...",
        submitAction = "Send review",
        submitting = "Sending...",
        overallLabel = "Overall",
        starContentDescription = { n -> "$n out of 5 stars" },
        commentCounter = { current, max -> "$current/$max" },
    )

internal val ratingStringsPt =
    RatingStrings(
        submitSuccess = "Avaliação enviada. Valeu!",
        alreadyRated = "Você já avaliou esse jogador nessa partida.",
        submitError = "Não foi possível enviar sua avaliação. Tente novamente.",
        formTitle = { playerName -> "Avaliar $playerName" },
        commentLabel = "Comentário (opcional)",
        commentPlaceholder = "Conta como foi o jogo...",
        submitAction = "Enviar avaliação",
        submitting = "Enviando...",
        overallLabel = "Nota geral",
        starContentDescription = { n -> "$n de 5 estrelas" },
        commentCounter = { current, max -> "$current/$max" },
    )
