package com.walcker.games.features.data.shared.repository

import com.walcker.games.fake.FakeRatingSource
import com.walcker.games.fake.playerDetails
import com.walcker.games.features.data.shared.cache.InMemoryPlayerCache
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RatingRepositoryImplTest {
    private fun repository(
        source: FakeRatingSource,
        cache: InMemoryPlayerCache = InMemoryPlayerCache(),
    ) = RatingRepositoryImpl(ratingSource = source, playerCache = cache)

    @Test
    fun `submitting a rating invalidates the rated player's cache entry`() =
        runTest {
            val cache = InMemoryPlayerCache()
            cache.putDetails("player-2", playerDetails(userId = "player-2"))
            val source = FakeRatingSource()

            repository(source, cache).submitPlayerRating(
                matchId = "match-1",
                ratedUserId = "player-2",
                rating = 5,
                comment = "",
            )

            assertNull(cache.details("player-2"))
        }

    @Test
    fun `a failed submission leaves the cache untouched`() =
        runTest {
            val cache = InMemoryPlayerCache()
            cache.putDetails("player-2", playerDetails(userId = "player-2"))
            val source = FakeRatingSource(submitResult = Result.failure(IllegalStateException("boom")))

            repository(source, cache).submitPlayerRating(
                matchId = "match-1",
                ratedUserId = "player-2",
                rating = 5,
                comment = "",
            )

            assertNotNull(cache.details("player-2"))
        }

    @Test
    fun `returns the outcome the source reports`() =
        runTest {
            val outcome = SubmitRatingOutcome.Updated(averageRating = 4.2f, ratingCount = 5)
            val source = FakeRatingSource(submitResult = Result.success(outcome))

            val result =
                repository(source).submitPlayerRating(
                    matchId = "match-1",
                    ratedUserId = "player-2",
                    rating = 4,
                    comment = "",
                )

            assertEquals(outcome, result.getOrThrow())
        }

    @Test
    fun `getUserRatings and getMatchLocationRatings pass through unchanged`() =
        runTest {
            val source = FakeRatingSource()

            val repo = repository(source)
            assertTrue(repo.getUserRatings("player-1").isSuccess)
            assertTrue(repo.getMatchLocationRatings("match-1").isSuccess)
        }

    @Test
    fun `getRatingsGivenForMatch passes through to the source`() =
        runTest {
            val existing =
                Rating(
                    id = "organizer-1_player-2",
                    matchId = "match-1",
                    ratedUserId = "player-2",
                    raterUserId = "organizer-1",
                    rating = 4,
                    comment = "",
                    createdAtMs = 1_000L,
                )
            val source = FakeRatingSource(ratingsGivenForMatchResult = Result.success(listOf(existing)))

            val result = repository(source).getRatingsGivenForMatch(matchId = "match-1", raterUserId = "organizer-1")

            assertEquals(listOf(existing), result.getOrThrow())
        }
}
