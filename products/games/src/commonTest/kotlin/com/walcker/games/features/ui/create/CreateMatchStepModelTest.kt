package com.walcker.games.features.ui.create

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeAvailabilityRepository
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeLocationProvider
import com.walcker.games.fake.FakeReverseGeocoder
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.features.domain.create.usecase.CreateMatchUseCaseImpl
import com.walcker.games.features.domain.create.usecase.UpdateMatchUseCaseImpl
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCaseImpl
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.geo.DefaultCenter
import com.walcker.match.navigator.MainTab
import com.walcker.match.navigator.TabCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateMatchStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = GamesStringsHolder().apply { setStrings(PtBrGamesStrings) }
    private val dateMillis = Instant.parse("2026-01-01T00:00:00Z").toEpochMilliseconds()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(
        gameRepository: FakeGameRepository = FakeGameRepository(),
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        tabCoordinator: TabCoordinator = TabCoordinator(),
        analytics: FakeAnalyticsTracker = FakeAnalyticsTracker(),
        crashReporter: FakeCrashReporter = FakeCrashReporter(),
        locationProvider: FakeLocationProvider = FakeLocationProvider(),
        reverseGeocoder: FakeReverseGeocoder = FakeReverseGeocoder(),
        editingMatchId: String? = null,
    ) = CreateMatchStepModel(
        createMatch = CreateMatchUseCaseImpl(gameRepository),
        updateMatch = UpdateMatchUseCaseImpl(gameRepository),
        getGameById = GetGameByIdUseCaseImpl(gameRepository),
        stringsHolder = stringsHolder,
        sessionHolder = sessionHolder,
        observeAvailability = ObserveAvailabilityUseCaseImpl(FakeAvailabilityRepository()),
        tabCoordinator = tabCoordinator,
        analytics = analytics,
        crashReporter = crashReporter,
        locationProvider = locationProvider,
        reverseGeocoder = reverseGeocoder,
        editingMatchId = editingMatchId,
    )

    private fun fillValidForm(model: CreateMatchStepModel) {
        model.onEvent(CreateMatchEvents.VenueNameChanged("Quadra Nova"))
        model.onEvent(CreateMatchEvents.SportSelected(Sport.FUTSAL))
        model.onEvent(CreateMatchEvents.DateSelected(dateMillis))
        model.onEvent(CreateMatchEvents.TimeSelected(10, 30))
    }

    @Test
    fun `with permission granted, resolves the current location and its address`() =
        runTest(testDispatcher) {
            val locationProvider = FakeLocationProvider()
            val reverseGeocoder = FakeReverseGeocoder()
            val model = buildModel(locationProvider = locationProvider, reverseGeocoder = reverseGeocoder)

            advanceUntilIdle()

            val state = model.state.value
            assertEquals(-23.55, state.lat)
            assertEquals(-46.63, state.lng)
            assertEquals("Rua Um, 100", state.address)
            assertFalse(state.isResolvingLocation)
        }

    @Test
    fun `without location permission, falls back to the default center`() =
        runTest(testDispatcher) {
            val model = buildModel(locationProvider = FakeLocationProvider(permissionGranted = false))

            advanceUntilIdle()

            val state = model.state.value
            assertEquals(DefaultCenter.lat, state.lat)
            assertEquals(DefaultCenter.lng, state.lng)
            assertFalse(state.isResolvingLocation)
        }

    @Test
    fun `editing an existing match loads its data into state`() =
        runTest(testDispatcher) {
            val editedGame =
                game(id = "match-1").copy(
                    venueName = "Quadra do Bairro",
                    sport = Sport.VOLEI,
                    durationMin = 120,
                    totalPlayers = 12,
                    priceCents = 1550,
                    recurrence = RecurrenceOption.WEEKLY,
                )
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(editedGame))
            val model = buildModel(gameRepository = gameRepository, editingMatchId = "match-1")

            advanceUntilIdle()

            val state = model.state.value
            assertEquals("Quadra do Bairro", state.venueName)
            assertEquals(Sport.VOLEI, state.selectedSport)
            assertEquals(120, state.durationMin)
            assertEquals(12, state.totalPlayers)
            assertEquals("15.50", state.pricePerPlayer)
            assertEquals(RecurrenceOption.WEEKLY, state.recurrence)
            assertTrue(state.isEditMode)
            assertFalse(state.isLoading)
            assertEquals(listOf("match-1"), gameRepository.getGameByIdCalls)
        }

    @Test
    fun `a failed load in edit mode surfaces a generic error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository, editingMatchId = "match-1")

            model.effects.test {
                advanceUntilIdle()

                val effect = assertIs<CreateMatchEffect.ShowMessage>(awaitItem())
                assertEquals(stringsHolder.strings.createMatch.genericError, effect.message)
                cancelAndIgnoreRemainingEvents()
            }
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `submitting an invalid form does nothing`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(CreateMatchEvents.Submit)
            advanceUntilIdle()

            assertTrue(gameRepository.createMatchCalls.isEmpty())
        }

    @Test
    fun `submitting a valid form while logged out requires login`() =
        runTest(testDispatcher) {
            val model = buildModel(sessionHolder = FakeSessionHolder(session = null))
            advanceUntilIdle()
            fillValidForm(model)

            model.effects.test {
                model.onEvent(CreateMatchEvents.Submit)
                advanceUntilIdle()

                assertIs<CreateMatchEffect.RequireLogin>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `submitting a valid form creates the match, tracks analytics and switches tabs`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(createMatchResult = Result.success("new-match"))
            val analytics = FakeAnalyticsTracker()
            val tabCoordinator = TabCoordinator()
            val model = buildModel(gameRepository = gameRepository, analytics = analytics, tabCoordinator = tabCoordinator)
            advanceUntilIdle()
            fillValidForm(model)

            model.effects.test {
                model.onEvent(CreateMatchEvents.Submit)
                advanceUntilIdle()

                val effect = assertIs<CreateMatchEffect.NavigateToMyMatches>(awaitItem())
                assertEquals("new-match", effect.matchId)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, gameRepository.createMatchCalls.size)
            assertEquals("Quadra Nova", gameRepository.createMatchCalls.single().venueName)
            assertTrue(analytics.trackedEvents.any { it is AnalyticsEvent.MatchCreated })
            assertFalse(model.state.value.isSubmitting)

            tabCoordinator.tabs.test {
                assertEquals(MainTab.MyMatches, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a failed match creation surfaces a generic error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(createMatchResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()
            fillValidForm(model)

            model.effects.test {
                model.onEvent(CreateMatchEvents.Submit)
                advanceUntilIdle()

                val effect = assertIs<CreateMatchEffect.ShowMessage>(awaitItem())
                assertEquals(stringsHolder.strings.createMatch.genericError, effect.message)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `submitting a valid form in edit mode updates the match`() =
        runTest(testDispatcher) {
            val gameRepository =
                FakeGameRepository(
                    getGameByIdResult = Result.success(game(id = "match-1")),
                    updateMatchResult = Result.success(Unit),
                )
            val model = buildModel(gameRepository = gameRepository, editingMatchId = "match-1")
            advanceUntilIdle()
            fillValidForm(model)

            model.effects.test {
                model.onEvent(CreateMatchEvents.Submit)
                advanceUntilIdle()

                assertIs<CreateMatchEffect.MatchUpdated>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, gameRepository.updateMatchCalls.size)
            assertEquals("match-1", gameRepository.updateMatchCalls.single().first)
        }
}
