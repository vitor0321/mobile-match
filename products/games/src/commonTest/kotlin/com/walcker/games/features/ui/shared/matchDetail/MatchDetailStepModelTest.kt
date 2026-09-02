package com.walcker.games.features.ui.shared.matchDetail

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakeRatingRepository
import com.walcker.games.fake.FakeReportRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.fake.testUserSession
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.model.RatingDimensions
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.JoinGameUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.LeaveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SubmitRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitReportUseCaseImpl
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.navigator.PromotionCoordinator
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
class MatchDetailStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = GamesStringsHolder().apply { setStrings(PtBrGamesStrings) }
    private val fixedNow = 10_000L

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
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        promotionCoordinator: PromotionCoordinator = PromotionCoordinator(),
        analytics: FakeAnalyticsTracker = FakeAnalyticsTracker(),
        matchId: String = "match-1",
        nowSeconds: () -> Long = { fixedNow },
    ) = MatchDetailStepModel(
        getGameById = GetGameByIdUseCaseImpl(gameRepository),
        observeMatch = ObserveMatchUseCaseImpl(gameRepository),
        observeParticipants = ObserveParticipantsUseCaseImpl(gameRepository),
        joinGame = JoinGameUseCaseImpl(gameRepository),
        leaveMatch = LeaveMatchUseCaseImpl(gameRepository),
        cancelMatch = CancelMatchUseCaseImpl(gameRepository),
        submitRating = SubmitRatingUseCase(ratingRepository),
        submitReport = SubmitReportUseCaseImpl(reportRepository),
        sessionHolder = sessionHolder,
        promotionCoordinator = promotionCoordinator,
        stringsHolder = stringsHolder,
        analytics = analytics,
        matchId = matchId,
        nowSeconds = nowSeconds,
    )

    @Test
    fun `loading the match populates state and tracks a view once`() =
        runTest(testDispatcher) {
            val myGame = game(id = "match-1", startsAtSeconds = 20_000)
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val analytics = FakeAnalyticsTracker()
            val model = buildModel(gameRepository = gameRepository, analytics = analytics)

            advanceUntilIdle()

            assertEquals(myGame, model.state.value.match)
            assertFalse(model.state.value.isLoading)
            assertEquals(1, analytics.trackedEvents.count { it is AnalyticsEvent.MatchViewed })
        }

    @Test
    fun `a load failure surfaces the generic load error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            assertEquals(stringsHolder.strings.matchDetail.loadError, model.state.value.errorMessage)
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `retry reloads the match`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()
            val callsBefore = gameRepository.getGameByIdCalls.size

            model.onEvent(MatchDetailEvent.Retry)
            advanceUntilIdle()

            assertEquals(callsBefore + 1, gameRepository.getGameByIdCalls.size)
        }

    @Test
    fun `the match stream updates state as new snapshots arrive`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            val updated = game(id = "match-1").copy(venueName = "Quadra Atualizada")
            gameRepository.emitMatch(Result.success(updated))
            advanceUntilIdle()

            assertEquals(
                "Quadra Atualizada",
                model.state.value.match
                    ?.venueName,
            )
        }

    @Test
    fun `a status change from open to full surfaces a message that can be dismissed`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            gameRepository.emitMatch(Result.success(game(id = "match-1", status = MatchStatus.OPEN)))
            advanceUntilIdle()
            assertNull(model.state.value.statusChangeMessage)

            gameRepository.emitMatch(Result.success(game(id = "match-1", status = MatchStatus.FULL)))
            advanceUntilIdle()
            assertEquals(stringsHolder.strings.matchDetail.statusChangedToFull, model.state.value.statusChangeMessage)

            model.onEvent(MatchDetailEvent.DismissStatusChange)
            assertNull(model.state.value.statusChangeMessage)
        }

    @Test
    fun `promotion from waitlist to confirmed notifies the coordinator`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val promotionCoordinator = PromotionCoordinator()
            val sessionHolder = FakeSessionHolder(session = testUserSession(uid = "user-1"))
            val model =
                buildModel(gameRepository = gameRepository, promotionCoordinator = promotionCoordinator, sessionHolder = sessionHolder)
            advanceUntilIdle()

            val waitlisted =
                Participant(userId = "user-1", displayName = "Ana", photoUrl = null, joinedAt = 0, isConfirmed = false, positionInWaitlist = 1)
            gameRepository.emitParticipants(
                Result.success(ParticipantsSummary(confirmed = emptyList(), waitlist = listOf(waitlisted), confirmedCount = 0, totalSlots = 10)),
            )
            advanceUntilIdle()
            assertFalse(model.state.value.justPromoted)

            val promoted = Participant(userId = "user-1", displayName = "Ana", photoUrl = null, joinedAt = 0, isConfirmed = true)

            promotionCoordinator.promotions.test {
                gameRepository.emitParticipants(
                    Result.success(ParticipantsSummary(confirmed = listOf(promoted), waitlist = emptyList(), confirmedCount = 1, totalSlots = 10)),
                )
                advanceUntilIdle()

                assertEquals("match-1", awaitItem().matchId)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(model.state.value.justPromoted)

            model.onEvent(MatchDetailEvent.DismissPromotion)
            assertFalse(model.state.value.justPromoted)
        }

    @Test
    fun `joining while logged out requires login`() =
        runTest(testDispatcher) {
            val model = buildModel(sessionHolder = FakeSessionHolder(session = null))
            advanceUntilIdle()

            model.effects.test {
                model.onEvent(MatchDetailEvent.JoinMatch)
                advanceUntilIdle()

                assertIs<MatchDetailEffect.RequireLogin>(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a confirmed join navigates to the confirmation screen and tracks analytics`() =
        runTest(testDispatcher) {
            val myGame = game(id = "match-1", startsAtSeconds = 50_000).copy(venueName = "Quadra Confirmada", sport = Sport.FUTSAL)
            val gameRepository =
                FakeGameRepository(
                    getGameByIdResult = Result.success(myGame),
                    joinGameResult = Result.success(JoinMatchOutcome.Confirmed(matchId = "match-1")),
                )
            val analytics = FakeAnalyticsTracker()
            val model = buildModel(gameRepository = gameRepository, analytics = analytics)
            advanceUntilIdle()

            model.effects.test {
                model.onEvent(MatchDetailEvent.JoinMatch)
                advanceUntilIdle()

                val effect = assertIs<MatchDetailEffect.NavigateToConfirmation>(awaitItem())
                assertEquals("match-1", effect.matchId)
                assertEquals("Quadra Confirmada", effect.venueName)
                assertEquals(50_000L, effect.startsAtSeconds)
                assertEquals(Sport.FUTSAL.label, effect.sportLabel)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(analytics.trackedEvents.any { it is AnalyticsEvent.MatchJoinAttempted })
            assertTrue(analytics.trackedEvents.any { it is AnalyticsEvent.MatchJoinResult })
            assertFalse(model.state.value.isJoining)
        }

    @Test
    fun `joining onto the waitlist sets a success message without navigating`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(joinGameResult = Result.success(JoinMatchOutcome.Waitlist(matchId = "match-1", position = 3)))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.JoinMatch)
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.matchDetail.joinWaitlistSuccess(3), model.state.value.successMessage)
        }

    @Test
    fun `a failed join surfaces the join error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(joinGameResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.JoinMatch)
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.matchDetail.joinError, model.state.value.errorMessage)
            assertFalse(model.state.value.isJoining)
        }

    @Test
    fun `leaving the match shows a confirmation dialog before acting`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(leaveMatchResult = Result.success(LeaveMatchOutcome(matchId = "match-1")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.RequestLeaveMatch)
            assertTrue(model.state.value.showLeaveConfirmDialog)

            model.onEvent(MatchDetailEvent.ConfirmLeaveMatch)
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(stringsHolder.strings.matchDetail.leaveSuccess, state.successMessage)
            assertFalse(state.showLeaveConfirmDialog)
            assertFalse(state.isLeavingMatch)
        }

    @Test
    fun `cancelling the leave dialog just closes it`() =
        runTest(testDispatcher) {
            val model = buildModel()
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.RequestLeaveMatch)
            model.onEvent(MatchDetailEvent.CancelLeaveMatch)

            assertFalse(model.state.value.showLeaveConfirmDialog)
        }

    @Test
    fun `cancelling the match surfaces a success message`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(cancelMatchResult = Result.success(CancelMatchOutcome.Cancelled(matchId = "match-1")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.RequestCancelMatch)
            model.onEvent(MatchDetailEvent.ConfirmCancelMatch)
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(stringsHolder.strings.matchDetail.cancelSuccess, state.successMessage)
            assertFalse(state.isCancellingMatch)
            assertFalse(state.showCancelConfirmDialog)
        }

    @Test
    fun `a failed cancel surfaces the cancel error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(cancelMatchResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.RequestCancelMatch)
            model.onEvent(MatchDetailEvent.ConfirmCancelMatch)
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.matchDetail.cancelError, model.state.value.errorMessage)
        }

    @Test
    fun `submitting a rating succeeds and closes the sheet`() =
        runTest(testDispatcher) {
            val ratingRepository = FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 3)))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(MatchDetailEvent.SubmitRating(rating = 5, comment = "Bom jogo", dimensions = RatingDimensions.None))
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.showRatingSheet)
            assertEquals(stringsHolder.strings.ratings.submitSuccess, state.successMessage)
            assertEquals(listOf("player-2"), ratingRepository.submitCalls)
        }

    @Test
    fun `rating a player already rated in this match still surfaces its own message`() =
        runTest(testDispatcher) {
            val ratingRepository = FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.AlreadyRated(averageRating = 4f, ratingCount = 2)))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(MatchDetailEvent.SubmitRating(rating = 5, comment = "", dimensions = RatingDimensions.None))
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.ratings.alreadyRated, model.state.value.successMessage)
        }

    @Test
    fun `a failed rating submission surfaces an error and keeps the sheet open`() =
        runTest(testDispatcher) {
            val ratingRepository = FakeRatingRepository(submitResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(MatchDetailEvent.SubmitRating(rating = 5, comment = "", dimensions = RatingDimensions.None))
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(stringsHolder.strings.ratings.submitError, state.ratingErrorMessage)
            assertFalse(state.isSubmittingRating)
            assertTrue(state.showRatingSheet)

            model.onEvent(MatchDetailEvent.DismissRatingError)
            assertNull(model.state.value.ratingErrorMessage)
        }

    @Test
    fun `submitting a report succeeds and closes the sheet`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository(submitResult = Result.success(SubmitReportOutcome.Recorded))
            val model = buildModel(reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenReportSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(MatchDetailEvent.SubmitReport(reason = ReportReason.NO_SHOW, details = "Não apareceu"))
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.showReportSheet)
            assertEquals(stringsHolder.strings.reports.success, state.successMessage)
            assertEquals(listOf("player-2"), reportRepository.submitCalls)
        }

    @Test
    fun `reporting a player already reported in this match surfaces its own message`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository(submitResult = Result.success(SubmitReportOutcome.AlreadyReported))
            val model = buildModel(reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenReportSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(MatchDetailEvent.SubmitReport(reason = ReportReason.OTHER, details = ""))
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.reports.alreadyReported, model.state.value.successMessage)
        }

    @Test
    fun `a failed report submission surfaces an error`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository(submitResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenReportSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(MatchDetailEvent.SubmitReport(reason = ReportReason.OTHER, details = ""))
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.reports.error, model.state.value.reportErrorMessage)
        }

    @Test
    fun `canRate reflects that the match is over and the user took part in it`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", startsAtSeconds = 1_000, durationMin = 1, status = MatchStatus.OPEN)
                    .copy(participants = listOf("user-1"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository, nowSeconds = { 5_000L })

            advanceUntilIdle()

            val state = model.state.value
            assertTrue(state.isMatchOver)
            assertTrue(state.canRate)
        }

    @Test
    fun `canRate is false when the match has not started yet`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", startsAtSeconds = 100_000, durationMin = 60, status = MatchStatus.OPEN)
                    .copy(participants = listOf("user-1"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository, nowSeconds = { 5_000L })

            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.isMatchOver)
            assertFalse(state.canRate)
        }
}
