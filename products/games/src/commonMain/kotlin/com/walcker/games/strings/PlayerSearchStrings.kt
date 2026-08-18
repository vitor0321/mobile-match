package com.walcker.games.strings

import com.walcker.match.core.format.formatDecimal

internal data class PlayerSearchStrings(
    val title: String,
    val subtitle: String,
    val placeholder: String,
    val emptySearchPrompt: String,
    val emptyForQuery: (String) -> String,
    val errorLoading: String,
    val ratingValue: (Float) -> String,
    val ratingAccessibility: (Float) -> String,
)

internal val playerSearchStringsEn = PlayerSearchStrings(
    title = "Find Players",
    subtitle = "Search by name, rating, or experience",
    placeholder = "Search by player name",
    emptySearchPrompt = "Search for a player to get started",
    emptyForQuery = { q -> "No players found matching \"$q\"." },
    errorLoading = "Error loading players. Please try again.",
    ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = '.') },
    ratingAccessibility = { value ->
        "Rated ${formatDecimal(value = value, decimals = 1, decimalSeparator = '.')} out of 5"
    },
)

internal val playerSearchStringsPt = PlayerSearchStrings(
    title = "Encontrar Jogadores",
    subtitle = "Pesquise por nome, avaliação ou experiência",
    placeholder = "Buscar por nome do jogador",
    emptySearchPrompt = "Busque um jogador para começar",
    emptyForQuery = { q -> "Nenhum jogador encontrado para \"$q\"." },
    errorLoading = "Erro ao carregar jogadores. Tente novamente.",
    ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = ',') },
    ratingAccessibility = { value ->
        "Nota ${formatDecimal(value = value, decimals = 1, decimalSeparator = ',')} de 5"
    },
)
