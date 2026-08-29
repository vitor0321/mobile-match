package com.walcker.games.features.domain.model

internal const val RATING_FIELD_CREATED_AT_MS: String = "createdAtMs"

internal const val RATING_FIELD_STARS: String = "rating"

internal enum class RatingSort(
    val primaryField: String,
    val descending: Boolean,
) {
    RECENT(primaryField = RATING_FIELD_CREATED_AT_MS, descending = true),

    HIGHEST(primaryField = RATING_FIELD_STARS, descending = true),

    LOWEST(primaryField = RATING_FIELD_STARS, descending = false),
}
