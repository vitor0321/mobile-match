package com.walcker.games.features.domain.model

internal data class RatingsPage(
    val ratings: List<Rating>,
    val nextCursor: String?,
) {
    val hasMore: Boolean get() = nextCursor != null

    internal companion object {
        internal val Empty: RatingsPage = RatingsPage(ratings = emptyList(), nextCursor = null)
    }
}
