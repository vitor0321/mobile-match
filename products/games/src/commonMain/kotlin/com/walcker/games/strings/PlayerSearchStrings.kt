package com.walcker.games.strings

import com.walcker.match.core.format.formatDecimal

internal data class PlayerSearchStrings(
    val title: String,
    val loadingLabel: String,
    val subtitle: String,
    val placeholder: String,
    val emptySearchPrompt: String,
    val emptyForQuery: (String) -> String,
    val errorLoading: String,
    val retry: String,
    val reachedLimit: String,
    val filtersTitle: String,
    val filtersButton: String,
    val ratingSection: String,
    val ratingMin: String,
    val ratingMax: String,
    val sportsSection: String,
    val clearFilters: String,
    val applyFilters: String,
    val ratingValue: (Float) -> String,
    val ratingAccessibility: (Float) -> String,
)

internal val playerSearchStringsEn =
    PlayerSearchStrings(
        title = "Find Players",
        loadingLabel = "Loading players…",
        subtitle = "Search by name, rating, or sport",
        placeholder = "Search by player name",
        emptySearchPrompt = "Search for a player to get started",
        emptyForQuery = { q -> "No players found matching \"$q\"." },
        errorLoading = "Error loading players. Please try again.",
        retry = "Try again",
        reachedLimit = "Showing the first results only — narrow your search to find more.",
        filtersTitle = "Filters",
        filtersButton = "Filters",
        ratingSection = "Rating",
        ratingMin = "Minimum",
        ratingMax = "Maximum",
        sportsSection = "Sports",
        clearFilters = "Clear",
        applyFilters = "Apply",
        ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = '.') },
        ratingAccessibility = { value ->
            "Rated ${formatDecimal(value = value, decimals = 1, decimalSeparator = '.')} out of 5"
        },
    )

internal val playerSearchStringsPt =
    PlayerSearchStrings(
        title = "Encontrar Jogadores",
        loadingLabel = "Carregando jogadores…",
        subtitle = "Pesquise por nome, avaliação ou esporte",
        placeholder = "Buscar por nome do jogador",
        emptySearchPrompt = "Busque um jogador para começar",
        emptyForQuery = { q -> "Nenhum jogador encontrado para \"$q\"." },
        errorLoading = "Erro ao carregar jogadores. Tente novamente.",
        retry = "Tentar novamente",
        reachedLimit = "Mostrando só os primeiros resultados — refine a busca para encontrar mais.",
        filtersTitle = "Filtros",
        filtersButton = "Filtros",
        ratingSection = "Avaliação",
        ratingMin = "Mínima",
        ratingMax = "Máxima",
        sportsSection = "Esportes",
        clearFilters = "Limpar",
        applyFilters = "Aplicar",
        ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = ',') },
        ratingAccessibility = { value ->
            "Nota ${formatDecimal(value = value, decimals = 1, decimalSeparator = ',')} de 5"
        },
    )
