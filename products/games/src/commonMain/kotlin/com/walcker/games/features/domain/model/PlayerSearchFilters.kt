package com.walcker.games.features.domain.model

/**
 * Advanced filters for player search.
 *
 * All fields are optional. Empty filters mean no filtering on that dimension.
 * Empty [favoriteSports] means all sports.
 */
internal data class PlayerSearchFilters(
    val query: String = "", // name search
    val minRating: Float? = null, // 0.0 - 5.0, null = no minimum
    val maxRating: Float? = null, // 0.0 - 5.0, null = no maximum
    val favoriteSports: Set<Sport> = emptySet(), // empty = all sports
    val minMatchesOrganized: Int? = null,
    val minMatchesParticipated: Int? = null,
    val sortBy: PlayerSortBy = PlayerSortBy.RATING,
)

/**
 * Sort order for player search results.
 */
internal enum class PlayerSortBy {
    RATING, // Highest rated first
    RECENT_ACTIVITY, // Most recently joined a game
    MATCHES_PLAYED, // Most games played (organized + participated)
    NAME, // A-Z alphabetically
}
