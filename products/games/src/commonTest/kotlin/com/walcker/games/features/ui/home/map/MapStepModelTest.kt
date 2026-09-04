package com.walcker.games.features.ui.home.map

import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeLocationProvider
import com.walcker.games.fake.game
import com.walcker.games.fake.testGamesPreferences
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapStepModelTest {
    private val testDispatcher = StandardTestDispatcher()

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
    ) = MapStepModel(
        repository = repository,
        preferences = testGamesPreferences(),
        locationProvider = locationProvider,
        analytics = FakeAnalyticsTracker(),
        crashReporter = FakeCrashReporter(),
    )

    private fun futureGame(id: String) = game(id = id, startsAtSeconds = Long.MAX_VALUE / 1000)

    @Test
    fun `matches become pins once loaded`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()

            assertEquals(
                listOf("match-1"),
                model.state.value.pins
                    .map { it.matchId },
            )
            assertTrue(!model.state.value.isLoading)
        }

    @Test
    fun `without location permission there are no nearby matches`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository, FakeLocationProvider(permissionGranted = false))
            advanceUntilIdle()

            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()

            assertTrue(
                model.state.value.nearbyMatches
                    .isEmpty(),
            )
            assertTrue(model.state.value.locationUnavailable)
        }

    @Test
    fun `with a known location, matches are sorted by distance`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val locationProvider =
                FakeLocationProvider(locationResult = Result.success(Coordinates(lat = -23.55, lng = -46.63)))
            val model = buildModel(repository, locationProvider)
            advanceUntilIdle()

            repository.emitMatches(listOf(futureGame("match-1")))
            advanceUntilIdle()

            assertEquals(1, model.state.value.nearbyMatches.size)
            assertTrue(model.state.value.hasLocationPermission)
        }

    @Test
    fun `a failed location lookup surfaces locationUnavailable`() =
        runTest(testDispatcher) {
            val locationProvider = FakeLocationProvider(locationResult = Result.failure(IllegalStateException("gps off")))
            val model = buildModel(FakeGameRepository(), locationProvider)

            advanceUntilIdle()

            assertTrue(model.state.value.locationUnavailable)
        }

    @Test
    fun `retrying location clears the unavailable flag on success`() =
        runTest(testDispatcher) {
            val locationProvider = FakeLocationProvider(permissionGranted = false)
            val model = buildModel(FakeGameRepository(), locationProvider)
            advanceUntilIdle()
            assertTrue(model.state.value.locationUnavailable)

            locationProvider.permissionGranted = true
            model.onRetryLocation()
            advanceUntilIdle()

            assertTrue(!model.state.value.locationUnavailable)
            assertTrue(model.state.value.hasLocationPermission)
        }

    @Test
    fun `onRefresh toggles isRefreshing`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            val callsBeforeRefresh = repository.refreshCalls.size

            model.onRefresh()
            advanceUntilIdle()

            assertTrue(!model.state.value.isRefreshing)
            assertEquals(callsBeforeRefresh + 1, repository.refreshCalls.size)
        }
}
