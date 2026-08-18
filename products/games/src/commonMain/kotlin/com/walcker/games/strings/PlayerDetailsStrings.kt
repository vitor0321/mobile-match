package com.walcker.games.strings

import com.walcker.match.core.format.formatDecimal

internal data class PlayerDetailsStrings(
    val title: String,
    val errorLoading: String,
    val retry: String,
    val back: String,
    val reviews: String,
    val noReviews: String,
    val seeAllReviews: String,
    val ratingsCount: (Int) -> String,
    val memberSince: (String) -> String,
    val ratingValue: (Float) -> String,
    val ratingAccessibility: (Float) -> String,
)

internal val playerDetailsStringsEn = PlayerDetailsStrings(
    title = "Player Profile",
    errorLoading = "Error loading player. Please try again.",
    retry = "Try again",
    back = "Back",
    reviews = "Reviews",
    noReviews = "This player hasn't been reviewed yet.",
    seeAllReviews = "See all reviews",
    ratingsCount = { count -> if (count == 1) "1 review" else "$count reviews" },
    memberSince = { date -> "Member since $date" },
    ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = '.') },
    ratingAccessibility = { value ->
        "Rated ${formatDecimal(value = value, decimals = 1, decimalSeparator = '.')} out of 5"
    },
)

internal val playerDetailsStringsPt = PlayerDetailsStrings(
    title = "Perfil do Jogador",
    errorLoading = "Erro ao carregar jogador. Tente novamente.",
    retry = "Tentar novamente",
    back = "Voltar",
    reviews = "Avaliações",
    noReviews = "Este jogador ainda não recebeu avaliações.",
    seeAllReviews = "Ver todas as avaliações",
    ratingsCount = { count -> if (count == 1) "1 avaliação" else "$count avaliações" },
    memberSince = { date -> "Membro desde $date" },
    ratingValue = { value -> formatDecimal(value = value, decimals = 1, decimalSeparator = ',') },
    ratingAccessibility = { value ->
        "Nota ${formatDecimal(value = value, decimals = 1, decimalSeparator = ',')} de 5"
    },
)
