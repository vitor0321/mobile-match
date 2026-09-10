package com.walcker.games.features.ui.shared.playerRatings

import app.cash.turbine.test
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakePlayerRepository
import com.walcker.games.fake.rating
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage
import com.walcker.games.features.domain.shared.usecase.GetPlayerRatingsUseCase
import com.walcker.games.features.domain.shared.usecase.GetPlayerRatingsUseCaseImpl
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerRatingsListStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = GamesStringsHolder().apply { setStrings(PtBrGamesStrings) }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(repository: FakePlayerRepository) =
        PlayerRatingsListStepModel(
            userId = "player-1",
            playerName = "Ana Souza",
            getPlayerRatings = GetPlayerRatingsUseCaseImpl(repository),
            stringsHolder = stringsHolder,
            crashReporter = FakeCrashReporter(),
        )

    private fun page(
        ids: IntRange,
        nextCursor: String?,
    ) = Result.success(
        RatingsPage(
            ratings = ids.map { rating(id = "r-$it") },
            nextCursor = nextCursor,
        ),
    )

    @Test
    fun `loads the first page sorted by most recent`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages = mapOf(null to page(1..20, nextCursor = "cursor-20")),
                )
            val model = buildModel(repository)

            advanceUntilIdle()

            val state = model.state.value
            assertEquals("Ana Souza", state.playerName)
            assertEquals(20, state.ratings.size)
            assertTrue(state.hasMore)
            assertFalse(state.isLoadingFirstPage)
            assertEquals(RatingSort.RECENT, state.sort)

            val call = repository.ratingCalls.single()
            assertEquals(GetPlayerRatingsUseCase.DEFAULT_PAGE_SIZE, call.limit)
            assertNull(call.cursor)
        }

    @Test
    fun `next page appends instead of replacing`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages =
                        mapOf(
                            null to page(1..20, nextCursor = "cursor-20"),
                            "cursor-20" to page(21..25, nextCursor = null),
                        ),
                )
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(PlayerRatingsEvents.LoadNextPage)
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(25, state.ratings.size)
            assertEquals("r-1", state.ratings.first().id)
            assertEquals("r-25", state.ratings.last().id)
            assertFalse(state.hasMore)
            assertFalse(state.isLoadingNextPage)
            assertEquals("cursor-20", repository.ratingCalls[1].cursor)
        }

    @Test
    fun `stops paging once the cursor is exhausted`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages = mapOf(null to page(1..5, nextCursor = null)),
                )
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(PlayerRatingsEvents.LoadNextPage)
            advanceUntilIdle()

            assertEquals(1, repository.ratingCalls.size)
        }

    @Test
    fun `changing the sort restarts from the first page`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages =
                        mapOf(
                            null to page(1..20, nextCursor = "cursor-20"),
                            "cursor-20" to page(21..25, nextCursor = null),
                        ),
                )
            val model = buildModel(repository)
            advanceUntilIdle()
            model.onEvent(PlayerRatingsEvents.LoadNextPage)
            advanceUntilIdle()
            assertEquals(25, model.state.value.ratings.size)

            model.onEvent(PlayerRatingsEvents.SortChanged(RatingSort.HIGHEST))
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(RatingSort.HIGHEST, state.sort)
            assertEquals(20, state.ratings.size)

            val lastCall = repository.ratingCalls.last()
            assertEquals(RatingSort.HIGHEST, lastCall.sort)
            assertNull(lastCall.cursor)
        }

    @Test
    fun `selecting the current sort does not refetch`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages = mapOf(null to page(1..5, nextCursor = null)),
                )
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(PlayerRatingsEvents.SortChanged(RatingSort.RECENT))
            advanceUntilIdle()

            assertEquals(1, repository.ratingCalls.size)
        }

    @Test
    fun `a failed first page owns the screen`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages = mapOf(null to Result.failure(IllegalStateException("sem rede"))),
                )
            val model = buildModel(repository)

            model.effects.test {
                advanceUntilIdle()

                val effect = assertIs<PlayerRatingsEffect.ShowMessage>(awaitItem())
                assertEquals("sem rede", effect.message)
                cancelAndIgnoreRemainingEvents()
            }

            val state = model.state.value
            assertEquals("sem rede", state.errorMessage)
            assertTrue(state.ratings.isEmpty())
            assertFalse(state.isLoadingFirstPage)
        }

    @Test
    fun `a failed next page keeps what is already on screen`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages =
                        mapOf(
                            null to page(1..20, nextCursor = "cursor-20"),
                            "cursor-20" to Result.failure(IllegalStateException("sem rede")),
                        ),
                )
            val model = buildModel(repository)
            advanceUntilIdle()

            model.effects.test {
                model.onEvent(PlayerRatingsEvents.LoadNextPage)
                advanceUntilIdle()

                assertIs<PlayerRatingsEffect.ShowMessage>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            val state = model.state.value
            assertEquals(20, state.ratings.size)
            assertNull(state.errorMessage)
            assertFalse(state.isLoadingNextPage)
        }

    @Test
    fun `retry reloads the first page`() =
        runTest(testDispatcher) {
            val repository =
                FakePlayerRepository(
                    ratingPages = mapOf(null to Result.failure(IllegalStateException("sem rede"))),
                )
            val model = buildModel(repository)
            advanceUntilIdle()
            assertEquals("sem rede", model.state.value.errorMessage)

            repository.ratingPages = mapOf(null to page(1..3, nextCursor = null))
            model.onEvent(PlayerRatingsEvents.Retry)
            advanceUntilIdle()

            val state = model.state.value
            assertNull(state.errorMessage)
            assertEquals(3, state.ratings.size)
        }
}
