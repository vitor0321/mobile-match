package com.walcker.games.features.domain.model

/**
 * How a player's ratings spread across the 1..5 star levels.
 *
 * Computed on the client from the ratings sample already in memory (decision D10
 * in the roadmap: ratings are not aggregated server-side). It therefore describes
 * the loaded sample, not necessarily every rating the player ever received — the
 * authoritative totals stay on the profile document.
 *
 * @param counts one entry per star level, ascending: index `0` is 1 star.
 */
internal data class RatingDistribution(
    val counts: List<Int>,
) {
    init {
        require(counts.size == STAR_LEVELS) {
            "counts must have $STAR_LEVELS entries (1..5 stars), was ${counts.size}"
        }
    }

    /** How many ratings this distribution was built from. */
    val total: Int get() = counts.sum()

    /** Mean of the sample, or `0f` when there is nothing to average. */
    val average: Float
        get() {
            val total = total
            if (total == 0) return 0f
            val weighted = counts.foldIndexed(0) { index, acc, count -> acc + (index + 1) * count }
            return weighted.toFloat() / total
        }

    internal companion object {
        internal const val STAR_LEVELS: Int = 5
        internal val Empty: RatingDistribution = RatingDistribution(counts = List(STAR_LEVELS) { 0 })
    }
}

/**
 * Buckets a rating sample by star level. Values outside `1..5` are ignored
 * rather than crashing — a malformed document should not take the screen down.
 */
internal fun List<Rating>.toDistribution(): RatingDistribution {
    val counts = MutableList(RatingDistribution.STAR_LEVELS) { 0 }
    for (rating in this) {
        val index = rating.rating - 1
        if (index in counts.indices) {
            counts[index] = counts[index] + 1
        }
    }
    return RatingDistribution(counts = counts)
}
