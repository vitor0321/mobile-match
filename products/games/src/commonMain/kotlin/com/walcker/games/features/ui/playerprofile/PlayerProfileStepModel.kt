package com.walcker.games.features.ui.playerprofile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.error.GamesError
import com.walcker.games.features.domain.usecase.GetMyMatchesUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.identity.api.LogoutService
import com.walcker.identity.api.SessionHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PlayerProfileStepModel(
    private val sessionHolder: SessionHolder,
    private val getMyMatches: GetMyMatchesUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val logoutService: LogoutService,
) : ScreenModel {

    private val _state = MutableStateFlow(PlayerProfileState())
    val state: StateFlow<PlayerProfileState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            sessionHolder.currentUser.collect { session ->
                if (session != null) {
                    _state.update { it.copy(userName = session.displayName, userEmail = session.email) }
                    loadStats(session.uid)
                }
            }
        }
    }

    fun onEvent(event: PlayerProfileEvent) {
        when (event) {
            PlayerProfileEvent.Refresh -> {
                val currentState = _state.value
                if (currentState.userName != null) {
                    // Re-derive userId from current state's email/name (not ideal but safe)
                    // Better: track uid in state
                }
            }
            PlayerProfileEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
            PlayerProfileEvent.LogoutRequested -> logout()
        }
    }

    private fun logout() {
        screenModelScope.launch {
            logoutService.logout()
                .onSuccess {
                    // Logout succeeded. The MatchScaffold will detect
                    // isAuthenticated=false and show the login screen.
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
            getMyMatches(userId, nowSeconds)
                .onSuccess { result ->
                    val allMatches = result.active + result.past
                    val organized = allMatches.count { match ->
                        match.role == com.walcker.games.features.domain.model.MatchRole.ORGANIZER
                    }
                    val participated = allMatches.count { match ->
                        match.role == com.walcker.games.features.domain.model.MatchRole.PARTICIPANT
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            matchesOrganized = organized,
                            matchesParticipated = participated,
                        )
                    }
                }
                .onFailure { error ->
                    val message = (error as? GamesError)?.message ?: error.message ?: "Erro"
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
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
