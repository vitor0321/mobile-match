package com.walcker.games.features.ui.playerprofile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.error.GamesError
import com.walcker.games.features.domain.usecase.GetMyMatchesUseCase
import com.walcker.games.features.domain.usecase.GetUserRatingsUseCase
import com.walcker.games.features.domain.usecase.ObserveAvailabilityUseCase
import com.walcker.games.features.domain.usecase.SetAvailabilityUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.LogoutService
import com.walcker.identity.api.SessionHolder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PlayerProfileStepModel(
    private val sessionHolder: SessionHolder,
    private val getMyMatches: GetMyMatchesUseCase,
    private val getUserRatings: GetUserRatingsUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val logoutService: LogoutService,
    private val observeAvailability: ObserveAvailabilityUseCase,
    private val setAvailability: SetAvailabilityUseCase,
) : ScreenModel {

    private val _state = MutableStateFlow(PlayerProfileState())
    val state: StateFlow<PlayerProfileState> = _state.asStateFlow()

    private val _effects = Channel<PlayerProfileEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerProfileEffect> = _effects.receiveAsFlow()

    private var currentUserId: String? = null

    init {
        screenModelScope.launch {
            sessionHolder.currentUser.collect { session ->
                if (session != null) {
                    currentUserId = session.uid
                    _state.update { it.copy(userName = session.displayName, userEmail = session.email) }
                    loadStats(session.uid)
                    observeAvailabilityOf(session.uid)
                } else {
                     currentUserId = null
                    _state.update {
                        it.copy(
                            isLoading = false,
                            userName = null,
                            userEmail = null,
                            matchesOrganized = 0,
                            matchesParticipated = 0,
                            ratings = emptyList(),
                            averageRating = 0f,
                            totalRatings = 0,
                            isAvailable = false,
                        )
                    }
                }
            }
        }
    }

    private fun observeAvailabilityOf(userId: String) {
        screenModelScope.launch {
            observeAvailability(userId).collect { result ->
                result.onSuccess { availability ->
                    _state.update { it.copy(isAvailable = availability.isAvailable) }
                }
            }
        }
    }

    private fun changeAvailability(isAvailable: Boolean) {
        val userId = currentUserId
        if (userId == null) {
            screenModelScope.launch { _effects.send(PlayerProfileEffect.RequireLogin) }
            return
        }
        val strings = stringsHolder.resolveStringsOrDefault().playerProfile

        screenModelScope.launch {
            _state.update {
                it.copy(
                    isAvailable = isAvailable,
                    isUpdatingAvailability = true,
                    availabilityErrorMessage = null,
                )
            }

            setAvailability(userId, isAvailable)
                .onSuccess {
                    _state.update { it.copy(isUpdatingAvailability = false) }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isAvailable = !isAvailable,
                            isUpdatingAvailability = false,
                            availabilityErrorMessage = strings.availabilityError,
                        )
                    }
                }
        }
    }

    fun onEvent(event: PlayerProfileEvent) {
        when (event) {
            PlayerProfileEvent.Refresh -> {
                val currentState = _state.value
                if (currentState.userName != null) {
                }
            }
            PlayerProfileEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
            PlayerProfileEvent.LogoutRequested -> logout()
            is PlayerProfileEvent.AvailabilityChanged -> changeAvailability(event.isAvailable)
            PlayerProfileEvent.DismissAvailabilityError ->
                _state.update { it.copy(availabilityErrorMessage = null) }
        }
    }

    private fun logout() {
        screenModelScope.launch {
            logoutService.logout()
                .onSuccess {
                }
                .onFailure { error ->
                    val message = error.message ?: "Erro ao fazer logout"
                    _state.update { it.copy(errorMessage = message) }
                }
        }
    }

    private fun loadStats(userId: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val nowSeconds = getCurrentEpochSeconds()

             val matchesResult = getMyMatches(userId, nowSeconds)
            val ratingsResult = getUserRatings(userId, limit = 50)

            matchesResult
                .onSuccess { result ->
                    val allMatches = result.active + result.past
                    val organized = allMatches.count { match ->
                        match.role == com.walcker.games.features.domain.model.MatchRole.ORGANIZER
                    }
                    val participated = allMatches.count { match ->
                        match.role == com.walcker.games.features.domain.model.MatchRole.PARTICIPANT
                    }

                    ratingsResult
                        .onSuccess { ratings ->
                            val avgRating = if (ratings.isNotEmpty()) {
                                ratings.map { it.rating }.average().toFloat()
                            } else {
                                0f
                            }

                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    matchesOrganized = organized,
                                    matchesParticipated = participated,
                                    ratings = ratings,
                                    averageRating = avgRating,
                                    totalRatings = ratings.size,
                                )
                            }
                        }
                        .onFailure { error ->
                            val message = (error as? GamesError)?.message ?: error.message ?: "Erro ao carregar avaliações"
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    matchesOrganized = organized,
                                    matchesParticipated = participated,
                                    errorMessage = message,
                                )
                            }
                        }
                }
                .onFailure { error ->
                    val message = (error as? GamesError)?.message ?: error.message ?: "Erro"
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }

    private fun getCurrentEpochSeconds(): Long =
        kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000L
}
