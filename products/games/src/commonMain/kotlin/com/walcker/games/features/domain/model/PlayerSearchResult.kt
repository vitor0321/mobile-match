package com.walcker.games.features.domain.model

/**
 * A player as shown in the search results list.
 *
 * Only carries what the `profiles/{uid}` document actually stores today. Match
 * counts and activity rates were removed in Phase 5 Sprint 3: nothing writes
 * them, so every card rendered zeros.
 */
internal data class PlayerSearchResult(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val averageRating: Float,
    val totalRatings: Int,
    val favoriteSports: List<Sport>,
)
