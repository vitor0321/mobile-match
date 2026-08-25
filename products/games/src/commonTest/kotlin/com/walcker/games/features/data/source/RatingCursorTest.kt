package com.walcker.games.features.data.source

import com.walcker.games.fake.rating
import com.walcker.games.features.domain.model.RatingSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RatingCursorTest {

    private val last = rating(id = "r-9", stars = 4, createdAtMs = 1_700_000_000_000L)

    @Test
    fun `recent cursor carries only the timestamp`() {
        val cursor = RatingCursor.encode(last, RatingSort.RECENT)

        assertEquals("1700000000000", cursor)
        assertEquals(listOf(1_700_000_000_000L), RatingCursor.decode(cursor, RatingSort.RECENT))
    }

    @Test
    fun `star ordered cursor carries stars and timestamp in orderBy order`() {
        val cursor = RatingCursor.encode(last, RatingSort.HIGHEST)

        assertEquals("4|1700000000000", cursor)
        assertEquals(
            listOf<Any>(4, 1_700_000_000_000L),
            RatingCursor.decode(cursor, RatingSort.HIGHEST),
        )
    }

    @Test
    fun `a null cursor means start from the beginning`() {
        assertTrue(RatingCursor.decode(null, RatingSort.RECENT).isEmpty())
        assertTrue(RatingCursor.decode("  ", RatingSort.LOWEST).isEmpty())
    }

    @Test
    fun `a malformed cursor degrades to the first page instead of throwing`() {
        assertTrue(RatingCursor.decode("not-a-number", RatingSort.RECENT).isEmpty())
        assertTrue(RatingCursor.decode("4", RatingSort.HIGHEST).isEmpty())
        assertTrue(RatingCursor.decode("x|y", RatingSort.LOWEST).isEmpty())
    }
}
