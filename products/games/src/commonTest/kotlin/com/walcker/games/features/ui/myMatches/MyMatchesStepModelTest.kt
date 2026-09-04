package com.walcker.games.features.ui.myMatches

import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.repository.MyMatch
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetMyMatchesUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.LeaveMatchUseCaseImpl
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.games.strings.resolveStringsOrDefault
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyMatchesStepModelTest {
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
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
    ) = MyMatchesStepModel(
        getMyMatches = GetMyMatchesUseCaseImpl(repository),
        cancelMatch = CancelMatchUseCaseImpl(repository),
        leaveMatch = LeaveMatchUseCaseImpl(repository),
        stringsHolder = stringsHolder,
        sessionHolder = sessionHolder,
        analytics = FakeAnalyticsTracker(),
        crashReporter = FakeCrashReporter(),
    )

    @Test
    fun `an anonymous user sees no matches and does not query the repository`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository, FakeSessionHolder(session = null))

            advanceUntilIdle()

            val state = model.state.value
            assertTrue(state.active.isEmpty())
            assertTrue(state.past.isEmpty())
            assertTrue(!state.isLoading)
        }

    @Test
    fun `future matches are active, past ones are not`() =
        runTest(testDispatcher) {
            val future = game(id = "match-future", startsAtSeconds = Long.MAX_VALUE / 1000)
            val past = game(id = "match-past", startsAtSeconds = 0L)
            val repository =
                FakeGameRepository(
                    myMatches =
                        Result.success(
                            listOf(
                                MyMatch(game = future, role = MatchRole.ORGANIZER),
                                MyMatch(game = past, role = MatchRole.PARTICIPANT),
                            ),
                        ),
                )
            val model = buildModel(repository)

            advanceUntilIdle()

            val state = model.state.value
            assertEquals(listOf("match-future"), state.active.map { it.game.id })
            assertEquals(listOf("match-past"), state.past.map { it.game.id })
        }

    @Test
    fun `a load failure surfaces an error message`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository(myMatches = Result.failure(IllegalStateException("offline")))
            val model = buildModel(repository)

            advanceUntilIdle()

            assertEquals("offline", model.state.value.errorMessage)
        }

    @Test
    fun `switching tabs updates the state without a new load`() =
        runTest(testDispatcher) {
            val model = buildModel(FakeGameRepository())
            advanceUntilIdle()

            model.onEvent(MyMatchesEvent.TabSelected(MyMatchesTab.PAST))

            assertEquals(MyMatchesTab.PAST, model.state.value.activeTab)
        }

    @Test
    fun `cancelling a match refreshes the list on success`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(MyMatchesEvent.CancelRequested("match-1"))
            advanceUntilIdle()

            assertEquals(listOf("match-1"), repository.cancelMatchCalls)
        }

    @Test
    fun `a failed cancel surfaces the games error message when available`() =
        runTest(testDispatcher) {
            val repository =
                FakeGameRepository(
                    cancelMatchResult = Result.failure(IllegalStateException("boom")),
                )
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(MyMatchesEvent.CancelRequested("match-1"))
            advanceUntilIdle()

            assertEquals(stringsHolder.resolveStringsOrDefault().myMatches.cancelError, model.state.value.errorMessage)
        }

    @Test
    fun `leaving a match refreshes the list on success`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository()
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(MyMatchesEvent.LeaveRequested("match-1"))
            advanceUntilIdle()

            assertEquals(listOf("match-1"), repository.leaveMatchCalls)
        }

    @Test
    fun `dismissing the error only clears the message`() =
        runTest(testDispatcher) {
            val repository = FakeGameRepository(myMatches = Result.failure(IllegalStateException("offline")))
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(MyMatchesEvent.DismissError)

            assertNull(model.state.value.errorMessage)
        }
}
