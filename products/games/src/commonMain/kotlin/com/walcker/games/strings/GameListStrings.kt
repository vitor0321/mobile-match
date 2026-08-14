package com.walcker.games.strings

internal data class GameListStrings(
    val title: String,
    val subtitle: String,
    val allSportsChip: String,
    val radiusLabel: (Int) -> String,
    val emptyMessage: String,
    val loadErrorMessage: String,
    val joinSuccess: String,
    val joinError: String,
    val joinButton: String,
    val perPlayer: (String) -> String,
    val playersAndSlots: (confirmed: Int, total: Int, openSlots: Int, slotWord: String) -> String,
)

internal val gameListStringsEn = GameListStrings(
    title = "Open slots",
    subtitle = "Find matches near you",
    allSportsChip = "All",
    radiusLabel = { km -> "Radius: $km km" },
    emptyMessage = "No open slots in your area right now.",
    loadErrorMessage = "Could not load matches.",
    joinSuccess = "You joined the match.",
    joinError = "Could not join the match.",
    joinButton = "JOIN MATCH",
    perPlayer = { price -> "$price per player" },
    playersAndSlots = { c, t, _, _ -> "$c/$t players" },
)

internal val gameListStringsPt = GameListStrings(
    title = "Vagas abertas",
    subtitle = "Encontre partidas perto de você",
    allSportsChip = "Todos",
    radiusLabel = { km -> "Raio: $km km" },
    emptyMessage = "Nenhuma vaga aberta na sua região agora.",
    loadErrorMessage = "Não foi possível carregar as partidas.",
    joinSuccess = "Você entrou no jogo.",
    joinError = "Não foi possível entrar no jogo.",
    joinButton = "ENTRAR NO JOGO",
    perPlayer = { price -> "$price por jogador" },
    playersAndSlots = { c, t, open, word -> "$c/$t jogadores · $open $word" },
)
