package com.walcker.games.features.domain.model

/**
 * Filters for player search.
 *
 * Doubles as the cache key for a search, so it must stay a value type with
 * everything the query depends on — including [query].
 *
 * All fields are optional; empty [favoriteSports] means all sports.
 */
internal data class PlayerSearchFilters(
    val query: String = "", // name search
    val minRating: Float? = null, // 0.0 - 5.0, null = no minimum
    val maxRating: Float? = null, // 0.0 - 5.0, null = no maximum
    val favoriteSports: Set<Sport> = emptySet(), // empty = all sports
    val sortBy: PlayerSortBy = PlayerSortBy.RATING,
)

/**
 * Sort order for player search results.
 *
 * Only orderings backed by a field the profile document actually has. Firestore
 * drops documents that are missing the `orderBy` field, so sorting by something
 * nobody writes returns an empty list rather than an unsorted one.
 *
 * @property field the `profiles/{uid}` field to order by
 * @property descending whether the order is descending
 */
internal enum class PlayerSortBy(
    val field: String,
    val descending: Boolean,
) {
    /** Highest rated first. */
    RATING(field = PROFILE_FIELD_RATING, descending = true),

    /** A-Z by name. */
    NAME(field = PROFILE_FIELD_FULL_NAME, descending = false),
}

internal const val PROFILE_FIELD_RATING: String = "rating"
internal const val PROFILE_FIELD_FULL_NAME: String = "fullName"
internal const val PROFILE_FIELD_IS_BANNED: String = "isBanned"
