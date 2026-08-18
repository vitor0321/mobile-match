package com.walcker.games.features.data.repository

import com.walcker.games.features.data.cache.InMemoryPlayerCache
import com.walcker.games.features.domain.error.GamesError
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.fake.FakePlayerSource
import com.walcker.games.fake.playerSearchResultDto
import com.walcker.games.features.data.source.PlayerSearchPageDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlayerRepositoryImplTest {

    private var now = 0L

    private fun repository(
        source: FakePlayerSource,
        ttlMs: Long = InMemoryPlayerCache.DEFAULT_TTL_MS,
    ) = PlayerRepositoryImpl(
        source = source,
        cache = InMemoryPlayerCache(ttlMs = ttlMs, nowMs = { now }),
    )

    private val filters = PlayerSearchFilters(query = "ana")

    @Test
    fun `maps the profile fields the documents actually use`() = runTest {
        val source = FakePlayerSource(
            searchResult = Result.success(
                PlayerSearchPageDto(
                    players = listOf(playerSearchResultDto(fullName = "Ana Souza")),
                    reachedLimit = false,
                ),
            ),
        )

        val results = repository(source).searchPlayers(filters).getOrThrow()

        // fullName -> displayName is the whole point: reading `displayName` off
        // the document returned null for every profile and emptied the search.
        assertEquals("Ana Souza", results.players.single().displayName)
    }

    @Test
    fun `a repeated search inside the ttl does not hit the source again`() = runTest {
        val source = FakePlayerSource()
        val repository = repository(source)

        repository.searchPlayers(filters)
        repository.searchPlayers(filters)

        assertEquals(1, source.searchCallCount)
    }

    @Test
    fun `a different filter set is a different search`() = runTest {
        val source = FakePlayerSource()
        val repository = repository(source)

        repository.searchPlayers(filters)
        repository.searchPlayers(filters.copy(minRating = 4f))

        assertEquals(2, source.searchCallCount)
    }

    @Test
    fun `the source is queried again once the entry expires`() = runTest {
        val source = FakePlayerSource()
        val repository = repository(source, ttlMs = 1_000L)

        repository.searchPlayers(filters)
        now = 1_500L
        repository.searchPlayers(filters)

        assertEquals(2, source.searchCallCount)
    }

    @Test
    fun `details are cached per player`() = runTest {
        val source = FakePlayerSource()
        val repository = repository(source)

        repository.getPlayerDetails("player-1")
        repository.getPlayerDetails("player-1")

        assertEquals(1, source.detailsCallCount)
    }

    @Test
    fun `a failure is not cached`() = runTest {
        val source = FakePlayerSource(
            searchResult = Result.failure(IllegalStateException("sem rede")),
        )
        val repository = repository(source)

        assertTrue(repository.searchPlayers(filters).isFailure)
        assertTrue(repository.searchPlayers(filters).isFailure)

        // Caching an error would keep the screen broken for five minutes.
        assertEquals(2, source.searchCallCount)
    }

    @Test
    fun `a missing profile becomes a domain NotFound`() = runTest {
        val source = FakePlayerSource(
            detailsResult = Result.failure(NoSuchElementException("Player x not found.")),
        )

        val error = repository(source).getPlayerDetails("x").exceptionOrNull()

        assertIs<GamesError.NotFound>(error)
    }
}
