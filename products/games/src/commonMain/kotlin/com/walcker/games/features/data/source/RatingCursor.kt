package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RatingSort

internal object RatingCursor {

    private const val SEPARATOR = "|"

    fun encode(rating: Rating, sort: RatingSort): String = when (sort) {
        RatingSort.RECENT -> rating.createdAtMs.toString()
        RatingSort.HIGHEST, RatingSort.LOWEST ->
            "${rating.rating}$SEPARATOR${rating.createdAtMs}"
    }

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
