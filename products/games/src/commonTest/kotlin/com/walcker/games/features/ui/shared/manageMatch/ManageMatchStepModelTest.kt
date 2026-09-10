package com.walcker.games.features.ui.shared.manageMatch

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakePlayerRepository
import com.walcker.games.fake.FakeRatingRepository
import com.walcker.games.fake.FakeReportRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SetTeamAssignmentsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitReportUseCaseImpl
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
        ratingRepository: FakeRatingRepository = FakeRatingRepository(),
        reportRepository: FakeReportRepository = FakeReportRepository(),
        playerRepository: FakePlayerRepository = FakePlayerRepository(),
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        analytics: FakeAnalyticsTracker = FakeAnalyticsTracker(),
        crashReporter: FakeCrashReporter = FakeCrashReporter(),
        matchId: String = "match-1",
    ) = ManageMatchStepModel(
        matchId = matchId,
        getGameById = GetGameByIdUseCaseImpl(gameRepository),
        observeMatch = ObserveMatchUseCaseImpl(gameRepository),
        observeParticipants = ObserveParticipantsUseCaseImpl(gameRepository),
        cancelMatch = CancelMatchUseCaseImpl(gameRepository),
        cancelMatchSeries = CancelMatchSeriesUseCaseImpl(gameRepository),
        setTeamAssignments = SetTeamAssignmentsUseCaseImpl(gameRepository),
        submitRating = SubmitRatingUseCase(ratingRepository),
        submitReport = SubmitReportUseCaseImpl(reportRepository),
        playerRepository = playerRepository,
        ratingRepository = ratingRepository,
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
    fun `canRatePlayers is true for the organizer, independent of match timing`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", startsAtSeconds = 100_000, durationMin = 60, status = MatchStatus.OPEN, organizerId = "user-1")
                    .copy(participants = listOf("player-2"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            assertTrue(model.state.value.canRatePlayers)
        }

    @Test
    fun `canRatePlayers is false for a participant who is not the organizer`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", organizerId = "someone-else")
                    .copy(participants = listOf("user-1"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            assertFalse(model.state.value.canRatePlayers)
        }

    @Test
    fun `loading the match fetches ratings the organizer already gave, keyed by rated player`() =
        runTest(testDispatcher) {
            val myGame = game(id = "match-1", organizerId = "user-1").copy(participants = listOf("player-2"))
            val existing =
                Rating(
                    id = "user-1_player-2",
                    matchId = "match-1",
                    ratedUserId = "player-2",
                    raterUserId = "user-1",
                    rating = 3,
                    comment = "",
                    createdAtMs = 1_000L,
                )
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val ratingRepository = FakeRatingRepository(ratingsGivenForMatchResult = Result.success(listOf(existing)))
            val model = buildModel(gameRepository = gameRepository, ratingRepository = ratingRepository)

            advanceUntilIdle()

            assertEquals(mapOf("player-2" to existing), model.state.value.organizerRatingsGiven)
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
    fun `shuffling teams writes a balanced assignment for the selected team count`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", organizerId = "player-1")
                    .copy(participants = listOf("p1", "p2", "p3", "p4"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()
            gameRepository.emitParticipants(
                Result.success(
                    fakeParticipantsSummaryFor(listOf("p1", "p2", "p3", "p4")),
                ),
            )
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.TeamCountSelected(2))
            model.onEvent(ManageMatchEvent.ShuffleTeams)
            advanceUntilIdle()

            val call = gameRepository.setTeamAssignmentsCalls.single()
            assertEquals("match-1", call.first)
            assertEquals(2, call.second)
            assertEquals(setOf("p1", "p2", "p3", "p4"), call.third.keys)
            assertTrue(call.third.values.all { it in 0..1 })
        }

    @Test
    fun `moving a player to another team writes only that player's new assignment`() =
        runTest(testDispatcher) {
            val myGame =
                game(
                    id = "match-1",
                    organizerId = "player-1",
                    teamCount = 2,
                    teamAssignments = mapOf("p1" to 0, "p2" to 1),
                ).copy(participants = listOf("p1", "p2"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.MovePlayerToTeam(userId = "p1", teamIndex = 1))
            advanceUntilIdle()

            val call = gameRepository.setTeamAssignmentsCalls.single()
            assertEquals(2, call.second)
            assertEquals(mapOf("p1" to 1, "p2" to 1), call.third)
        }

    @Test
    fun `selecting a team count only updates local state, no write happens yet`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.TeamCountSelected(4))

            assertEquals(4, model.state.value.selectedTeamCount)
            assertTrue(gameRepository.setTeamAssignmentsCalls.isEmpty())
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

    @Test
    fun `submitting a rating succeeds and closes the sheet`() =
        runTest(testDispatcher) {
            val ratingRepository =
                FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 3)))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(rating = 5, comment = "Bom jogo", reportReason = null, reportDetails = ""),
            )
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.showRatingSheet)
            assertEquals(stringsHolder.strings.ratings.submitSuccess, state.ratingSuccessMessage)
            assertEquals(listOf("player-2"), ratingRepository.submitCalls)
        }

    @Test
    fun `submitting a rating updates the participant's visible rating summary`() =
        runTest(testDispatcher) {
            val ratingRepository =
                FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 3)))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
            )
            advanceUntilIdle()

            assertEquals(
                PlayerRatingSummary(rating = 4.5f, ratingCount = 3),
                model.state.value.participantRatings["player-2"],
            )
        }

    @Test
    fun `editing a rating surfaces its own message`() =
        runTest(testDispatcher) {
            val ratingRepository =
                FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Updated(averageRating = 4f, ratingCount = 2)))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
            )
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.ratings.updated, model.state.value.ratingSuccessMessage)
        }

    @Test
    fun `a failed rating submission surfaces an error and closes the sheet`() =
        runTest(testDispatcher) {
            val ratingRepository = FakeRatingRepository(submitResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
            )
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(stringsHolder.strings.ratings.submitError, state.ratingErrorMessage)
            assertFalse(state.isSubmittingRating)
            assertFalse(state.showRatingSheet)
            assertNull(state.selectedPlayerForRating)

            model.onEvent(ManageMatchEvent.DismissRatingError)
            assertNull(model.state.value.ratingErrorMessage)
        }

    @Test
    fun `submitting a rating with a report reason also files the report`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository(submitResult = Result.success(SubmitReportOutcome.Recorded))
            val ratingRepository =
                FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 3)))
            val model = buildModel(ratingRepository = ratingRepository, reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(
                    rating = 5,
                    comment = "",
                    reportReason = ReportReason.NO_SHOW,
                    reportDetails = "Não apareceu",
                ),
            )
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.showRatingSheet)
            assertEquals(listOf("player-2"), reportRepository.submitCalls)
            assertEquals(
                "${stringsHolder.strings.ratings.submitSuccess} ${stringsHolder.strings.reports.success}",
                state.ratingSuccessMessage,
            )
        }

    @Test
    fun `submitting a rating without a report reason never calls the report repository`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository()
            val model = buildModel(reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
            )
            advanceUntilIdle()

            assertEquals(emptyList<String>(), reportRepository.submitCalls)
        }

    @Test
    fun `a report reason already reported still shows the rating's success message`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository(submitResult = Result.success(SubmitReportOutcome.AlreadyReported))
            val model = buildModel(reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(
                    rating = 5,
                    comment = "",
                    reportReason = ReportReason.OTHER,
                    reportDetails = "",
                ),
            )
            advanceUntilIdle()

            assertEquals(
                "${stringsHolder.strings.ratings.submitSuccess} ${stringsHolder.strings.reports.alreadyReported}",
                model.state.value.ratingSuccessMessage,
            )
        }

    @Test
    fun `a failed report submission does not hide the rating's success message`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository(submitResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                ManageMatchEvent.SubmitRating(
                    rating = 5,
                    comment = "",
                    reportReason = ReportReason.OTHER,
                    reportDetails = "",
                ),
            )
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.showRatingSheet)
            assertEquals(stringsHolder.strings.ratings.submitSuccess, state.ratingSuccessMessage)
        }

    @Test
    fun `closing the rating sheet clears the selected player`() =
        runTest(testDispatcher) {
            val model = buildModel()
            advanceUntilIdle()

            model.onEvent(ManageMatchEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            assertTrue(model.state.value.showRatingSheet)

            model.onEvent(ManageMatchEvent.CloseRatingSheet)

            val state = model.state.value
            assertFalse(state.showRatingSheet)
            assertNull(state.selectedPlayerForRating)
        }
}

private fun fakeParticipantsSummaryFor(userIds: List<String>): ParticipantsSummary =
    ParticipantsSummary(
        confirmed =
            userIds.map { userId ->
                Participant(userId = userId, displayName = userId, photoUrl = null, joinedAt = 0, isConfirmed = true)
            },
        waitlist = emptyList(),
        confirmedCount = userIds.size,
        totalSlots = 10,
    )
