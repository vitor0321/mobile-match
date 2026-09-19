package com.walcker.games.features.data.shared.repository

import com.walcker.games.fake.FakeGameSource
import com.walcker.games.fake.game
import com.walcker.games.features.data.shared.cache.InMemoryMatchCache
import com.walcker.games.features.domain.shared.error.GamesError
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.Sport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameRepositoryImplTest {
    private fun repository(source: FakeGameSource) = GameRepositoryImpl(source = source, cache = InMemoryMatchCache())

    private val createRequest =
        CreateMatchRequest(
            sport = Sport.FUTSAL,
            venueName = "Quadra Central",
            neighborhood = "Centro",
            city = "São Paulo",
            address = "Rua Um, 100",
            lat = -23.55,
            lng = -46.63,
            geohash = "6gyf4",
            startsAtSeconds = 1_700_000_000L,
            durationMin = 60,
            totalPlayers = 10,
            recurrence = RecurrenceOption.NONE,
            pricePerPlayer = null,
        )

    @Test
    fun `refresh replaces the cache and observeMatches reflects it`() =
        runTest {
            val source = FakeGameSource(openGamesResult = { listOf(game(id = "match-1")) })
            val repository = repository(source)

            repository.refresh(radiusKm = 10.0)

            assertEquals(listOf("match-1"), repository.observeMatches().first().map { it.id })
        }

    @Test
    fun `refresh retries a failing source before giving up`() =
        runTest {
            var attempts = 0
            val source =
                FakeGameSource(
                    openGamesResult = {
                        attempts++
                        if (attempts < 2) error("network hiccup") else listOf(game())
                    },
                )

            val result = repository(source).refresh(radiusKm = 10.0)

            assertTrue(result.isSuccess)
            assertEquals(2, attempts)
        }

    @Test
    fun `refresh maps an exhausted retry into a GamesError`() =
        runTest {
            val source = FakeGameSource(openGamesResult = { error("still down") })

            val error = repository(source).refresh(radiusKm = 10.0).exceptionOrNull()

            assertIs<GamesError>(error)
            assertEquals(3, source.openGamesCallCount)
        }

    @Test
    fun `joinGame does not retry on failure`() =
        runTest {
            val source = FakeGameSource(joinGameResult = { error("full") })

            val result = repository(source).joinGame("match-1")

            assertTrue(result.isFailure)
            assertEquals(1, source.joinGameCallCount)
        }

    @Test
    fun `createMatch retries a failing source`() =
        runTest {
            var attempts = 0
            val source =
                FakeGameSource(
                    createMatchResult = {
                        attempts++
                        if (attempts < 2) error("timeout") else "match-9"
                    },
                )

            val result = repository(source).createMatch(createRequest)

            assertEquals("match-9", result.getOrThrow())
        }

    @Test
    fun `updateMatch retries a failing source`() =
        runTest {
            var attempts = 0
            val source =
                FakeGameSource(
                    updateMatchResult = {
                        attempts++
                        if (attempts < 2) error("timeout") else Unit
                    },
                )

            val result = repository(source).updateMatch("match-1", createRequest)

            assertTrue(result.isSuccess)
            assertEquals(2, attempts)
        }

    @Test
    fun `getMyMatches marks the caller as organizer on their own matches`() =
        runTest {
            val source =
                FakeGameSource(
                    matchesForUserResult = {
                        listOf(
                            game(id = "match-1").copy(organizerId = "me"),
                            game(id = "match-2").copy(organizerId = "someone-else"),
                        )
                    },
                )

            val matches = repository(source).getMyMatches("me").getOrThrow()

            val roles = matches.associate { it.game.id to it.role }
            assertEquals(MatchRole.ORGANIZER, roles.getValue("match-1"))
            assertEquals(MatchRole.PARTICIPANT, roles.getValue("match-2"))
        }

    @Test
    fun `getMyMatches failure becomes a GamesError`() =
        runTest {
            val source = FakeGameSource(matchesForUserResult = { error("NOT_FOUND: user") })

            val error = repository(source).getMyMatches("me").exceptionOrNull()

            assertIs<GamesError.NotFound>(error)
        }

    @Test
    fun `cancelMatch retries and returns the source outcome`() =
        runTest {
            var attempts = 0
            val source =
                FakeGameSource(
                    cancelMatchResult = {
                        attempts++
                        if (attempts < 2) error("timeout") else CancelMatchOutcome.Cancelled("match-1")
                    },
                )

            val result = repository(source).cancelMatch("match-1")

            assertTrue(result.isSuccess)
            assertEquals(2, attempts)
        }

    @Test
    fun `leaveMatch retries and returns the source outcome`() =
        runTest {
            var attempts = 0
            val source =
                FakeGameSource(
                    leaveMatchResult = {
                        attempts++
                        if (attempts < 2) error("timeout") else LeaveMatchOutcome("match-1")
                    },
                )

            val result = repository(source).leaveMatch("match-1")

            assertTrue(result.isSuccess)
            assertEquals(2, attempts)
        }

    @Test
    fun `getGameById wraps a permission failure into a GamesError`() =
        runTest {
            val source = FakeGameSource(getGameByIdResult = { error("PERMISSION_DENIED") })

            val error = repository(source).getGameById("match-1").exceptionOrNull()

            assertIs<GamesError.PermissionDenied>(error)
        }

    @Test
    fun `setVipStatus does not retry on failure`() =
        runTest {
            val source = FakeGameSource(setVipStatusResult = { error("not organizer") })

            val result = repository(source).setVipStatus("match-1", "target-uid", true)

            assertTrue(result.isFailure)
            assertEquals(1, source.setVipStatusCallCount)
        }

    @Test
    fun `setVipStatus returns success`() =
        runTest {
            val source = FakeGameSource(setVipStatusResult = { })

            val result = repository(source).setVipStatus("match-1", "target-uid", true)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `confirmWaitlistedPlayer does not retry on failure`() =
        runTest {
            val source = FakeGameSource(confirmWaitlistedPlayerResult = { error("not waitlisted") })

            val result = repository(source).confirmWaitlistedPlayer("match-1", "target-uid")

            assertTrue(result.isFailure)
            assertEquals(1, source.confirmWaitlistedPlayerCallCount)
        }

    @Test
    fun `confirmWaitlistedPlayer returns success`() =
        runTest {
            val source = FakeGameSource(confirmWaitlistedPlayerResult = { })

            val result = repository(source).confirmWaitlistedPlayer("match-1", "target-uid")

            assertTrue(result.isSuccess)
        }

    @Test
    fun `banPlayerFromMatch does not retry on failure`() =
        runTest {
            val source = FakeGameSource(banPlayerFromMatchResult = { error("not organizer") })

            val result = repository(source).banPlayerFromMatch("match-1", "target-uid")

            assertTrue(result.isFailure)
            assertEquals(1, source.banPlayerFromMatchCallCount)
        }

    @Test
    fun `banPlayerFromMatch returns the promoted uid`() =
        runTest {
            val source = FakeGameSource(banPlayerFromMatchResult = { "promoted-uid" })

            val result = repository(source).banPlayerFromMatch("match-1", "target-uid")

            assertEquals("promoted-uid", result.getOrNull())
        }
}
