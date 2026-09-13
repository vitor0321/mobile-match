package com.walcker.games.features.ui.shared.manageMatch

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.usecase.CancelMatchSeriesUseCase
import com.walcker.games.features.domain.shared.usecase.CancelMatchUseCase
import com.walcker.games.features.domain.shared.usecase.GetGameByIdUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveMatchUseCase
import com.walcker.games.features.domain.shared.usecase.ObserveParticipantsUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ManageMatchState(
    val match: Game? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null,
    val successMessage: String? = null,
    val isCancellingMatch: Boolean = false,
    val showCancelConfirmDialog: Boolean = false,
    val isCancellingSeries: Boolean = false,
    val showCancelSeriesConfirmDialog: Boolean = false,
    val currentUserId: String? = null,
    val vipCount: Int = 0,
)

internal sealed interface ManageMatchEvent {
    data object Retry : ManageMatchEvent

    data object DismissActionError : ManageMatchEvent

    data object DismissSuccess : ManageMatchEvent

    data object RequestCancelMatch : ManageMatchEvent

    data object ConfirmCancelMatch : ManageMatchEvent

    data object CancelCancelMatch : ManageMatchEvent

    data object RequestCancelSeries : ManageMatchEvent

    data object ConfirmCancelSeries : ManageMatchEvent

    data object CancelCancelSeries : ManageMatchEvent
}

internal sealed interface ManageMatchEffect {
    data object MatchCancelled : ManageMatchEffect
}

internal class ManageMatchStepModel(
    private val matchId: String,
    private val getGameById: GetGameByIdUseCase,
    private val observeMatch: ObserveMatchUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val cancelMatch: CancelMatchUseCase,
    private val cancelMatchSeries: CancelMatchSeriesUseCase,
    private val sessionHolder: SessionHolder,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : ScreenModel {
    private val _state = MutableStateFlow(ManageMatchState())
    val state: StateFlow<ManageMatchState> = _state.asStateFlow()

    private val _effects = Channel<ManageMatchEffect>(Channel.BUFFERED)
    val effects: Flow<ManageMatchEffect> = _effects.receiveAsFlow()

    private var currentUserId: String? = null

    init {
        loadMatch()
        subscribeToMatch()
        subscribeToCurrentUser()
        subscribeToVipCount()
    }

    private fun subscribeToVipCount() {
        screenModelScope.launch {
            observeParticipants(matchId)
                .catch { }
                .collect { result ->
                    result.onSuccess { summary ->
                        _state.update { it.copy(vipCount = summary.confirmed.count { player -> player.isVip }) }
                    }
                }
        }
    }

    fun onEvent(event: ManageMatchEvent) {
        when (event) {
            ManageMatchEvent.Retry -> loadMatch()
            ManageMatchEvent.DismissActionError -> {
                _state.update { it.copy(actionErrorMessage = null) }
            }
            ManageMatchEvent.DismissSuccess -> {
                _state.update { it.copy(successMessage = null) }
            }
            ManageMatchEvent.RequestCancelMatch -> {
                _state.update { it.copy(showCancelConfirmDialog = true) }
            }
            ManageMatchEvent.ConfirmCancelMatch -> cancelMatchAction()
            ManageMatchEvent.CancelCancelMatch -> {
                _state.update { it.copy(showCancelConfirmDialog = false) }
            }
            ManageMatchEvent.RequestCancelSeries -> {
                _state.update { it.copy(showCancelSeriesConfirmDialog = true) }
            }
            ManageMatchEvent.ConfirmCancelSeries -> cancelMatchSeriesAction()
            ManageMatchEvent.CancelCancelSeries -> {
                _state.update { it.copy(showCancelSeriesConfirmDialog = false) }
            }
        }
    }

    private fun loadMatch() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getGameById(matchId)
                .onSuccess { game ->
                    _state.update {
                        it.copy(isLoading = false, match = game)
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = stringsHolder.resolveStringsOrDefault().manageMatch.notFound,
                        )
                    }
                }
        }
    }

    private fun subscribeToMatch() {
        screenModelScope.launch {
            observeMatch(matchId)
                .catch { }
                .collect { result ->
                    result.onSuccess { game ->
                        _state.update {
                            it.copy(match = game, isLoading = false)
                        }
                    }
                }
        }
    }

    private fun subscribeToCurrentUser() {
        screenModelScope.launch {
            currentUserId = sessionHolder.currentUser.first()?.uid
            _state.update { it.copy(currentUserId = currentUserId) }
        }
    }

    private fun cancelMatchAction() {
        val sport =
            _state.value.match
                ?.sport
                ?.name ?: UNKNOWN_SPORT
        val strings = stringsHolder.resolveStringsOrDefault().manageMatch
        screenModelScope.launch {
            _state.update { it.copy(isCancellingMatch = true, showCancelConfirmDialog = false, actionErrorMessage = null) }

            cancelMatch(matchId)
                .onSuccess {
                    analytics.track(AnalyticsEvent.MatchCancelled(sport, isSeries = false))
                    _state.update { it.copy(isCancellingMatch = false) }
                    _effects.send(ManageMatchEffect.MatchCancelled)
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isCancellingMatch = false,
                            actionErrorMessage = strings.cancelError,
                        )
                    }
                }
        }
    }

    private fun cancelMatchSeriesAction() {
        val sport =
            _state.value.match
                ?.sport
                ?.name ?: UNKNOWN_SPORT
        val strings = stringsHolder.resolveStringsOrDefault().manageMatch
        screenModelScope.launch {
            _state.update {
                it.copy(isCancellingSeries = true, showCancelSeriesConfirmDialog = false, actionErrorMessage = null)
            }

            cancelMatchSeries(matchId)
                .onSuccess {
                    analytics.track(AnalyticsEvent.MatchCancelled(sport, isSeries = true))
                    _state.update {
                        it.copy(
                            isCancellingSeries = false,
                            successMessage = strings.cancelSeriesSuccess,
                        )
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isCancellingSeries = false,
                            actionErrorMessage = strings.cancelSeriesError,
                        )
                    }
                }
        }
    }

    private companion object {
        const val UNKNOWN_SPORT = "unknown"
    }
}
