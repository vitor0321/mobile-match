package com.walcker.games.features.ui.player_ratings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.usecase.GetPlayerRatingsUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the paginated list of ratings a player received.
 *
 * Paging is cursor-based and server-ordered, so changing [RatingSort] restarts
 * from the first page rather than reordering what happens to be in memory.
 */
internal class PlayerRatingsListStepModel(
    private val userId: String,
    private val playerName: String,
    private val getPlayerRatings: GetPlayerRatingsUseCase,
    private val stringsHolder: GamesStringsHolder,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().playerRatings

    private val _state = MutableStateFlow(
        PlayerRatingsState(userId = userId, playerName = playerName),
    )
    val state: StateFlow<PlayerRatingsState> = _state.asStateFlow()

    private val _effects = Channel<PlayerRatingsEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerRatingsEffect> = _effects.receiveAsFlow()

    /** Cursor for the next page; `null` once the list is exhausted. */
    private var nextCursor: String? = null

    /**
     * The in-flight page request. Cancelled whenever the list restarts so a
     * slow response from the previous sort can never append stale rows.
     */
    private var loadJob: Job? = null

    init {
        loadFirstPage()
    }

    fun onEvent(event: PlayerRatingsEvents) {
        when (event) {
            PlayerRatingsEvents.Retry -> loadFirstPage()

            PlayerRatingsEvents.LoadNextPage -> loadNextPage()

            is PlayerRatingsEvents.SortChanged -> {
                if (event.sort != _state.value.sort) {
                    _state.update { it.copy(sort = event.sort) }
                    loadFirstPage()
                }
            }
        }
    }

    private fun loadFirstPage() {
        loadJob?.cancel()
        nextCursor = null
        _state.update {
            it.copy(
                isLoadingFirstPage = true,
                isLoadingNextPage = false,
                errorMessage = null,
            )
        }

        loadJob = screenModelScope.launch {
            getPlayerRatings(
                userId = userId,
                limit = GetPlayerRatingsUseCase.DEFAULT_PAGE_SIZE,
                sort = _state.value.sort,
                cursor = null,
            )
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _state.update {
                        it.copy(
                            ratings = page.ratings.toImmutableList(),
                            hasMore = page.hasMore,
                            isLoadingFirstPage = false,
                        )
                    }
                }
                .onFailure { error -> handleFailure(error, isFirstPage = true) }
        }
    }

    private fun loadNextPage() {
        val cursor = nextCursor ?: return
        val current = _state.value
        if (current.isLoadingFirstPage || current.isLoadingNextPage) return

        _state.update { it.copy(isLoadingNextPage = true) }

        loadJob = screenModelScope.launch {
            getPlayerRatings(
                userId = userId,
                limit = GetPlayerRatingsUseCase.DEFAULT_PAGE_SIZE,
                sort = current.sort,
                cursor = cursor,
            )
                .onSuccess { page ->
                    nextCursor = page.nextCursor
                    _state.update {
                        it.copy(
                            ratings = (it.ratings + page.ratings).toImmutableList(),
                            hasMore = page.hasMore,
                            isLoadingNextPage = false,
                        )
                    }
                }
                .onFailure { error -> handleFailure(error, isFirstPage = false) }
        }
    }

    /**
     * A failed first page owns the screen (empty list, retry button); a failed
     * next page keeps what is already visible and only surfaces a snackbar.
     */
    private suspend fun handleFailure(error: Throwable, isFirstPage: Boolean) {
        val message = error.message ?: strings.errorLoading
        _state.update {
            it.copy(
                isLoadingFirstPage = false,
                isLoadingNextPage = false,
                errorMessage = if (isFirstPage) message else it.errorMessage,
            )
        }
        _effects.send(PlayerRatingsEffect.ShowMessage(message))
    }
}
