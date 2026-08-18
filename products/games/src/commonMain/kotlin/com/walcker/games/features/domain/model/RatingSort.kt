package com.walcker.games.features.domain.model

/**
 * Firestore field holding the rating's creation time, as epoch millis.
 *
 * Declared at top level rather than in [RatingSort]'s companion: enum entries are
 * initialized before their companion object, so referencing it from an entry
 * constructor is fragile.
 */
internal const val RATING_FIELD_CREATED_AT_MS: String = "createdAtMs"

/** Firestore field holding the star count (1..5). */
internal const val RATING_FIELD_STARS: String = "rating"

/**
 * Ordering applied to a player's received ratings.
 *
 * Every option maps to a Firestore `orderBy` so pagination stays server-side —
 * sorting a partially loaded list on the client would reorder only the page the
 * user already has, which reads as a bug the moment they tap "load more".
 *
 * @property primaryField Firestore field the query orders by first
 * @property descending whether [primaryField] is ordered descending
 */
internal enum class RatingSort(
    val primaryField: String,
    val descending: Boolean,
) {
    /** Newest first. Default for a review list. */
    RECENT(primaryField = RATING_FIELD_CREATED_AT_MS, descending = true),

    /** Best reviews first, newest breaking ties. */
    HIGHEST(primaryField = RATING_FIELD_STARS, descending = true),

    /** Worst reviews first, newest breaking ties. */
    LOWEST(primaryField = RATING_FIELD_STARS, descending = false),
}
