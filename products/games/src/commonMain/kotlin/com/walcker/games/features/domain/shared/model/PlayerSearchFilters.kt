package com.walcker.games.features.domain.shared.model

internal data class PlayerSearchFilters(
    val query: String = "",
    val minRating: Float? = null,
    val maxRating: Float? = null,
    val favoriteSports: Set<Sport> = emptySet(),
    val sortBy: PlayerSortBy = PlayerSortBy.RATING,
)

internal enum class PlayerSortBy(
    val field: String,
    val descending: Boolean,
) {
    RATING(field = PROFILE_FIELD_RATING, descending = true),

    NAME(field = PROFILE_FIELD_FULL_NAME, descending = false),
}

internal const val PROFILE_FIELD_RATING: String = "rating"
internal const val PROFILE_FIELD_FULL_NAME: String = "fullName"
internal const val PROFILE_FIELD_IS_BANNED: String = "isBanned"
