package com.walcker.games.strings

import com.walcker.match.core.format.formatDecimal

internal data class PlayerRatingsStrings(
    val title: String,
    val loadingLabel: String,
    val sortRecent: String,
    val sortHighest: String,
    val sortLowest: String,
    val loadMore: String,
    val empty: String,
    val errorLoading: String,
    val retry: String,
    val back: String,
    val ratingValue: (Float) -> String,
    val ratingAccessibility: (Float) -> String,
)

internal val playerRatingsStringsEn = PlayerRatingsStrings(
    title = "Reviews",
    loadingLabel = "Loading reviews…",
    sortRecent = "Recent",
    sortHighest = "Highest",
    sortLowest = "Lowest",
    loadMore = "Load more",
    empty = "No reviews yet.",
    errorLoading = "Error loading reviews. Please try again.",
    retry = "Try again",
    back = "Back",
    ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = '.') },
    ratingAccessibility = { value ->
        "Rated ${formatDecimal(value = value, decimals = 1, decimalSeparator = '.')} out of 5"
    },
)

internal val playerRatingsStringsPt = PlayerRatingsStrings(
    title = "Avaliações",
    loadingLabel = "Carregando avaliações…",
    sortRecent = "Recentes",
    sortHighest = "Melhores",
    sortLowest = "Piores",
    loadMore = "Carregar mais",
    empty = "Nenhuma avaliação ainda.",
    errorLoading = "Erro ao carregar avaliações. Tente novamente.",
    retry = "Tentar novamente",
    back = "Voltar",
    ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = ',') },
    ratingAccessibility = { value ->
        "Nota ${formatDecimal(value = value, decimals = 1, decimalSeparator = ',')} de 5"
    },
)
