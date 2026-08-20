package com.walcker.games.strings

internal data class MatchConfirmedStrings(
    val title: String,
    val subtitle: String,
    val codeLabel: String,
    val viewDetails: String,
    val backToMatches: String,
)

internal val matchConfirmedStringsEn = MatchConfirmedStrings(
    title = "You're in!",
    subtitle = "Your slot is confirmed.",
    codeLabel = "Match code",
    viewDetails = "View match details",
    backToMatches = "Back to matches",
)

internal val matchConfirmedStringsPt = MatchConfirmedStrings(
    title = "Temos Jogo!",
    subtitle = "Sua vaga está garantida.",
    codeLabel = "Código da partida",
    viewDetails = "Ver detalhes da partida",
    backToMatches = "Voltar para as partidas",
)
