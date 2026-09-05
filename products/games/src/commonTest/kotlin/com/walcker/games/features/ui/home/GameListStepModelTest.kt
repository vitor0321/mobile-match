package com.walcker.games.features.ui.home

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeAvailabilityRepository
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.fake.testGamesPreferences
import com.walcker.games.features.data.home.preferences.GamesPreferences
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCaseImpl
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.match.navigator.HomeViewCoordinator
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
class GameListStepModelTest {
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

    private fun buildModel(
        repository: FakeGameRepository,
        homeViewCoordinator: HomeViewCoordinator = HomeViewCoordinator(),
        preferences: GamesPreferences = testGamesPreferences(),
    ) = GameListStepModel(
        repository = repository,
        preferences = preferences,
        stringsHolder = stringsHolder,
        analytics = FakeAnalyticsTracker(),
        crashReporter = FakeCrashReporter(),
        homeViewCoordinator = homeViewCoordinator,
        sessionHolder = FakeSessionHolder(session = null),
        observeAvailability = ObserveAvailabilityUseCaseImpl(FakeAvailabilityRepository()),
    )

    private fun futureGame(id: String) = game(id = id, startsAtSeconds = Long.MAX_VALUE / 1000)

    @Test
    fun `preferences load before any match is shown`() =
        runTest(testDispatcher) {
            val model = buildModel(FakeGameRepository())

            advanceUntilIdle()

            assertTrue(model.state.value.preferencesLoaded)
            assertTrue(
                model.state.value.games
                    .isEmpty(),
            )
        }

    @Test
    fun `matches from the repository land in state once discoverable`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()

            assertEquals(
                listOf("match-1"),
                model.state.value.games
                    .map { it.id },
            )
            assertTrue(!model.state.value.isLoading)
        }

    @Test
    fun `a sport filter only keeps matching games`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()
            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()

            model.onEvent(GameListEvents.SelectSport(Sport.FUTEBOL))
            advanceUntilIdle()

            assertTrue(
                model.state.value.games
                    .isEmpty(),
            )
        }

    @Test
    fun `marks home data ready even when the refresh fails`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository(refreshResult = Result.failure(IllegalStateException("offline")))
            val coordinator = HomeViewCoordinator()
            buildModel(repository, coordinator)

            advanceUntilIdle()

            assertTrue(coordinator.isHomeDataReady.value)
        }

    @Test
    fun `a refresh failure surfaces an error message`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository(refreshResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(repository)

            advanceUntilIdle()

            assertEquals("offline", model.state.value.errorMessage)
            assertTrue(!model.state.value.isLoading)
        }

    @Test
    fun `selecting a game emits a navigation effect`() =
        runTest(testDispatcher) {
            val model = buildModel(FakeGameRepository())
            advanceUntilIdle()

            model.effects.test {
                model.onEvent(GameListEvents.SelectGame("match-9"))
                advanceUntilIdle()

                val effect = assertIs<GameListEffect.NavigateToMatchDetail>(awaitItem())
                assertEquals("match-9", effect.matchId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `hasMore reflects what the repository reports`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            repository.emitHasMoreMatches(true)
            advanceUntilIdle()

            assertTrue(model.state.value.hasMore)
        }

    @Test
    fun `LoadMore asks the repository for the next page`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()
            repository.emitHasMoreMatches(true)
            advanceUntilIdle()

            model.onEvent(GameListEvents.LoadMore)
            advanceUntilIdle()

            assertEquals(1, repository.loadMoreMatchesCalls.size)
            assertTrue(!model.state.value.isLoadingMore)
        }

    @Test
    fun `LoadMore is a no-op when there is nothing more to load`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(GameListEvents.LoadMore)
            advanceUntilIdle()

            assertTrue(repository.loadMoreMatchesCalls.isEmpty())
        }

    @Test
    fun `first launch with no nearby matches falls back to an unlimited nearest search`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository, preferences = testGamesPreferences(hasSyncedBefore = false))
            advanceUntilIdle()

            assertEquals(listOf(15.0, 20_000.0), repository.refreshCalls)
            assertTrue(model.state.value.isShowingNearestFallback)

            repository.emitMatches((1..25).map { futureGame("match-$it") })
            advanceUntilIdle()

            assertEquals(20, model.state.value.games.size)
            assertTrue(!model.state.value.hasMore)
        }

    @Test
    fun `first launch with nearby matches does not trigger the fallback`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository, preferences = testGamesPreferences(hasSyncedBefore = false))
            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()

            assertEquals(listOf(15.0), repository.refreshCalls)
            assertTrue(!model.state.value.isShowingNearestFallback)
        }

    @Test
    fun `a returning user does not trigger the first launch fallback`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            assertEquals(listOf(15.0), repository.refreshCalls)
            assertTrue(!model.state.value.isShowingNearestFallback)
        }
}
