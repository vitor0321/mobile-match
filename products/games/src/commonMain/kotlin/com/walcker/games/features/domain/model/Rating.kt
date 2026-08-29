package com.walcker.games.features.domain.model

internal data class Rating(
    val id: String,
    val matchId: String,
    val ratedUserId: String,
    val raterUserId: String,
    val rating: Int,
    val comment: String,
    val createdAtMs: Long,
    val dimensions: RatingDimensions = RatingDimensions.None,
)
