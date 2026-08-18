package com.walcker.games.strings

internal data class PlayerSearchStrings(
    val title: String,
    val subtitle: String,
    val placeholder: String,
    val emptySearchPrompt: String,
    val emptyForQuery: (String) -> String,
    val errorLoading: String,
)

internal val playerSearchStringsEn = PlayerSearchStrings(
    title = "Find Players",
    subtitle = "Search by name, rating, or experience",
    placeholder = "Search by player name",
    emptySearchPrompt = "Search for a player to get started",
    emptyForQuery = { q -> "No players found matching \"$q\"." },
    errorLoading = "Error loading players. Please try again.",
)

internal val playerSearchStringsPt = PlayerSearchStrings(
    title = "Encontrar Jogadores",
    subtitle = "Pesquise por nome, avaliação ou experiência",
    placeholder = "Buscar por nome do jogador",
    emptySearchPrompt = "Busque um jogador para começar",
    emptyForQuery = { q -> "Nenhum jogador encontrado para \"$q\"." },
    errorLoading = "Erro ao carregar jogadores. Tente novamente.",
)
