package com.walcker.games.features.ui.shared.teamShuffle

import com.walcker.games.fake.FakeCrashReporter
import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.FakePlayerRepository
import com.walcker.games.fake.FakeRatingRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.game
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SetTeamAssignmentsUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.SubmitSkillRatingUseCase
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamShuffleStepModelTest {
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
        playerRepository: FakePlayerRepository = FakePlayerRepository(),
        ratingRepository: FakeRatingRepository = FakeRatingRepository(),
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
        crashReporter: FakeCrashReporter = FakeCrashReporter(),
        matchId: String = "match-1",
    ) = TeamShuffleStepModel(
        matchId = matchId,
        getGameById = GetGameByIdUseCaseImpl(gameRepository),
        observeMatch = ObserveMatchUseCaseImpl(gameRepository),
        observeParticipants = ObserveParticipantsUseCaseImpl(gameRepository),
        setTeamAssignments = SetTeamAssignmentsUseCaseImpl(gameRepository),
        submitSkillRating = SubmitSkillRatingUseCase(ratingRepository),
        playerRepository = playerRepository,
        ratingRepository = ratingRepository,
        sessionHolder = sessionHolder,
        stringsHolder = stringsHolder,
        crashReporter = crashReporter,
    )

    @Test
    fun `shuffling teams writes a balanced assignment for the selected team count and players per team`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", organizerId = "player-1")
                    .copy(participants = listOf("p1", "p2", "p3", "p4"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()
            gameRepository.emitParticipants(
                Result.success(fakeParticipantsSummaryFor(listOf("p1", "p2", "p3", "p4"))),
            )
            advanceUntilIdle()

            model.onEvent(TeamShuffleEvent.TeamCountSelected(2))
            model.onEvent(TeamShuffleEvent.PlayersPerTeamSelected(2))
            model.onEvent(TeamShuffleEvent.ShuffleTeams)
            advanceUntilIdle()

            val call = gameRepository.setTeamAssignmentsCalls.single()
            assertEquals("match-1", call.matchId)
            assertEquals(2, call.teamCount)
            assertEquals(2, call.playersPerTeam)
            assertEquals(setOf("p1", "p2", "p3", "p4"), call.assignments.keys)
            assertTrue(call.assignments.values.all { it in 0..1 })
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
                    playersPerTeam = 5,
                ).copy(participants = listOf("p1", "p2"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(TeamShuffleEvent.MovePlayerToTeam(userId = "p1", teamIndex = 1))
            advanceUntilIdle()

            val call = gameRepository.setTeamAssignmentsCalls.single()
            assertEquals(2, call.teamCount)
            assertEquals(5, call.playersPerTeam)
            assertEquals(mapOf("p1" to 1, "p2" to 1), call.assignments)
        }

    @Test
    fun `loading a match with existing selections syncs team count and players per team`() =
        runTest(testDispatcher) {
            val myGame =
                game(
                    id = "match-1",
                    organizerId = "player-1",
                    teamCount = 4,
                    teamAssignments = mapOf("p1" to 0, "p2" to 1, "p3" to 2, "p4" to 3),
                    playersPerTeam = 3,
                ).copy(participants = listOf("p1", "p2", "p3", "p4"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            assertEquals(4, model.state.value.selectedTeamCount)
            assertEquals(3, model.state.value.selectedPlayersPerTeam)
        }

    @Test
    fun `observing a match with existing selections syncs team count and players per team`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            val myGame =
                game(
                    id = "match-1",
                    organizerId = "player-1",
                    teamCount = 3,
                    teamAssignments = mapOf("p1" to 0, "p2" to 1, "p3" to 2),
                    playersPerTeam = 4,
                ).copy(participants = listOf("p1", "p2", "p3"))
            gameRepository.emitMatch(Result.success(myGame))
            advanceUntilIdle()

            assertEquals(3, model.state.value.selectedTeamCount)
            assertEquals(4, model.state.value.selectedPlayersPerTeam)
        }

    @Test
    fun `selecting a team count only updates local state, no write happens yet`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(TeamShuffleEvent.TeamCountSelected(4))

            assertEquals(4, model.state.value.selectedTeamCount)
            assertTrue(gameRepository.setTeamAssignmentsCalls.isEmpty())
        }

    @Test
    fun `selecting players per team only updates local state, no write happens yet`() =
        runTest(testDispatcher) {
            val gameRepository = FakeGameRepository()
            val model = buildModel(gameRepository = gameRepository)
            advanceUntilIdle()

            model.onEvent(TeamShuffleEvent.PlayersPerTeamSelected(7))

            assertEquals(7, model.state.value.selectedPlayersPerTeam)
            assertTrue(gameRepository.setTeamAssignmentsCalls.isEmpty())
        }

    @Test
    fun `canRateSkill is true for the organizer`() =
        runTest(testDispatcher) {
            val myGame = game(id = "match-1", organizerId = "user-1")
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            assertTrue(model.state.value.canRateSkill)
        }

    @Test
    fun `canRateSkill is false for a participant who is not the organizer`() =
        runTest(testDispatcher) {
            val myGame = game(id = "match-1", organizerId = "someone-else")
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val model = buildModel(gameRepository = gameRepository)

            advanceUntilIdle()

            assertFalse(model.state.value.canRateSkill)
        }

    @Test
    fun `confirmed players load skill rating averages and the organizer's own ratings`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", organizerId = "user-1").copy(participants = listOf("p1", "p2"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val playerRepository =
                FakePlayerRepository(
                    skillRatingSummaryResult =
                        Result.success(mapOf("p1" to PlayerRatingSummary(rating = 8f, ratingCount = 2))),
                )
            val ratingRepository = FakeRatingRepository(mySkillRatingsResult = Result.success(mapOf("p1" to 8)))
            val model =
                buildModel(gameRepository = gameRepository, playerRepository = playerRepository, ratingRepository = ratingRepository)
            advanceUntilIdle()
            gameRepository.emitParticipants(Result.success(fakeParticipantsSummaryFor(listOf("p1", "p2"))))
            advanceUntilIdle()

            assertEquals(8f, model.state.value.skillRatings["p1"]?.rating)
            assertEquals(8, model.state.value.mySkillRatings["p1"])
        }

    @Test
    fun `rating a player's skill updates the average and the organizer's own rating locally`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", organizerId = "user-1").copy(participants = listOf("p1"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val ratingRepository =
                FakeRatingRepository(
                    submitResult = Result.success(SubmitRatingOutcome.Recorded(averageRating = 7f, ratingCount = 1)),
                )
            val model = buildModel(gameRepository = gameRepository, ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(TeamShuffleEvent.RateSkill(userId = "p1", rating = 7))
            advanceUntilIdle()

            assertEquals(listOf("skill:p1"), ratingRepository.submitCalls)
            assertEquals(7f, model.state.value.skillRatings["p1"]?.rating)
            assertEquals(7, model.state.value.mySkillRatings["p1"])
        }

    @Test
    fun `a failed skill rating surfaces an action error`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", organizerId = "user-1").copy(participants = listOf("p1"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val ratingRepository = FakeRatingRepository(submitResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(gameRepository = gameRepository, ratingRepository = ratingRepository)
            advanceUntilIdle()

            model.onEvent(TeamShuffleEvent.RateSkill(userId = "p1", rating = 7))
            advanceUntilIdle()

            assertEquals(stringsHolder.strings.manageMatch.skillRatingError, model.state.value.actionErrorMessage)
        }

    @Test
    fun `shuffling teams balances skill ratings across teams, defaulting unrated players to beginner`() =
        runTest(testDispatcher) {
            val myGame =
                game(id = "match-1", organizerId = "player-1")
                    .copy(participants = listOf("aceA", "aceB", "rookieA", "rookieB"))
            val gameRepository = FakeGameRepository(getGameByIdResult = Result.success(myGame))
            val playerRepository =
                FakePlayerRepository(
                    skillRatingSummaryResult =
                        Result.success(
                            mapOf(
                                "aceA" to PlayerRatingSummary(rating = 10f, ratingCount = 1),
                                "aceB" to PlayerRatingSummary(rating = 10f, ratingCount = 1),
                            ),
                        ),
                )
            val model = buildModel(gameRepository = gameRepository, playerRepository = playerRepository)
            advanceUntilIdle()
            gameRepository.emitParticipants(
                Result.success(fakeParticipantsSummaryFor(listOf("aceA", "aceB", "rookieA", "rookieB"))),
            )
            advanceUntilIdle()

            model.onEvent(TeamShuffleEvent.TeamCountSelected(2))
            model.onEvent(TeamShuffleEvent.PlayersPerTeamSelected(2))
            model.onEvent(TeamShuffleEvent.ShuffleTeams)
            advanceUntilIdle()

            val assignments = gameRepository.setTeamAssignmentsCalls.single().assignments
            val teamWithAceA = assignments.getValue("aceA")
            val teamWithAceB = assignments.getValue("aceB")
            assertTrue(teamWithAceA != teamWithAceB)
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
