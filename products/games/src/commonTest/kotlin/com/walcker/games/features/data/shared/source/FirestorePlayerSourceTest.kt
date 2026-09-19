package com.walcker.games.features.data.shared.source

import com.walcker.games.fake.FakeFirestoreClient
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.PlayerSortBy
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.Sport
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestorePlayerSourceTest {
    private fun source(firestore: FakeFirestoreClient) = FirestorePlayerSource(firestore, FirestoreRatingSource(firestore))

    private fun profile(
        name: String?,
        rating: Double = 4.0,
        ratingCount: Long = 1L,
        sports: List<String> = listOf("FUTSAL"),
        isBanned: Boolean = false,
    ): Map<String, Any?> =
        mapOf(
            "fullName" to name,
            "rating" to rating,
            "ratingCount" to ratingCount,
            "sports" to sports,
            "isBanned" to isBanned,
            "city" to "São Paulo",
            "neighborhood" to "Centro",
        )

    @Test
    fun `banned players and profiles without a name never show up in the search`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/ok", profile("Ana"))
                    seed("profiles/banned", profile("Bruno", isBanned = true))
                    seed("profiles/nameless", profile(" "))
                }

            val page = source(firestore).searchPlayers(PlayerSearchFilters(), limit = 10).getOrThrow()

            assertEquals(listOf("ok"), page.players.map { it.userId })
        }

    @Test
    fun `the search matches names ignoring case, the rating range and the sports`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/ana", profile("Ana Souza", rating = 4.5, sports = listOf("FUTSAL")))
                    seed("profiles/anabela", profile("Anabela", rating = 2.0, sports = listOf("FUTSAL")))
                    seed("profiles/mariana", profile("Mariana", rating = 4.8, sports = listOf("VOLEI")))
                }
            val filters = PlayerSearchFilters(query = "ana", minRating = 3f, favoriteSports = setOf(Sport.FUTSAL))

            val page = source(firestore).searchPlayers(filters, limit = 10).getOrThrow()

            assertEquals(listOf("ana"), page.players.map { it.userId })
        }

    @Test
    fun `a full page tells the screen there may be more`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/a", profile("A"))
                    seed("profiles/b", profile("B"))
                }

            assertTrue(source(firestore).searchPlayers(PlayerSearchFilters(), limit = 2).getOrThrow().reachedLimit)
            assertFalse(source(firestore).searchPlayers(PlayerSearchFilters(), limit = 5).getOrThrow().reachedLimit)
        }

    @Test
    fun `sorting by name is alphabetical`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/c", profile("Caio"))
                    seed("profiles/a", profile("Ana"))
                    seed("profiles/b", profile("Bia"))
                }

            val page = source(firestore).searchPlayers(PlayerSearchFilters(sortBy = PlayerSortBy.NAME), limit = 10).getOrThrow()

            assertEquals(listOf("Ana", "Bia", "Caio"), page.players.map { it.fullName })
        }

    @Test
    fun `a player profile is read with its details`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("profiles/ana", profile("Ana", rating = 4.5, ratingCount = 3L)) }

            val details = source(firestore).getPlayerDetails("ana").getOrThrow()

            assertEquals("Ana", details.fullName)
            assertEquals(4.5f, details.rating)
            assertEquals(3, details.ratingCount)
            assertEquals("São Paulo", details.city)
        }

    @Test
    fun `a missing or nameless player is a failure`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("profiles/nameless", profile(null)) }

            assertTrue(source(firestore).getPlayerDetails("gone").isFailure)
            assertTrue(source(firestore).getPlayerDetails("nameless").isFailure)
        }

    @Test
    fun `players without ratings are left out of the rating summary`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/rated", profile("A", rating = 4.2, ratingCount = 5L))
                    seed("profiles/new", profile("B", rating = 0.0, ratingCount = 0L))
                }

            val summary = source(firestore).getPlayersRatingSummary(listOf("rated", "new", "gone", "rated")).getOrThrow()

            assertEquals(mapOf("rated" to PlayerRatingSummary(rating = 4.2f, ratingCount = 5)), summary)
        }

    @Test
    fun `the organizer reputation is read from the fields the server writes`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/org", profile("Org", rating = 1.0) + mapOf("asOrganizerRating" to 4.7, "asOrganizerRatingCount" to 9L))
                }

            val summary = source(firestore).getOrganizerRatingSummary("org").getOrThrow()

            assertEquals(PlayerRatingSummary(rating = 4.7f, ratingCount = 9), summary)
        }

    @Test
    fun `an organizer never rated has no reputation yet`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("profiles/org", profile("Org")) }

            assertNull(source(firestore).getOrganizerRatingSummary("org").getOrThrow())
        }

    @Test
    fun `the skill rating is read from the fields the server writes`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("profiles/p", profile("P") + mapOf("skillRating" to 3.5, "skillRatingCount" to 2L))
                }

            val summary = source(firestore).getPlayersSkillRatingSummary(listOf("p")).getOrThrow()

            assertEquals(mapOf("p" to PlayerRatingSummary(rating = 3.5f, ratingCount = 2)), summary)
        }

    @Test
    fun `a player's ratings come from the rating source`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed(
                        "profiles/p/ratings/r1",
                        mapOf("matchId" to "m1", "ratedUserId" to "p", "raterUserId" to "x", "rating" to 5L, "comment" to "", "createdAtMs" to 1L),
                    )
                }

            val page = source(firestore).getPlayerRatings("p", limit = 10, sort = RatingSort.RECENT, cursor = null).getOrThrow()

            assertEquals(listOf("r1"), page.ratings.map { it.id })
        }
}
