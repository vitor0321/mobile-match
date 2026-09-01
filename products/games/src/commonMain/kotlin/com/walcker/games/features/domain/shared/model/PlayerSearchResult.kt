package com.walcker.games.features.domain.shared.model

internal data class PlayerSearchResult(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val averageRating: Float,
    val totalRatings: Int,
    val favoriteSports: List<Sport>,
)
