package com.walcker.games.features.ui.search

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeAvailabilityRepository
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeLocationProvider
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCaseImpl
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.NearbyMatchesPage
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.home.map.MapCamera
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.match.core.geo.Coordinates
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

    private fun buildModel(
        repository: FakeGameRepository,
        locationProvider: FakeLocationProvider = FakeLocationProvider(),
    ) = SearchStepModel(
        repository = repository,
        stringsHolder = stringsHolder,
        analytics = FakeAnalyticsTracker(),
        sessionHolder = FakeSessionHolder(session = null),
        observeAvailability = ObserveAvailabilityUseCaseImpl(FakeAvailabilityRepository()),
        locationProvider = locationProvider,
        crashReporter = FakeCrashReporter(),
    )

    // isDiscoverable() drops anything already started, so fixtures need a start far in the future.
    private fun futureGame(id: String) = game(id = id, startsAtSeconds = Long.MAX_VALUE / 1000)

    private fun pageOf(games: List<Game>) = Result.success(NearbyMatchesPage(games = games, rangeCursors = emptyList()))

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
            val repository = FakeGameRepository(searchMatchesResult = pageOf(listOf(futureGame("match-1"), futureGame("match-2"))))
            val model = buildModel(repository)
            advanceUntilIdle()

            assertEquals(2, model.state.value.results.size)
        }

    @Test
    fun `the query filters by venue, neighborhood, city and sport label`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository(searchMatchesResult = pageOf(listOf(futureGame("match-1"), futureGame("match-2"))))
            val model = buildModel(repository)
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
            val repository = FakeGameRepository(searchMatchesResult = pageOf(listOf(futureGame("match-1"))))
            val model = buildModel(repository)
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
            val repository = FakeGameRepository(searchMatchesResult = pageOf(listOf(futureGame("match-1"))))
            val model = buildModel(repository)
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
    fun `only the first page of results is visible until load more is requested`() =
        runTest(testDispatcher) {
            val games = (1..25).map { futureGame("match-$it") }
            val repository = FakeGameRepository(searchMatchesResult = pageOf(games))
            val model = buildModel(repository)
            advanceUntilIdle()

            assertEquals(25, model.state.value.results.size)
            assertEquals(SEARCH_RESULTS_PAGE_SIZE, model.state.value.visibleResults.size)
            assertTrue(model.state.value.hasMoreResults)

            model.onEvent(SearchEvents.LoadMoreResults)
            advanceUntilIdle()

            assertEquals(25, model.state.value.visibleResults.size)
            assertTrue(!model.state.value.hasMoreResults)
        }

    @Test
    fun `changing the query resets pagination back to the first page`() =
        runTest(testDispatcher) {
            val games = (1..25).map { futureGame("match-$it") }
            val repository = FakeGameRepository(searchMatchesResult = pageOf(games))
            val model = buildModel(repository)
            advanceUntilIdle()
            model.onEvent(SearchEvents.LoadMoreResults)
            advanceUntilIdle()
            assertEquals(25, model.state.value.visibleResults.size)

            model.onEvent(SearchEvents.QueryChanged("centro"))
            advanceUntilIdle()

            assertEquals(SEARCH_RESULTS_PAGE_SIZE, model.state.value.visibleResults.size)
        }

    @Test
    fun `results accumulate across multiple pages from the repository`() =
        runTest(testDispatcher) {
            val firstPageGames = (1..25).map { futureGame("match-$it") }
            val secondPageGames = listOf(futureGame("match-26"))
            val repository =
                FakeGameRepository(
                    searchMatchesResults =
                        mutableListOf(
                            Result.success(NearbyMatchesPage(games = firstPageGames, rangeCursors = listOf("cursor-1"))),
                            Result.success(NearbyMatchesPage(games = secondPageGames, rangeCursors = emptyList())),
                        ),
                )
            val model = buildModel(repository)
            advanceUntilIdle()

            assertEquals(26, model.state.value.results.size)
        }

    @Test
    fun `opening the map searches near the user's location`() =
        runTest(testDispatcher) {
            val locationProvider =
                FakeLocationProvider(locationResult = Result.success(Coordinates(lat = -20.3155, lng = -40.3128)))
            val repository = FakeGameRepository(searchMatchesNearResult = pageOf(listOf(futureGame("match-1"))))
            val model = buildModel(repository, locationProvider)
            advanceUntilIdle()

            model.onEvent(SearchEvents.ToggleMap)
            advanceUntilIdle()

            assertEquals(1, repository.searchMatchesNearCalls.size)
            assertEquals(Coordinates(lat = -20.3155, lng = -40.3128), repository.searchMatchesNearCalls.first().first)
            assertEquals(1, model.state.value.mapResults.size)
            assertEquals(-20.3155, model.state.value.mapCamera.lat)
            assertTrue(!model.state.value.isMapLoading)
        }

    @Test
    fun `opening the map falls back to the default camera without location permission`() =
        runTest(testDispatcher) {
            val locationProvider = FakeLocationProvider(permissionGranted = false)
            val repository = FakeGameRepository()
            val model = buildModel(repository, locationProvider)
            advanceUntilIdle()

            model.onEvent(SearchEvents.ToggleMap)
            advanceUntilIdle()

            assertEquals(DEFAULT_SEARCH_MAP_CAMERA, model.state.value.mapCamera)
        }

    @Test
    fun `opening the map a second time does not search again`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(SearchEvents.ToggleMap)
            advanceUntilIdle()
            model.onEvent(SearchEvents.ToggleMap)
            model.onEvent(SearchEvents.ToggleMap)
            advanceUntilIdle()

            assertEquals(1, repository.searchMatchesNearCalls.size)
        }

    @Test
    fun `panning the map searches the new area and replaces the pins`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository(searchMatchesNearResult = pageOf(listOf(futureGame("match-1"))))
            val model = buildModel(repository)
            advanceUntilIdle()
            model.onEvent(SearchEvents.ToggleMap)
            advanceUntilIdle()

            val newCamera = MapCamera(lat = -22.9068, lng = -43.1729, zoom = 14f)
            repository.searchMatchesNearResult = pageOf(listOf(futureGame("match-2"), futureGame("match-3")))
            model.onEvent(SearchEvents.MapCameraIdle(newCamera))
            advanceUntilIdle()

            assertEquals(2, repository.searchMatchesNearCalls.size)
            assertEquals(Coordinates(lat = -22.9068, lng = -43.1729), repository.searchMatchesNearCalls.last().first)
            assertEquals(2, model.state.value.mapResults.size)
            assertEquals(newCamera, model.state.value.mapCamera)
        }

    @Test
    fun `a failed map search surfaces an error that retry clears`() =
        runTest(testDispatcher) {
            val repository =
                FakeGameRepository(searchMatchesNearResult = Result.failure(IllegalStateException("boom")))
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(SearchEvents.ToggleMap)
            advanceUntilIdle()

            assertEquals(
                stringsHolder.strings.search.loadErrorMessage,
                model.state.value.mapErrorMessage,
            )
            assertTrue(!model.state.value.isMapLoading)

            model.onEvent(SearchEvents.MapErrorDismissed)
            assertEquals(null, model.state.value.mapErrorMessage)

            repository.searchMatchesNearResult = pageOf(listOf(futureGame("match-1")))
            model.onEvent(SearchEvents.MapRetry)
            advanceUntilIdle()

            assertEquals(null, model.state.value.mapErrorMessage)
            assertEquals(1, model.state.value.mapResults.size)
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
