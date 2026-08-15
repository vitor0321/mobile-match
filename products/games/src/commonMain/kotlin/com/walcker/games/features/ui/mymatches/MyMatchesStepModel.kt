package com.walcker.games.features.ui.mymatches

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.error.GamesError
import com.walcker.games.features.domain.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.usecase.GetMyMatchesUseCase
import com.walcker.games.features.domain.usecase.LeaveMatchUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MyMatchesStepModel(
    private val getMyMatches: GetMyMatchesUseCase,
    private val cancelMatch: CancelMatchUseCase,
    private val leaveMatch: LeaveMatchUseCase,
    private val stringsHolder: GamesStringsHolder,
) : ScreenModel {

    private val _state = MutableStateFlow(MyMatchesState())
    val state: StateFlow<MyMatchesState> = _state.asStateFlow()

    private val _effects = Channel<MyMatchesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Stub for now: real app reads from SessionHolder. Keeping it as a constant
     * keeps the screen functional during E2E and unit-style verification.
     */
    private val currentUserId = "user_vitor"

    init {
        refresh()
    }

    fun onEvent(event: MyMatchesEvent) {
        when (event) {
            MyMatchesEvent.Refresh -> refresh()
            is MyMatchesEvent.TabSelected -> _state.update { it.copy(activeTab = event.tab) }
            MyMatchesEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
            is MyMatchesEvent.CancelRequested -> handleCancel(event.gameId)
            is MyMatchesEvent.LeaveRequested -> handleLeave(event.gameId)
        }
    }

    private fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val nowSeconds = getCurrentEpochSeconds()
            getMyMatches(currentUserId, nowSeconds)
                .onSuccess { matches ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            active = matches.active,
                            past = matches.past,
                        )
                    }
                }
                .onFailure { error ->
                    val message = (error as? GamesError)?.message ?: error.message ?: "Erro"
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }

    private fun handleCancel(gameId: String) {
        screenModelScope.launch {
            cancelMatch(gameId)
                .onSuccess { refresh() }
                .onFailure { error ->
                    val message = (error as? GamesError)?.message
                        ?: stringsHolder.resolveStringsOrDefault().myMatches.cancelError
                    _state.update { it.copy(errorMessage = message) }
                }
        }
    }

    private fun handleLeave(gameId: String) {
        screenModelScope.launch {
            leaveMatch(gameId)
                .onSuccess { refresh() }
                .onFailure { error ->
                    val message = (error as? GamesError)?.message
                        ?: stringsHolder.resolveStringsOrDefault().myMatches.leaveError
                    _state.update { it.copy(errorMessage = message) }
                }
        }
    }

    /**
     * Returns the current epoch seconds. Expects an expect/actual override in
     * commonTest etc.; default implementation delegates to [kotlin.time.Clock]
     * which is part of stdlib since Kotlin 1.9.
     */
    private fun getCurrentEpochSeconds(): Long =
        kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000L
}
