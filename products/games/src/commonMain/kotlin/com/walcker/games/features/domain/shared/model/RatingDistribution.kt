package com.walcker.games.features.domain.shared.model

internal data class RatingDistribution(
    val counts: List<Int>,
) {
    init {
        require(counts.size == STAR_LEVELS) {
            "counts must have $STAR_LEVELS entries (1..5 stars), was ${counts.size}"
        }
    }

    val total: Int get() = counts.sum()

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
