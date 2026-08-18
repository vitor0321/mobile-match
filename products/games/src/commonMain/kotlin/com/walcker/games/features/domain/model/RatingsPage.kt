package com.walcker.games.features.domain.model

/**
 * One page of a player's ratings plus the cursor needed to ask for the next one.
 *
 * @param ratings the items in this page, already in [RatingSort] order
 * @param nextCursor opaque token for the following page; `null` means the list
 *        is exhausted, so the UI can hide its "load more" affordance
 */
internal data class RatingsPage(
    val ratings: List<Rating>,
    val nextCursor: String?,
) {
    val hasMore: Boolean get() = nextCursor != null

    internal companion object {
        internal val Empty: RatingsPage = RatingsPage(ratings = emptyList(), nextCursor = null)
    }
}
