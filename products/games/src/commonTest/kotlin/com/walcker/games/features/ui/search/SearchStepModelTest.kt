package com.walcker.games.features.ui.search

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.game
import com.walcker.games.features.domain.shared.model.Sport
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchStepModelTest {
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

    private fun buildModel(repository: FakeGameRepository) =
        SearchStepModel(
            repository = repository,
            stringsHolder = stringsHolder,
            analytics = FakeAnalyticsTracker(),
        )

    // isDiscoverable() drops anything already started, so fixtures need a start far in the future.
    private fun futureGame(id: String) = game(id = id, startsAtSeconds = Long.MAX_VALUE / 1000)

    @Test
    fun `starts empty until matches arrive from the repository`() =
        runTest(testDispatcher) {
            val model = buildModel(FakeGameRepository())

            advanceUntilIdle()

            assertTrue(
                model.state.value.results
                    .isEmpty(),
            )
        }

    @Test
    fun `an empty query matches every discoverable game`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            repository.emitMatches(listOf(futureGame("match-1"), futureGame("match-2")))
            advanceUntilIdle()

            assertEquals(2, model.state.value.results.size)
        }

    @Test
    fun `the query filters by venue, neighborhood, city and sport label`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            repository.emitMatches(listOf(futureGame("match-1"), futureGame("match-2")))
            advanceUntilIdle()

            model.onEvent(SearchEvents.QueryChanged("centro"))
            advanceUntilIdle()

            assertEquals(2, model.state.value.results.size)

            model.onEvent(SearchEvents.QueryChanged("bairro que não existe"))
            advanceUntilIdle()

            assertTrue(
                model.state.value.results
                    .isEmpty(),
            )
        }

    @Test
    fun `a sport filter narrows the results`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()

            model.onEvent(SearchEvents.SportFilterChanged(setOf(Sport.FUTEBOL)))
            advanceUntilIdle()

            assertTrue(
                model.state.value.results
                    .isEmpty(),
            )

            model.onEvent(SearchEvents.SportFilterChanged(setOf(Sport.FUTSAL)))
            advanceUntilIdle()

            assertEquals(1, model.state.value.results.size)
        }

    @Test
    fun `resetting filters clears the query and reapplies to the full list`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()
            model.onEvent(SearchEvents.QueryChanged("não existe"))
            advanceUntilIdle()
            assertTrue(
                model.state.value.results
                    .isEmpty(),
            )

            model.onEvent(SearchEvents.ResetFilters)
            advanceUntilIdle()

            val state = model.state.value
            assertEquals("", state.query)
            assertEquals(1, state.results.size)
            assertTrue(!state.showFiltersPanel)
        }

    @Test
    fun `toggling the filters panel flips its visibility`() =
        runTest(testDispatcher) {
            val model = buildModel(FakeGameRepository())

            model.onEvent(SearchEvents.ToggleFiltersPanel)
            assertTrue(model.state.value.showFiltersPanel)

            model.onEvent(SearchEvents.ToggleFiltersPanel)
            assertTrue(!model.state.value.showFiltersPanel)
        }

    @Test
    fun `selecting a game emits a navigation effect`() =
        runTest(testDispatcher) {
            val model = buildModel(FakeGameRepository())

            model.effects.test {
                model.onEvent(SearchEvents.SelectGame("match-9"))
                advanceUntilIdle()

                val effect = assertIs<SearchEffect.NavigateToMatchDetail>(awaitItem())
                assertEquals("match-9", effect.matchId)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
