package com.walcker.games.features.data.shared.source

import com.walcker.games.fake.FakeFirestoreClient
import com.walcker.games.fake.FunctionCall
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreRatingSourceTest {
    private fun ratingDoc(
        stars: Long,
        createdAtMs: Long,
        rater: String = "r1",
        rated: String = "p1",
        omit: String? = null,
    ): Map<String, Any?> =
        mapOf(
            "matchId" to "m1",
            "ratedUserId" to rated,
            "raterUserId" to rater,
            "rating" to stars,
            "comment" to "ok",
            "createdAtMs" to createdAtMs,
        ).filterKeys { it != omit }

    @Test
    fun `each rating kind calls its own function with its own arguments`() =
        runTest {
            val firestore = FakeFirestoreClient()
            listOf("submitPlayerRating", "submitMatchRating", "submitOrganizerRating", "submitSkillRating").forEach {
                firestore.functionResults[it] = Result.success(mapOf("status" to "recorded"))
            }
            val source = FirestoreRatingSource(firestore)

            source.submitPlayerRating("m1", "p1", 5, "boa")
            source.submitMatchRating("m1", 4)
            source.submitOrganizerRating("m1", 3)
            source.submitSkillRating("m1", "p1", 2)

            assertEquals(
                listOf(
                    FunctionCall("submitPlayerRating", mapOf("matchId" to "m1", "ratedUserId" to "p1", "rating" to 5, "comment" to "boa")),
                    FunctionCall("submitMatchRating", mapOf("matchId" to "m1", "rating" to 4)),
                    FunctionCall("submitOrganizerRating", mapOf("matchId" to "m1", "rating" to 3)),
                    FunctionCall("submitSkillRating", mapOf("matchId" to "m1", "ratedUserId" to "p1", "rating" to 2)),
                ),
                firestore.functionCalls,
            )
        }

    @Test
    fun `every answer of the server carries the new average`() =
        runTest {
            val firestore = FakeFirestoreClient()
            val source = FirestoreRatingSource(firestore)

            firestore.functionResults["submitPlayerRating"] = Result.success(mapOf("status" to "recorded", "averageRating" to 4.5, "ratingCount" to 2L))
            assertEquals(SubmitRatingOutcome.Recorded(4.5f, 2), source.submitPlayerRating("m1", "p1", 5, "").getOrThrow())

            firestore.functionResults["submitPlayerRating"] = Result.success(mapOf("status" to "updated", "averageRating" to 4L, "ratingCount" to 2L))
            assertEquals(SubmitRatingOutcome.Updated(4f, 2), source.submitPlayerRating("m1", "p1", 4, "").getOrThrow())

            firestore.functionResults["submitPlayerRating"] = Result.success(mapOf("status" to "already_rated"))
            assertEquals(SubmitRatingOutcome.AlreadyRated(0f, 0), source.submitPlayerRating("m1", "p1", 4, "").getOrThrow())
        }

    @Test
    fun `an unknown answer is a failure`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["submitMatchRating"] = Result.success(mapOf("status" to "???")) }

            assertTrue(FirestoreRatingSource(firestore).submitMatchRating("m1", 3).isFailure)
        }

    @Test
    fun `received ratings come newest first and continue on the next page`() =
        runTest {
            val firestore = FakeFirestoreClient()
            (1..5).forEach { firestore.seed("profiles/p1/ratings/r$it", ratingDoc(stars = 5, createdAtMs = it * 1_000L, rater = "r$it")) }
            val source = FirestoreRatingSource(firestore)

            val first = source.getUserRatingsPage("p1", limit = 3, sort = RatingSort.RECENT, cursor = null).getOrThrow()
            val second = source.getUserRatingsPage("p1", limit = 3, sort = RatingSort.RECENT, cursor = first.nextCursor).getOrThrow()

            assertEquals(listOf("r5", "r4", "r3"), first.ratings.map { it.id })
            assertEquals(listOf("r2", "r1"), second.ratings.map { it.id })
            assertNull(second.nextCursor)
        }

    @Test
    fun `the highest ratings come first`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/p1/ratings/low", ratingDoc(stars = 2, createdAtMs = 3_000L))
                    seed("profiles/p1/ratings/top", ratingDoc(stars = 5, createdAtMs = 1_000L))
                    seed("profiles/p1/ratings/mid", ratingDoc(stars = 4, createdAtMs = 2_000L))
                }

            val page = FirestoreRatingSource(firestore).getUserRatingsPage("p1", limit = 10, sort = RatingSort.HIGHEST, cursor = null).getOrThrow()

            assertEquals(listOf("top", "mid", "low"), page.ratings.map { it.id })
        }

    @Test
    fun `a malformed rating in a full page does not stop the pagination`() =
        runTest {
            val firestore = FakeFirestoreClient()
            (1..4).forEach { index ->
                firestore.seed("profiles/p1/ratings/r$index", ratingDoc(stars = 5, createdAtMs = index * 1_000L, omit = if (index == 4) "matchId" else null))
            }
            val source = FirestoreRatingSource(firestore)

            val first = source.getUserRatingsPage("p1", limit = 3, sort = RatingSort.RECENT, cursor = null).getOrThrow()
            val second = source.getUserRatingsPage("p1", limit = 3, sort = RatingSort.RECENT, cursor = first.nextCursor).getOrThrow()

            assertTrue("r1" in (first.ratings + second.ratings).map { it.id }, "cursor after the first page was ${first.nextCursor}")
        }

    @Test
    fun `the ratings the organizer gave are only theirs`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("matches/m1/ratings/a", ratingDoc(stars = 5, createdAtMs = 1L, rater = "org", rated = "a"))
                    seed("matches/m1/ratings/b", ratingDoc(stars = 3, createdAtMs = 2L, rater = "someone", rated = "b"))
                }

            val given = FirestoreRatingSource(firestore).getRatingsGivenForMatch("m1", "org").getOrThrow()

            assertEquals(listOf("a"), given.map { it.ratedUserId })
        }

    @Test
    fun `my skill ratings are read per player and missing ones are skipped`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("profiles/a/skillRatings/org", mapOf("rating" to 4L)) }

            val ratings = FirestoreRatingSource(firestore).getMySkillRatings("org", listOf("a", "b", "a")).getOrThrow()

            assertEquals(mapOf("a" to 4), ratings)
        }
}
