package com.walcker.games.features.ui.shared.matchDetail

import app.cash.turbine.test
import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakePlayerRepository
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
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.JoinGameUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.LeaveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SubmitMatchRatingUseCase
import com.walcker.games.features.domain.shared.usecase.SubmitOrganizerRatingUseCase
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
import kotlin.test.assertNotNull
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
        playerRepository: FakePlayerRepository = FakePlayerRepository(),
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        promotionCoordinator: PromotionCoordinator = PromotionCoordinator(),
        analytics: FakeAnalyticsTracker = FakeAnalyticsTracker(),
        crashReporter: FakeCrashReporter = FakeCrashReporter(),
        matchId: String = "match-1",
        nowSeconds: () -> Long = { fixedNow },
    ) = MatchDetailStepModel(
        getGameById = GetGameByIdUseCaseImpl(gameRepository),
        observeMatch = ObserveMatchUseCaseImpl(gameRepository),
        observeParticipants = ObserveParticipantsUseCaseImpl(gameRepository),
        joinGame = JoinGameUseCaseImpl(gameRepository),
        leaveMatch = LeaveMatchUseCaseImpl(gameRepository),
        cancelMatch = CancelMatchUseCaseImpl(gameRepository),
        cancelMatchSeries = CancelMatchSeriesUseCaseImpl(gameRepository),
        submitRating = SubmitRatingUseCase(ratingRepository),
        submitMatchRating = SubmitMatchRatingUseCase(ratingRepository),
        submitOrganizerRating = SubmitOrganizerRatingUseCase(ratingRepository),
        submitReport = SubmitReportUseCaseImpl(reportRepository),
        playerRepository = playerRepository,
        ratingRepository = ratingRepository,
        sessionHolder = sessionHolder,
        promotionCoordinator = promotionCoordinator,
        stringsHolder = stringsHolder,
        analytics = analytics,
        crashReporter = crashReporter,
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
                assertEquals(myGame.durationMin, effect.durationMin)
                assertEquals(Sport.FUTSAL, effect.sport)
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

            assertEquals(stringsHolder.strings.matchDetail.joinError, model.state.value.actionErrorMessage)
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

            assertEquals(stringsHolder.strings.matchDetail.cancelError, model.state.value.actionErrorMessage)
        }

    @Test
    fun `a failed leave surfaces the leave error without hiding the match`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(leaveMatchResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.RequestLeaveMatch)
            model.onEvent(MatchDetailEvent.ConfirmLeaveMatch)
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(stringsHolder.strings.matchDetail.leaveError, state.actionErrorMessage)
            assertNull(state.errorMessage)
            assertNotNull(state.match)
        }

    @Test
    fun `submitting a rating succeeds and closes the sheet`() =
        runTest(testDispatcher) {
            val ratingRepository = FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 3)))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(rating = 5, comment = "Bom jogo", reportReason = null, reportDetails = ""),
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

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
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
            val ratingRepository = FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Updated(averageRating = 4f, ratingCount = 2)))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
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

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
            )
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(stringsHolder.strings.ratings.submitError, state.ratingErrorMessage)
            assertFalse(state.isSubmittingRating)
            assertFalse(state.showRatingSheet)
            assertNull(state.selectedPlayerForRating)

            model.onEvent(MatchDetailEvent.DismissRatingError)
            assertNull(model.state.value.ratingErrorMessage)
        }

    @Test
    fun `submitting an organizer rating succeeds, refreshes the summary and closes the sheet`() =
        runTest(testDispatcher) {
            val playerRepository =
                FakePlayerRepository(
                    organizerRatingSummaryResult =
                        Result.success(PlayerRatingSummary(rating = 4.5f, ratingCount = 3)),
                )
            val ratingRepository =
                FakeRatingRepository(
                    submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 3)),
                )
            val model = buildModel(ratingRepository = ratingRepository, playerRepository = playerRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenOrganizerRatingSheet)
            model.onEvent(MatchDetailEvent.SubmitOrganizerRating(rating = 5))
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.showOrganizerRatingSheet)
            assertEquals(PlayerRatingSummary(rating = 4.5f, ratingCount = 3), state.organizerRatingSummary)
            assertEquals(stringsHolder.strings.matchDetail.organizerRatingSubmitSuccess, state.successMessage)
        }

    @Test
    fun `a failed organizer rating submission surfaces an error and closes the sheet`() =
        runTest(testDispatcher) {
            val ratingRepository = FakeRatingRepository(submitResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenOrganizerRatingSheet)
            model.onEvent(MatchDetailEvent.SubmitOrganizerRating(rating = 5))
            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.showOrganizerRatingSheet)
            assertEquals(stringsHolder.strings.ratings.submitError, state.ratingErrorMessage)
            assertNull(state.errorMessage)
        }

    @Test
    fun `submitting a rating with a report reason also files the report`() =
        runTest(testDispatcher) {
            val reportRepository = FakeReportRepository(submitResult = Result.success(SubmitReportOutcome.Recorded))
            val ratingRepository =
                FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.5f, ratingCount = 3)))
            val model = buildModel(ratingRepository = ratingRepository, reportRepository = reportRepository)
            advanceUntilIdle()

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(
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

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""),
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

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(
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

            model.onEvent(MatchDetailEvent.OpenRatingSheet(userId = "player-2", displayName = "Bruno"))
            model.onEvent(
                MatchDetailEvent.SubmitRating(
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

    @Test
    fun `canRatePlayers is true for the organizer, independent of match timing`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", startsAtSeconds = 100_000, durationMin = 60, status = MatchStatus.OPEN, organizerId = "user-1")
                    .copy(participants = listOf("player-2"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository, nowSeconds = { 5_000L })

            advanceUntilIdle()

            val state = model.state.value
            assertFalse(state.isMatchOver)
            assertTrue(state.canRatePlayers)
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
    fun `canRate for the match itself is unaffected by canRatePlayers`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", startsAtSeconds = 1_000, durationMin = 1, status = MatchStatus.OPEN, organizerId = "someone-else")
                    .copy(participants = listOf("user-1"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository, nowSeconds = { 5_000L })

            advanceUntilIdle()

            val state = model.state.value
            assertTrue(state.canRate)
            assertFalse(state.canRatePlayers)
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
}
