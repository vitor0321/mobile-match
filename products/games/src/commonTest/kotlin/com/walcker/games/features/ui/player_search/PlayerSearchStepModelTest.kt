package com.walcker.games.features.ui.player_search

import com.walcker.games.features.domain.model.PlayerSearchResults
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.features.domain.usecase.SearchPlayersUseCaseImpl
import com.walcker.games.fake.FakePlayerRepository
import com.walcker.games.fake.playerSearchResult
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerSearchStepModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = GamesStringsHolder().apply { setStrings(PtBrGamesStrings) }
    private val debounceMs = 300L

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(repository: FakePlayerRepository) = PlayerSearchStepModel(
        searchPlayersUseCase = SearchPlayersUseCaseImpl(repository),
        stringsHolder = stringsHolder,
        debounceMs = debounceMs,
    )

    private fun results(vararg names: String, reachedLimit: Boolean = false) =
        Result.success(
            PlayerSearchResults(
                players = names.mapIndexed { index, name ->
                    playerSearchResult(userId = "p$index", displayName = name)
                },
                reachedLimit = reachedLimit,
            ),
        )

    @Test
    fun `starts idle and searches nothing`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository()
        val model = buildModel(repository)

        advanceUntilIdle()

        assertTrue(model.state.value.isIdle)
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `typing fires a single search after the debounce`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(searchResult = results("Ana Souza"))
        val model = buildModel(repository)

        model.onEvent(PlayerSearchEvents.QueryChanged("a"))
        advanceTimeBy(100)
        model.onEvent(PlayerSearchEvents.QueryChanged("an"))
        advanceTimeBy(100)
        model.onEvent(PlayerSearchEvents.QueryChanged("ana"))
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls.size)
        assertEquals("ana", repository.searchCalls.single().query)
        assertEquals(1, model.state.value.results.size)
    }

    @Test
    fun `a pause between keystrokes produces two searches`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(searchResult = results("Ana Souza"))
        val model = buildModel(repository)

        model.onEvent(PlayerSearchEvents.QueryChanged("an"))
        advanceUntilIdle()
        model.onEvent(PlayerSearchEvents.QueryChanged("ana"))
        advanceUntilIdle()

        assertEquals(listOf("an", "ana"), repository.searchCalls.map { it.query })
    }

    @Test
    fun `the query is trimmed and travels inside the filters`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(searchResult = results("Ana Souza"))
        val model = buildModel(repository)

        model.onEvent(PlayerSearchEvents.QueryChanged("  ana  "))
        advanceUntilIdle()

        assertEquals("ana", repository.searchCalls.single().query)
    }

    @Test
    fun `clearing the query goes back to idle without querying`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(searchResult = results("Ana Souza"))
        val model = buildModel(repository)

        model.onEvent(PlayerSearchEvents.QueryChanged("ana"))
        advanceUntilIdle()
        model.onEvent(PlayerSearchEvents.QueryChanged(""))
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls.size)
        assertTrue(model.state.value.isIdle)
        assertTrue(model.state.value.results.isEmpty())
        assertFalse(model.state.value.isLoading)
    }

    @Test
    fun `a filter with no query still searches`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(searchResult = results("Ana Souza"))
        val model = buildModel(repository)

        model.onEvent(PlayerSearchEvents.SportsFilterChanged(setOf(Sport.entries.first())))
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls.size)
        assertFalse(model.state.value.isIdle)
    }

    @Test
    fun `resetting filters clears results immediately`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(searchResult = results("Ana Souza"))
        val model = buildModel(repository)
        model.onEvent(PlayerSearchEvents.QueryChanged("ana"))
        advanceUntilIdle()

        model.onEvent(PlayerSearchEvents.ResetFilters)
        advanceUntilIdle()

        val state = model.state.value
        assertTrue(state.isIdle)
        assertTrue(state.results.isEmpty())
        assertFalse(state.showFiltersPanel)
        assertEquals(1, repository.searchCalls.size)
    }

    @Test
    fun `the read cap is surfaced to the UI`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            searchResult = results("Ana Souza", reachedLimit = true),
        )
        val model = buildModel(repository)

        model.onEvent(PlayerSearchEvents.QueryChanged("a"))
        advanceUntilIdle()

        assertTrue(model.state.value.reachedLimit)
    }

    @Test
    fun `a failure surfaces a message and stops the spinner`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            searchResult = Result.failure(IllegalStateException("sem rede")),
        )
        val model = buildModel(repository)

        model.onEvent(PlayerSearchEvents.QueryChanged("ana"))
        advanceUntilIdle()

        val state = model.state.value
        assertEquals("sem rede", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a new search clears the previous error`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            searchResult = Result.failure(IllegalStateException("sem rede")),
        )
        val model = buildModel(repository)
        model.onEvent(PlayerSearchEvents.QueryChanged("ana"))
        advanceUntilIdle()

        repository.searchResult = results("Ana Souza")
        model.onEvent(PlayerSearchEvents.QueryChanged("ana s"))
        advanceUntilIdle()

        assertNull(model.state.value.errorMessage)
        assertEquals(1, model.state.value.results.size)
    }
}
