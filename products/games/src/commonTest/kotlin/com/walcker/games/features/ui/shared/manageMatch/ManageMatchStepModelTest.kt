package com.walcker.games.features.ui.shared.manageMatch

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCaseImpl
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ManageMatchStepModelTest {
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
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        analytics: FakeAnalyticsTracker = FakeAnalyticsTracker(),
        crashReporter: FakeCrashReporter = FakeCrashReporter(),
        matchId: String = "match-1",
    ) = ManageMatchStepModel(
        matchId = matchId,
        getGameById = GetGameByIdUseCaseImpl(gameRepository),
        observeMatch = ObserveMatchUseCaseImpl(gameRepository),
        cancelMatch = CancelMatchUseCaseImpl(gameRepository),
        cancelMatchSeries = CancelMatchSeriesUseCaseImpl(gameRepository),
        sessionHolder = sessionHolder,
        stringsHolder = stringsHolder,
        analytics = analytics,
        crashReporter = crashReporter,
    )

    @Test
    fun `a load failure surfaces the not found error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            assertEquals(stringsHolder.strings.manageMatch.notFound, model.state.value.errorMessage)
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `cancelling the match emits the cancelled effect`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(cancelMatchResult = Result.success(CancelMatchOutcome.Cancelled(matchId = "match-1")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.effects.test {
                model.onEvent(ManageMatchEvent.RequestCancelMatch)
                assertTrue(model.state.value.showCancelConfirmDialog)

                model.onEvent(ManageMatchEvent.ConfirmCancelMatch)
                advanceUntilIdle()

                assertIs<ManageMatchEffect.MatchCancelled>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertFalse(model.state.value.isCancellingMatch)
            assertFalse(model.state.value.showCancelConfirmDialog)
        }

    @Test
    fun `a failed cancel surfaces the cancel error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(cancelMatchResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.RequestCancelMatch)
            model.onEvent(ManageMatchEvent.ConfirmCancelMatch)
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.manageMatch.cancelError, model.state.value.actionErrorMessage)
            assertFalse(model.state.value.isCancellingMatch)
        }

    @Test
    fun `cancelling the cancel dialog just closes it`() =
        runTest(testDispatcher) {
            val model = buildModel()
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.RequestCancelMatch)
            model.onEvent(ManageMatchEvent.CancelCancelMatch)

            assertFalse(model.state.value.showCancelConfirmDialog)
        }

    @Test
    fun `cancelling the series surfaces a success message without cancelling the current match`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(cancelMatchSeriesResult = Result.success(Unit))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.RequestCancelSeries)
            assertTrue(model.state.value.showCancelSeriesConfirmDialog)

            model.onEvent(ManageMatchEvent.ConfirmCancelSeries)
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(stringsHolder.strings.manageMatch.cancelSeriesSuccess, state.successMessage)
            assertFalse(state.isCancellingSeries)
            assertFalse(state.showCancelSeriesConfirmDialog)
            assertEquals(emptyList<String>(), gameRepository.cancelMatchCalls)
        }

    @Test
    fun `a failed cancel series surfaces the cancel series error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(cancelMatchSeriesResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.RequestCancelSeries)
            model.onEvent(ManageMatchEvent.ConfirmCancelSeries)
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.manageMatch.cancelSeriesError, model.state.value.actionErrorMessage)
            assertFalse(model.state.value.isCancellingSeries)
        }

    @Test
    fun `cancelling the cancel series dialog just closes it`() =
        runTest(testDispatcher) {
            val model = buildModel()
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.RequestCancelSeries)
            model.onEvent(ManageMatchEvent.CancelCancelSeries)

            assertFalse(model.state.value.showCancelSeriesConfirmDialog)
        }
}
