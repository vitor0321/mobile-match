package com.walcker.games.features.ui.player_details

import app.cash.turbine.test
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.games.features.domain.usecase.GetPlayerDetailsUseCaseImpl
import com.walcker.games.features.domain.usecase.GetPlayerRatingsUseCaseImpl
import com.walcker.games.fake.FakePlayerRepository
import com.walcker.games.fake.playerDetails
import com.walcker.games.fake.rating
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerDetailsStepModelTest {

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
        repository: FakePlayerRepository,
        userId: String = "player-1",
    ) = PlayerDetailsStepModel(
        userId = userId,
        getPlayerDetails = GetPlayerDetailsUseCaseImpl(repository),
        getPlayerRatings = GetPlayerRatingsUseCaseImpl(repository),
        stringsHolder = stringsHolder,
    )

    @Test
    fun `initial state carries the requested user and nothing else`() = runTest(testDispatcher) {
        val model = buildModel(FakePlayerRepository())

        val state = model.state.value

        assertEquals("player-1", state.userId)
        assertNull(state.player)
        assertTrue(state.previewRatings.isEmpty())
        assertEquals(0, state.distribution.total)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loads the profile and the ratings sample`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            detailsResult = Result.success(playerDetails(displayName = "Ana Souza")),
            ratingPages = mapOf(
                null to Result.success(
                    RatingsPage(
                        ratings = listOf(
                            rating(id = "1", stars = 5),
                            rating(id = "2", stars = 4),
                            rating(id = "3", stars = 5),
                        ),
                        nextCursor = null,
                    ),
                ),
            ),
        )
        val model = buildModel(repository)

        advanceUntilIdle()

        val state = model.state.value
        assertEquals("Ana Souza", state.player?.displayName)
        assertFalse(state.isLoadingPlayer)
        assertFalse(state.isLoadingRatings)
        assertEquals(3, state.previewRatings.size)
        assertEquals(listOf(0, 0, 0, 1, 2), state.distribution.counts)
        assertFalse(state.hasMoreRatings)
    }

    @Test
    fun `asks for a sample larger than the preview so the histogram is meaningful`() =
        runTest(testDispatcher) {
            val repository = FakePlayerRepository()
            buildModel(repository)

            advanceUntilIdle()

            val call = repository.ratingCalls.single()
            assertEquals("player-1", call.userId)
            assertEquals(PlayerDetailsState.RATINGS_SAMPLE_SIZE, call.limit)
            assertEquals(RatingSort.RECENT, call.sort)
            assertNull(call.cursor)
            assertTrue(PlayerDetailsState.RATINGS_SAMPLE_SIZE > PlayerDetailsState.PREVIEW_RATINGS_COUNT)
        }

    @Test
    fun `shows only the first reviews and offers the full list`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            ratingPages = mapOf(
                null to Result.success(
                    RatingsPage(
                        ratings = (1..8).map { rating(id = "r-$it") },
                        nextCursor = null,
                    ),
                ),
            ),
        )
        val model = buildModel(repository)

        advanceUntilIdle()

        val state = model.state.value
        assertEquals(PlayerDetailsState.PREVIEW_RATINGS_COUNT, state.previewRatings.size)
        assertTrue(state.hasMoreRatings)
        assertEquals(8, state.distribution.total)
    }

    @Test
    fun `a failed profile load surfaces an error and an effect`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            detailsResult = Result.failure(IllegalStateException("Jogador não encontrado")),
        )
        val model = buildModel(repository)

        model.effects.test {
            advanceUntilIdle()

            val effect = assertIs<PlayerDetailsEffect.ShowMessage>(awaitItem())
            assertEquals("Jogador não encontrado", effect.message)
            cancelAndIgnoreRemainingEvents()
        }

        val state = model.state.value
        assertNull(state.player)
        assertEquals("Jogador não encontrado", state.errorMessage)
        assertFalse(state.isLoadingPlayer)
        assertTrue(repository.ratingCalls.isEmpty())
    }

    @Test
    fun `a failed ratings load keeps the profile usable`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            ratingPages = mapOf(null to Result.failure(IllegalStateException("boom"))),
        )
        val model = buildModel(repository)

        advanceUntilIdle()

        val state = model.state.value
        assertNotNull(state.player)
        assertNull(state.errorMessage)
        assertTrue(state.previewRatings.isEmpty())
        assertFalse(state.isLoadingRatings)
    }

    @Test
    fun `retry clears the error and loads again`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            detailsResult = Result.failure(IllegalStateException("offline")),
        )
        val model = buildModel(repository)
        advanceUntilIdle()
        assertEquals("offline", model.state.value.errorMessage)

        repository.detailsResult = Result.success(playerDetails(displayName = "Ana Souza"))
        model.onEvent(PlayerDetailsEvents.RetryLoading)
        advanceUntilIdle()

        val state = model.state.value
        assertNull(state.errorMessage)
        assertEquals("Ana Souza", state.player?.displayName)
    }

    @Test
    fun `dismissing the error only clears the message`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            detailsResult = Result.failure(IllegalStateException("offline")),
        )
        val model = buildModel(repository)
        advanceUntilIdle()

        model.onEvent(PlayerDetailsEvents.DismissError)

        assertNull(model.state.value.errorMessage)
    }

    @Test
    fun `see all reviews emits a navigation effect with the loaded player`() =
        runTest(testDispatcher) {
            val repository = FakePlayerRepository(
                detailsResult = Result.success(
                    playerDetails(userId = "player-7", displayName = "Ana Souza"),
                ),
            )
            val model = buildModel(repository, userId = "player-7")
            advanceUntilIdle()

            model.effects.test {
                model.onEvent(PlayerDetailsEvents.SeeAllRatingsClicked)
                advanceUntilIdle()

                val effect = assertIs<PlayerDetailsEffect.NavigateToRatings>(awaitItem())
                assertEquals("player-7", effect.userId)
                assertEquals("Ana Souza", effect.playerName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `see all reviews is a no-op while the profile has not loaded`() = runTest(testDispatcher) {
        val repository = FakePlayerRepository(
            detailsResult = Result.failure(IllegalStateException("offline")),
        )
        val model = buildModel(repository)
        advanceUntilIdle()

        model.effects.test {
            awaitItem()

            model.onEvent(PlayerDetailsEvents.SeeAllRatingsClicked)
            advanceUntilIdle()

            expectNoEvents()
        }
    }
}
