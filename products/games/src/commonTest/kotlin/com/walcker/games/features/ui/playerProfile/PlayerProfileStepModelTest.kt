package com.walcker.games.features.ui.playerProfile

import app.cash.turbine.test
import com.walcker.games.fake.FakeAvailabilityRepository
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeLogoutService
import com.walcker.games.fake.FakeRatingRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.fake.rating
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCaseImpl
import com.walcker.games.features.domain.playerProfile.usecase.SetAvailabilityUseCaseImpl
import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.repository.MyMatch
import com.walcker.games.features.domain.shared.usecase.GetMyMatchesUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetUserRatingsUseCase
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
class PlayerProfileStepModelTest {
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
        gameRepository: FakeGameRepository = FakeGameRepository(),
        ratingRepository: FakeRatingRepository = FakeRatingRepository(),
        availabilityRepository: FakeAvailabilityRepository = FakeAvailabilityRepository(),
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        logoutService: FakeLogoutService = FakeLogoutService(),
    ) = PlayerProfileStepModel(
        sessionHolder = sessionHolder,
        getMyMatches = GetMyMatchesUseCaseImpl(gameRepository),
        getUserRatings = GetUserRatingsUseCase(ratingRepository),
        stringsHolder = stringsHolder,
        logoutService = logoutService,
        observeAvailability = ObserveAvailabilityUseCaseImpl(availabilityRepository),
        setAvailability = SetAvailabilityUseCaseImpl(availabilityRepository),
    )

    @Test
    fun `an anonymous session clears the profile`() =
        runTest(testDispatcher) {
            val model = buildModel(sessionHolder = FakeSessionHolder(session = null))

            advanceUntilIdle()

            val state = model.state.value
            assertNull(state.userName)
            assertFalse(state.isLoading)
        }

    @Test
    fun `loads name, email and computes organizer versus participant counts`() =
        runTest(testDispatcher) {
            val organized = game(id = "match-1").let { it.copy(organizerId = "user-1") }
            val participated = game(id = "match-2").let { it.copy(organizerId = "someone-else") }
            val gameRepository =
                FakeGameRepository(
                    myMatches =
                        Result.success(
                            listOf(
                                MyMatch(game = organized, role = MatchRole.ORGANIZER),
                                MyMatch(game = participated, role = MatchRole.PARTICIPANT),
                            ),
                        ),
                )
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            val state = model.state.value
            assertEquals("Ana Souza", state.userName)
            assertEquals("ana@example.com", state.userEmail)
            assertEquals(1, state.matchesOrganized)
            assertEquals(1, state.matchesParticipated)
        }

    @Test
    fun `computes the average from the loaded ratings`() =
        runTest(testDispatcher) {
            val ratingRepository =
                FakeRatingRepository(
                    userRatingsResult = Result.success(listOf(rating(id = "1", stars = 5), rating(id = "2", stars = 3))),
                )
            val model = buildModel(ratingRepository = ratingRepository)

            advanceUntilIdle()

            val state = model.state.value
            assertEquals(4f, state.averageRating)
            assertEquals(2, state.totalRatings)
        }

    @Test
    fun `observes availability and reflects it in state`() =
        runTest(testDispatcher) {
            val availabilityRepository = FakeAvailabilityRepository()
            val model = buildModel(availabilityRepository = availabilityRepository)
            advanceUntilIdle()

            availabilityRepository.emit(Result.success(Availability(isAvailable = true)))
            advanceUntilIdle()

            assertTrue(model.state.value.isAvailable)
        }

    @Test
    fun `changing availability while logged out requires login`() =
        runTest(testDispatcher) {
            val model = buildModel(sessionHolder = FakeSessionHolder(session = null))
            advanceUntilIdle()

            model.effects.test {
                model.onEvent(PlayerProfileEvent.AvailabilityChanged(true))
                advanceUntilIdle()

                assertIs<PlayerProfileEffect.RequireLogin>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a failed availability change reverts the optimistic update`() =
        runTest(testDispatcher) {
            val availabilityRepository = FakeAvailabilityRepository(setAvailableResult = Result.failure(IllegalStateException("boom")))
            val model = buildModel(availabilityRepository = availabilityRepository)
            advanceUntilIdle()

            model.onEvent(PlayerProfileEvent.AvailabilityChanged(true))
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.isAvailable)
            assertFalse(state.isUpdatingAvailability)
            assertEquals(stringsHolder.strings.playerProfile.availabilityError, state.availabilityErrorMessage)
        }

    @Test
    fun `logging out calls the logout service`() =
        runTest(testDispatcher) {
            val logoutService = FakeLogoutService()
            val model = buildModel(logoutService = logoutService)
            advanceUntilIdle()

            model.onEvent(PlayerProfileEvent.LogoutRequested)
            advanceUntilIdle()

            assertEquals(1, logoutService.logoutCallCount)
        }

    @Test
    fun `a failed logout surfaces an error`() =
        runTest(testDispatcher) {
            val logoutService = FakeLogoutService(result = Result.failure(IllegalStateException("offline")))
            val model = buildModel(logoutService = logoutService)
            advanceUntilIdle()

            model.onEvent(PlayerProfileEvent.LogoutRequested)
            advanceUntilIdle()

            assertEquals("offline", model.state.value.errorMessage)
        }

    @Test
    fun `dismissing the availability error only clears that message`() =
        runTest(testDispatcher) {
            val availabilityRepository = FakeAvailabilityRepository(setAvailableResult = Result.failure(IllegalStateException("boom")))
            val model = buildModel(availabilityRepository = availabilityRepository)
            advanceUntilIdle()
            model.onEvent(PlayerProfileEvent.AvailabilityChanged(true))
            advanceUntilIdle()

            model.onEvent(PlayerProfileEvent.DismissAvailabilityError)

            assertNull(model.state.value.availabilityErrorMessage)
        }
}
