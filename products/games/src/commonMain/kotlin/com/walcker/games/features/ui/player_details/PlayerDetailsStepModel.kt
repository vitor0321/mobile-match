package com.walcker.games.features.ui.player_details

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.toDistribution
import com.walcker.games.features.domain.usecase.GetPlayerDetailsUseCase
import com.walcker.games.features.domain.usecase.GetPlayerRatingsUseCase
import com.walcker.games.features.ui.player_details.PlayerDetailsState.Companion.PREVIEW_RATINGS_COUNT
import com.walcker.games.features.ui.player_details.PlayerDetailsState.Companion.RATINGS_SAMPLE_SIZE
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the player details screen.
 *
 * Loads the profile first, then a sample of received ratings. A ratings failure
 * is deliberately non-fatal: the profile is still worth showing, so the error is
 * swallowed into an empty reviews section instead of blanking the screen.
 */
internal class PlayerDetailsStepModel(
    private val userId: String,
    private val getPlayerDetails: GetPlayerDetailsUseCase,
    private val getPlayerRatings: GetPlayerRatingsUseCase,
    private val stringsHolder: GamesStringsHolder,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().playerDetails

    private val _state = MutableStateFlow(PlayerDetailsState(userId = userId))
    val state: StateFlow<PlayerDetailsState> = _state.asStateFlow()

    private val _effects = Channel<PlayerDetailsEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerDetailsEffect> = _effects.receiveAsFlow()

    init {
        loadPlayerData()
    }

    fun onEvent(event: PlayerDetailsEvents) {
        when (event) {
            PlayerDetailsEvents.DismissError ->
                _state.update { it.copy(errorMessage = null) }

            PlayerDetailsEvents.RetryLoading ->
                loadPlayerData()

            PlayerDetailsEvents.SeeAllRatingsClicked ->
                navigateToRatings()
        }
    }

    private fun loadPlayerData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingPlayer = true, errorMessage = null) }

            getPlayerDetails(userId)
                .onSuccess { player ->
                    _state.update { it.copy(player = player, isLoadingPlayer = false) }
                    loadRatingsSample()
                }
                .onFailure { error ->
                    val message = error.message ?: strings.errorLoading
                    _state.update { it.copy(isLoadingPlayer = false, errorMessage = message) }
                    _effects.send(PlayerDetailsEffect.ShowMessage(message))
                }
        }
    }

    private suspend fun loadRatingsSample() {
        _state.update { it.copy(isLoadingRatings = true) }

        getPlayerRatings(
            userId = userId,
            limit = RATINGS_SAMPLE_SIZE,
            sort = RatingSort.RECENT,
        )
            .onSuccess { page ->
                _state.update {
                    it.copy(
                        previewRatings = page.ratings
                            .take(PREVIEW_RATINGS_COUNT)
                            .toImmutableList(),
                        distribution = page.ratings.toDistribution(),
                        hasMoreRatings = page.hasMore ||
                            page.ratings.size > PREVIEW_RATINGS_COUNT,
                        isLoadingRatings = false,
                    )
                }
            }
            .onFailure {
                // Reviews are supplementary: keep the profile usable.
                _state.update { it.copy(isLoadingRatings = false) }
            }
    }

    private fun navigateToRatings() {
        val player = _state.value.player ?: return
        screenModelScope.launch {
            _effects.send(
                PlayerDetailsEffect.NavigateToRatings(
                    userId = player.userId,
                    playerName = player.displayName,
                ),
            )
        }
    }
}
