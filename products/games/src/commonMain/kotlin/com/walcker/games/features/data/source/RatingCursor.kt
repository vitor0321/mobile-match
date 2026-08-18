package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingSort

/**
 * Encodes/decodes the opaque pagination cursor exchanged between the data layer
 * and the UI.
 *
 * A Firestore `startAfter` needs one value per `orderBy` clause, so the cursor
 * carries exactly the fields the current [RatingSort] orders by. Keeping it a
 * `String` means the domain and UI never learn the query shape — swapping the
 * ordering later does not ripple upwards.
 */
internal object RatingCursor {

    private const val SEPARATOR = "|"

    /** Builds the cursor that resumes right after [rating] under [sort]. */
    fun encode(rating: Rating, sort: RatingSort): String = when (sort) {
        RatingSort.RECENT -> rating.createdAtMs.toString()
        RatingSort.HIGHEST, RatingSort.LOWEST ->
            "${rating.rating}$SEPARATOR${rating.createdAtMs}"
    }

    /**
     * Expands [cursor] into the `startAfter` arguments for [sort].
     *
     * Returns an empty list for a null or malformed cursor, which degrades to
     * "start from the beginning" instead of throwing on a value we did not mint.
     */
    fun decode(cursor: String?, sort: RatingSort): List<Any> {
        if (cursor.isNullOrBlank()) return emptyList()

        return when (sort) {
            RatingSort.RECENT -> listOfNotNull(cursor.toLongOrNull())
            RatingSort.HIGHEST, RatingSort.LOWEST -> {
                val parts = cursor.split(SEPARATOR)
                val stars = parts.getOrNull(0)?.toIntOrNull()
                val createdAtMs = parts.getOrNull(1)?.toLongOrNull()
                if (stars == null || createdAtMs == null) emptyList() else listOf(stars, createdAtMs)
            }
        }
    }
}
