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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /**
     * Guardado porque o toggle precisa do uid a cada toque, e a sessão só é
     * lida no `collect` do init.
     */
    private var currentUserId: String? = null

    init {
        screenModelScope.launch {
            sessionHolder.currentUser.collect { session ->
                if (session != null) {
                    currentUserId = session.uid
                    _state.update { it.copy(userName = session.displayName, userEmail = session.email) }
                    loadStats(session.uid)
                    observeAvailabilityOf(session.uid)
                }
            }
        }
    }

    /**
     * O switch reflete o documento, não o último toque: se a gravação falhar,
     * ou se a pessoa mudar a disponibilidade em outro aparelho, o snapshot é
     * quem manda. É o que evita o switch mentir sobre o estado real.
     */
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
        val userId = currentUserId ?: return
        val strings = stringsHolder.resolveStringsOrDefault().playerProfile

        screenModelScope.launch {
            // Otimista: o switch acompanha o dedo. O observe acima corrige se a
            // gravação não passar.
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
                    // Re-derive userId from current state's email/name (not ideal but safe)
                    // Better: track uid in state
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

            // Load matches and ratings in parallel
            val matchesResult = getMyMatches(userId, nowSeconds)
            val ratingsResult = getUserRatings(userId, limit = 50)

            // Process matches
            matchesResult
                .onSuccess { result ->
                    val allMatches = result.active + result.past
                    val organized = allMatches.count { match ->
                        match.role == com.walcker.games.features.domain.model.MatchRole.ORGANIZER
                    }
                    val participated = allMatches.count { match ->
                        match.role == com.walcker.games.features.domain.model.MatchRole.PARTICIPANT
                    }

                    // Process ratings
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
                            // Ratings failed but matches loaded successfully
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

    /**
     * Returns the current epoch seconds. Expects an expect/actual override in
     * commonTest etc.; default implementation delegates to [kotlin.time.Clock]
     * which is part of stdlib since Kotlin 1.9.
     */
    private fun getCurrentEpochSeconds(): Long =
        kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000L
}
