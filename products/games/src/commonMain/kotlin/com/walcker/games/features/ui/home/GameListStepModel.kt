package com.walcker.games.features.ui.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.data.home.preferences.GamesPreferences
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCase
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.model.isDiscoverable
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.analytics.MatchListSource
import com.walcker.match.navigator.HomeViewCoordinator
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MILLIS_PER_SECOND = 1000L
private const val NEAREST_FALLBACK_RADIUS_KM = 20_000.0
private const val NEAREST_FALLBACK_LIMIT = 20

internal class GameListStepModel(
    private val repository: GameRepository,
    private val preferences: GamesPreferences,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val homeViewCoordinator: HomeViewCoordinator,
    private val sessionHolder: SessionHolder,
    private val observeAvailability: ObserveAvailabilityUseCase,
) : ScreenModel {
    init {
        analytics.track(AnalyticsEvent.MatchListViewed(MatchListSource.HOME))
    }

    private val strings get() = stringsHolder.resolveStringsOrDefault().gameList

    private val _state = MutableStateFlow(GameListState(strings = strings))
    val state: StateFlow<GameListState> = _state.asStateFlow()

    private val _effects = Channel<GameListEffect>(Channel.BUFFERED)
    val effects: Flow<GameListEffect> = _effects.receiveAsFlow()

    private val _isNearestFallback = MutableStateFlow(false)

    init {
        observePreferencesAndMatches()
        observeMySports()
        refresh()
    }

    private fun observeMySports() {
        screenModelScope.launch {
            sessionHolder.currentUser.collect { session ->
                if (session == null) {
                    _state.update { it.copy(mySports = emptySet()) }
                } else {
                    observeAvailabilityOf(session.uid)
                }
            }
        }
    }

    private fun observeAvailabilityOf(userId: String) {
        screenModelScope.launch {
            observeAvailability(userId).collect { result ->
                result.onSuccess { availability ->
                    _state.update { it.copy(mySports = availability.sports) }
                }
            }
        }
    }

    fun onEvent(event: GameListEvents) {
        when (event) {
            is GameListEvents.Refresh -> refresh()
            is GameListEvents.SelectSport -> {
                screenModelScope.launch { preferences.setSelectedSport(event.sport) }
            }
            is GameListEvents.SetRadius -> {
                screenModelScope.launch {
                    _isNearestFallback.value = false
                    preferences.setRadiusKm(event.radiusKm)
                    refresh()
                }
            }
            is GameListEvents.SelectGame -> {
                screenModelScope.launch {
                    _effects.send(GameListEffect.NavigateToMatchDetail(event.gameId))
                }
            }
            is GameListEvents.LoadMore -> loadMore()
        }
    }

    private fun observePreferencesAndMatches() {
        combine(
            preferences.selectedSport,
            preferences.radiusKm,
            repository.observeMatches(),
            repository.observeHasMoreMatches(),
            _isNearestFallback,
        ) { sport, radius, games, hasMore, isFallback ->
            GamesUpdate(sport, radius, games, hasMore, isFallback)
        }.onEach { update ->
            val nowSeconds =
                kotlin.time.Clock.System
                    .now()
                    .toEpochMilliseconds() / MILLIS_PER_SECOND
            val filtered =
                update.games.filter { game ->
                    (update.sport == null || game.sport == update.sport) && game.isDiscoverable(nowSeconds)
                }
            val ordered =
                if (update.isFallback) {
                    filtered.take(NEAREST_FALLBACK_LIMIT)
                } else {
                    filtered.sortedBy { it.startsAtSeconds }
                }
            _state.update {
                it.copy(
                    strings = strings,
                    selectedSport = update.sport,
                    radiusKm = update.radius,
                    games = ordered.toImmutableList(),
                    preferencesLoaded = true,
                    hasMore = if (update.isFallback) false else update.hasMore,
                    isShowingNearestFallback = update.isFallback,
                )
            }
            homeViewCoordinator.markHomeDataReady()
        }.launchIn(screenModelScope)
    }

    private fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val isFirstLaunch = preferences.lastSyncAt.first() == null
            val radiusKm = preferences.radiusKm.first()
            repository
                .refresh(radiusKm)
                .onSuccess {
                    preferences.setLastSyncAt(kotlin.time.Clock.System.now().toEpochMilliseconds())
                    val hasNearbyMatches = repository.observeMatches().first().isNotEmpty()
                    if (isFirstLaunch && !hasNearbyMatches) {
                        showNearestFallback()
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: strings.loadErrorMessage,
                        )
                    }
                    homeViewCoordinator.markHomeDataReady()
                }
        }
    }

    private suspend fun showNearestFallback() {
        repository
            .refresh(NEAREST_FALLBACK_RADIUS_KM)
            .onSuccess {
                _isNearestFallback.value = true
                _state.update { it.copy(isLoading = false) }
            }.onFailure { error ->
                crashReporter.recordException(error)
                _state.update { it.copy(isLoading = false) }
            }
    }

    private fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return

        screenModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val radiusKm = preferences.radiusKm.first()
            repository
                .loadMoreMatches(radiusKm)
                .onSuccess {
                    _state.update { it.copy(isLoadingMore = false) }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = error.message ?: strings.loadErrorMessage,
                        )
                    }
                }
        }
    }

    private data class GamesUpdate(
        val sport: Sport?,
        val radius: Double,
        val games: List<Game>,
        val hasMore: Boolean,
        val isFallback: Boolean,
    )
}
