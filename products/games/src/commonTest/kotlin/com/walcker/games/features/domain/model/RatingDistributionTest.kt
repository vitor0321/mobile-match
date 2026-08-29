package com.walcker.games.features.domain.model

import com.walcker.games.fake.rating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RatingDistributionTest {

    @Test
    fun `empty distribution has no total and a zero average`() {
        assertEquals(0, RatingDistribution.Empty.total)
        assertEquals(0f, RatingDistribution.Empty.average)
    }

    @Test
    fun `buckets ratings by star level`() {
        val ratings = listOf(
            rating(id = "1", stars = 5),
            rating(id = "2", stars = 5),
            rating(id = "3", stars = 3),
            rating(id = "4", stars = 1),
        )

        val distribution = ratings.toDistribution()

        assertEquals(listOf(1, 0, 1, 0, 2), distribution.counts)
        assertEquals(4, distribution.total)
    }

    @Test
    fun `average weights each star level`() {
        val ratings = listOf(
            rating(id = "1", stars = 5),
            rating(id = "2", stars = 4),
            rating(id = "3", stars = 3),
        )

        assertEquals(4f, ratings.toDistribution().average)
    }

    @Test
    fun `ignores star values outside the valid range instead of crashing`() {
        val ratings = listOf(
            rating(id = "1", stars = 0),
            rating(id = "2", stars = 9),
            rating(id = "3", stars = 4),
        )

        val distribution = ratings.toDistribution()

        assertEquals(listOf(0, 0, 0, 1, 0), distribution.counts)
        assertEquals(1, distribution.total)
    }

    @Test
    fun `rejects a malformed counts list`() {
        assertFailsWith<IllegalArgumentException> {
            RatingDistribution(counts = listOf(1, 2, 3))
        }
    }
}
