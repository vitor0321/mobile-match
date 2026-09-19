package com.walcker.games.features.ui.shared.managePlayers

import com.walcker.games.fake.FakeAnalyticsTracker
import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakePlayerRepository
import com.walcker.games.fake.FakeRatingRepository
import com.walcker.games.fake.FakeReportRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.fake.rating
import com.walcker.games.fake.testUserSession
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.model.SubmitReportOutcome
import com.walcker.games.features.domain.shared.usecase.BanPlayerFromMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ConfirmWaitlistedPlayerUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SetVipStatusUseCaseImpl
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ManagePlayersStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = GamesStringsHolder().apply { setStrings(PtBrGamesStrings) }
    private val manageStrings = PtBrGamesStrings.manageMatch

    private val organizerSession = testUserSession(uid = "organizer-1")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private class Fixture(
        val model: ManagePlayersStepModel,
        val gameRepository: FakeGameRepository,
        val ratingRepository: FakeRatingRepository,
        val reportRepository: FakeReportRepository,
        val crashReporter: FakeCrashReporter,
    )

    private fun buildFixture(
        gameRepository: FakeGameRepository = FakeGameRepository(),
        ratingRepository: FakeRatingRepository = FakeRatingRepository(),
        reportRepository: FakeReportRepository = FakeReportRepository(),
        playerRepository: FakePlayerRepository = FakePlayerRepository(),
        sessionHolder: FakeSessionHolder = FakeSessionHolder(organizerSession),
        crashReporter: FakeCrashReporter = FakeCrashReporter(),
    ): Fixture {
        val model =
            ManagePlayersStepModel(
                matchId = "match-1",
                getGameById = GetGameByIdUseCaseImpl(gameRepository),
                observeMatch = ObserveMatchUseCaseImpl(gameRepository),
                observeParticipants = ObserveParticipantsUseCaseImpl(gameRepository),
                submitRating = SubmitRatingUseCase(ratingRepository),
                submitReport = SubmitReportUseCaseImpl(reportRepository),
                setVipStatus = SetVipStatusUseCaseImpl(gameRepository),
                confirmWaitlistedPlayer = ConfirmWaitlistedPlayerUseCaseImpl(gameRepository),
                banPlayerFromMatch = BanPlayerFromMatchUseCaseImpl(gameRepository),
                playerRepository = playerRepository,
                ratingRepository = ratingRepository,
                sessionHolder = sessionHolder,
                stringsHolder = stringsHolder,
                analytics = FakeAnalyticsTracker(),
                crashReporter = crashReporter,
            )
        return Fixture(model, gameRepository, ratingRepository, reportRepository, crashReporter)
    }

    private fun participant(
        userId: String,
        isConfirmed: Boolean = true,
        positionInWaitlist: Int? = null,
    ) = Participant(
        userId = userId,
        displayName = "Nome $userId",
        photoUrl = null,
        joinedAt = 0L,
        isConfirmed = isConfirmed,
        positionInWaitlist = positionInWaitlist,
    )

    private fun summary(
        confirmed: List<Participant> = emptyList(),
        waitlist: List<Participant> = emptyList(),
    ) = ParticipantsSummary(
        confirmed = confirmed,
        waitlist = waitlist,
        confirmedCount = confirmed.size,
    )

    @Test
    fun `the organizer can manage an open match`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()

            advanceUntilIdle()

            val state = fixture.model.state.value
            assertFalse(state.isLoading)
            assertEquals("match-1", state.match?.id)
            assertTrue(state.canManage)
        }

    @Test
    fun `someone other than the organizer cannot manage the match`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(sessionHolder = FakeSessionHolder(testUserSession(uid = "player-1")))

            advanceUntilIdle()

            assertFalse(fixture.model.state.value.canManage)
        }

    @Test
    fun `a cancelled match cannot be managed even by its organizer`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(game(status = MatchStatus.CANCELLED)))
            val fixture = buildFixture(gameRepository = gameRepository)

            advanceUntilIdle()

            assertFalse(fixture.model.state.value.canManage)
        }

    @Test
    fun `a load failure shows not found and reports the error`() =
        runTest(testDispatcher) {
            val cause = IllegalStateException("offline")
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.failure(cause))
            val fixture = buildFixture(gameRepository = gameRepository)

            advanceUntilIdle()

            val state = fixture.model.state.value
            assertFalse(state.isLoading)
            assertEquals(manageStrings.notFound, state.errorMessage)
            assertEquals(listOf<Throwable>(cause), fixture.crashReporter.recordedExceptions)
        }

    @Test
    fun `retrying after a failure loads the match`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.failure(IllegalStateException("offline")))
            val fixture = buildFixture(gameRepository = gameRepository)
            advanceUntilIdle()

            gameRepository.getGameByIdResult = Result.success(game())
            fixture.model.onEvent(ManagePlayersEvent.Retry)
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertNull(state.errorMessage)
            assertEquals("match-1", state.match?.id)
        }

    @Test
    fun `participants are split into confirmed and waitlist`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.gameRepository.emitParticipants(
                Result.success(
                    summary(
                        confirmed = listOf(participant("a"), participant("b")),
                        waitlist = listOf(participant("c", isConfirmed = false, positionInWaitlist = 1)),
                    ),
                ),
            )
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(listOf("a", "b"), state.confirmedPlayers.map { it.userId })
            assertEquals(listOf("c"), state.waitlistPlayers.map { it.userId })
        }

    @Test
    fun `ratings are loaded for the confirmed players`() =
        runTest(testDispatcher) {
            val ratings = mapOf("a" to PlayerRatingSummary(rating = 4.5f, ratingCount = 2))
            val fixture = buildFixture(playerRepository = FakePlayerRepository(ratingSummaryResult = Result.success(ratings)))
            advanceUntilIdle()

            fixture.gameRepository.emitParticipants(Result.success(summary(confirmed = listOf(participant("a")))))
            advanceUntilIdle()

            assertEquals(ratings, fixture.model.state.value.participantRatings)
        }

    @Test
    fun `making a player vip sends the new status and confirms by name`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ToggleVip(userId = "a", displayName = "Ana", currentlyVip = false))
            advanceUntilIdle()

            assertEquals(listOf(Triple("match-1", "a", true)), fixture.gameRepository.setVipStatusCalls)
            assertEquals(manageStrings.vipSuccessMessage("Ana"), fixture.model.state.value.successMessage)
            assertFalse(fixture.model.state.value.isUpdatingVip)
        }

    @Test
    fun `removing vip sends false and says it was removed`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ToggleVip(userId = "a", displayName = "Ana", currentlyVip = true))
            advanceUntilIdle()

            assertEquals(listOf(Triple("match-1", "a", false)), fixture.gameRepository.setVipStatusCalls)
            assertEquals(manageStrings.vipRemovedMessage("Ana"), fixture.model.state.value.successMessage)
        }

    @Test
    fun `a vip failure shows the vip error and reports it`() =
        runTest(testDispatcher) {
            val cause = IllegalStateException("functions down")
            val fixture = buildFixture(gameRepository = FakeGameRepository(setVipStatusResult = Result.failure(cause)))
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ToggleVip(userId = "a", displayName = "Ana", currentlyVip = false))
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(manageStrings.vipError, state.actionErrorMessage)
            assertNull(state.successMessage)
            assertFalse(state.isUpdatingVip)
            assertEquals(listOf<Throwable>(cause), fixture.crashReporter.recordedExceptions)
        }

    @Test
    fun `tapping vip twice before the first answer sends a single request`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ToggleVip(userId = "a", displayName = "Ana", currentlyVip = false))
            fixture.model.onEvent(ManagePlayersEvent.ToggleVip(userId = "a", displayName = "Ana", currentlyVip = false))
            advanceUntilIdle()

            assertEquals(1, fixture.gameRepository.setVipStatusCalls.size)
        }

    @Test
    fun `confirming a waitlisted player confirms by name`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ConfirmWaitlisted(userId = "c", displayName = "Caio"))
            advanceUntilIdle()

            assertEquals(listOf("match-1" to "c"), fixture.gameRepository.confirmWaitlistedPlayerCalls)
            assertEquals(manageStrings.confirmWaitlistedSuccessMessage("Caio"), fixture.model.state.value.successMessage)
            assertFalse(fixture.model.state.value.isConfirmingWaitlisted)
        }

    @Test
    fun `a failed waitlist confirmation shows its error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(confirmWaitlistedPlayerResult = Result.failure(IllegalStateException("full")))
            val fixture = buildFixture(gameRepository = gameRepository)
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ConfirmWaitlisted(userId = "c", displayName = "Caio"))
            advanceUntilIdle()

            assertEquals(manageStrings.confirmWaitlistedError, fixture.model.state.value.actionErrorMessage)
            assertFalse(fixture.model.state.value.isConfirmingWaitlisted)
        }

    @Test
    fun `tapping confirm twice before the first answer sends a single request`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ConfirmWaitlisted(userId = "c", displayName = "Caio"))
            fixture.model.onEvent(ManagePlayersEvent.ConfirmWaitlisted(userId = "c", displayName = "Caio"))
            advanceUntilIdle()

            assertEquals(1, fixture.gameRepository.confirmWaitlistedPlayerCalls.size)
        }

    @Test
    fun `requesting a ban only asks for confirmation`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.RequestBan(userId = "a", displayName = "Ana"))
            advanceUntilIdle()

            assertEquals("a" to "Ana", fixture.model.state.value.playerPendingBan)
            assertTrue(fixture.gameRepository.banPlayerFromMatchCalls.isEmpty())
        }

    @Test
    fun `cancelling a ban keeps the player`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.RequestBan(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(ManagePlayersEvent.CancelBan)
            advanceUntilIdle()

            assertNull(fixture.model.state.value.playerPendingBan)
            assertTrue(fixture.gameRepository.banPlayerFromMatchCalls.isEmpty())
        }

    @Test
    fun `confirming a ban removes the player from the confirmed list`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()
            fixture.gameRepository.emitParticipants(Result.success(summary(confirmed = listOf(participant("a"), participant("b")))))
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.RequestBan(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(ManagePlayersEvent.ConfirmBan)
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(listOf("match-1" to "a"), fixture.gameRepository.banPlayerFromMatchCalls)
            assertEquals(listOf("b"), state.confirmedPlayers.map { it.userId })
            assertNull(state.playerPendingBan)
            assertEquals(manageStrings.banSuccessMessage("Ana"), state.successMessage)
        }

    @Test
    fun `confirming a ban twice before the first answer sends a single request`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.RequestBan(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(ManagePlayersEvent.ConfirmBan)
            fixture.model.onEvent(ManagePlayersEvent.ConfirmBan)
            advanceUntilIdle()

            assertEquals(1, fixture.gameRepository.banPlayerFromMatchCalls.size)
        }

    @Test
    fun `confirming a ban with nobody pending does nothing`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.ConfirmBan)
            advanceUntilIdle()

            assertTrue(fixture.gameRepository.banPlayerFromMatchCalls.isEmpty())
        }

    @Test
    fun `a failed ban keeps the player and shows the error`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository(banPlayerFromMatchResult = Result.failure(IllegalStateException("denied")))
            val fixture = buildFixture(gameRepository = gameRepository)
            advanceUntilIdle()
            fixture.gameRepository.emitParticipants(Result.success(summary(confirmed = listOf(participant("a")))))
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.RequestBan(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(ManagePlayersEvent.ConfirmBan)
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(listOf("a"), state.confirmedPlayers.map { it.userId })
            assertEquals(manageStrings.banError, state.actionErrorMessage)
            assertNull(state.playerPendingBan)
            assertFalse(state.isBanningPlayer)
        }

    @Test
    fun `opening the rating sheet brings the rating already given to that player`() =
        runTest(testDispatcher) {
            val given = rating(id = "r1", stars = 3)
            val ratingRepository = FakeRatingRepository(ratingsGivenForMatchResult = Result.success(listOf(given)))
            val fixture = buildFixture(ratingRepository = ratingRepository)
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.OpenRatingSheet(userId = "player-1", displayName = "Pedro"))

            val state = fixture.model.state.value
            assertTrue(state.showRatingSheet)
            assertEquals("player-1" to "Pedro", state.selectedPlayerForRating)
            assertEquals(given, state.existingRatingForSelectedPlayer)
        }

    @Test
    fun `a new rating closes the sheet and updates the player average`() =
        runTest(testDispatcher) {
            val ratingRepository =
                FakeRatingRepository(submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 4.2f, ratingCount = 5)))
            val fixture = buildFixture(ratingRepository = ratingRepository)
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.OpenRatingSheet(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(ManagePlayersEvent.SubmitRating(rating = 4, comment = "Boa", reportReason = null, reportDetails = ""))
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertFalse(state.showRatingSheet)
            assertFalse(state.isSubmittingRating)
            assertNull(state.selectedPlayerForRating)
            assertEquals(PtBrGamesStrings.ratings.submitSuccess, state.ratingSuccessMessage)
            assertEquals(PlayerRatingSummary(rating = 4.2f, ratingCount = 5), state.participantRatings["a"])
            assertTrue(fixture.reportRepository.submitCalls.isEmpty())
        }

    @Test
    fun `a rating with a report sends both and tells the user about both`() =
        runTest(testDispatcher) {
            val fixture = buildFixture(reportRepository = FakeReportRepository(submitResult = Result.success(SubmitReportOutcome.Recorded)))
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.OpenRatingSheet(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(
                ManagePlayersEvent.SubmitRating(rating = 1, comment = "", reportReason = ReportReason.entries.first(), reportDetails = "faltou"),
            )
            advanceUntilIdle()

            assertEquals(1, fixture.reportRepository.submitCalls.size)
            val expected = "${PtBrGamesStrings.ratings.submitSuccess} ${PtBrGamesStrings.reports.success}"
            assertEquals(expected, fixture.model.state.value.ratingSuccessMessage)
        }

    @Test
    fun `a failed report still confirms the rating and reports the error`() =
        runTest(testDispatcher) {
            val cause = IllegalStateException("report down")
            val fixture = buildFixture(reportRepository = FakeReportRepository(submitResult = Result.failure(cause)))
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.OpenRatingSheet(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(
                ManagePlayersEvent.SubmitRating(rating = 1, comment = "", reportReason = ReportReason.entries.first(), reportDetails = ""),
            )
            advanceUntilIdle()

            assertEquals(PtBrGamesStrings.ratings.submitSuccess, fixture.model.state.value.ratingSuccessMessage)
            assertEquals(listOf<Throwable>(cause), fixture.crashReporter.recordedExceptions)
        }

    @Test
    fun `a failed rating shows the rating error and sends no report`() =
        runTest(testDispatcher) {
            val ratingRepository = FakeRatingRepository(submitResult = Result.failure(IllegalStateException("down")))
            val fixture = buildFixture(ratingRepository = ratingRepository)
            advanceUntilIdle()
            fixture.model.onEvent(ManagePlayersEvent.OpenRatingSheet(userId = "a", displayName = "Ana"))

            fixture.model.onEvent(
                ManagePlayersEvent.SubmitRating(rating = 1, comment = "", reportReason = ReportReason.entries.first(), reportDetails = ""),
            )
            advanceUntilIdle()

            val state = fixture.model.state.value
            assertEquals(PtBrGamesStrings.ratings.submitError, state.ratingErrorMessage)
            assertNull(state.ratingSuccessMessage)
            assertFalse(state.showRatingSheet)
            assertTrue(fixture.reportRepository.submitCalls.isEmpty())
        }

    @Test
    fun `submitting without a selected player does nothing`() =
        runTest(testDispatcher) {
            val fixture = buildFixture()
            advanceUntilIdle()

            fixture.model.onEvent(ManagePlayersEvent.SubmitRating(rating = 5, comment = "", reportReason = null, reportDetails = ""))
            advanceUntilIdle()

            assertTrue(fixture.ratingRepository.submitCalls.isEmpty())
        }
}
