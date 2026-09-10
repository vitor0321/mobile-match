package com.walcker.games.features.ui.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.playerProfile.usecase.ObserveAvailabilityUseCase
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.isDiscoverable
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.features.ui.home.map.MapCamera
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.analytics.MatchListSource
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.location.LocationProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MILLIS_PER_SECOND = 1000L
private const val SEARCH_RADIUS_KM = 20_000.0
private const val INITIAL_MAP_ZOOM = 13f

internal class SearchStepModel(
    private val repository: GameRepository,
    private val stringsHolder: GamesStringsHolder,
    private val analytics: AnalyticsTracker,
    private val sessionHolder: SessionHolder,
    private val observeAvailability: ObserveAvailabilityUseCase,
    private val locationProvider: LocationProvider,
    private val crashReporter: CrashReporter,
) : ScreenModel {
    init {
        analytics.track(AnalyticsEvent.MatchListViewed(MatchListSource.SEARCH))
    }

    private val gamesStrings get() = stringsHolder.resolveStringsOrDefault()

    private val _state =
        MutableStateFlow(
            SearchState(
                strings = gamesStrings.search,
                cardStrings = gamesStrings.gameList,
                mapStrings = gamesStrings.map,
            ),
        )
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects: Flow<SearchEffect> = _effects.receiveAsFlow()

    private var allMatches: List<Game> = emptyList()
    private var hasLoadedMapOnce = false

    init {
        loadAllMatches()
    }

    private fun loadAllMatches() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            allMatches = emptyList()

            val accumulated = mutableListOf<Game>()
            var cursors: List<String?>? = null
            var receivedAnyPage = false

            do {
                val result = repository.searchMatches(SEARCH_RADIUS_KM, cursors)
                val page = result.getOrNull()
                if (page == null) {
                    if (!receivedAnyPage) {
                        _state.update { it.copy(isLoading = false, errorMessage = gamesStrings.search.loadErrorMessage) }
                    }
                    return@launch
                }

                receivedAnyPage = true
                accumulated += page.games
                allMatches = accumulated
                cursors = page.rangeCursors

                _state.update { it.copy(isLoading = false) }
                applyFilters(resetPage = false)
            } while (cursors.any { it != null })
        }
    }

    init {
        screenModelScope.launch {
            sessionHolder.currentUser.collect { session ->
                if (session == null) {
                    _state.update { it.copy(mySports = emptySet()) }
                } else {
                    observeMySports(session.uid)
                }
            }
        }
    }

    private fun observeMySports(userId: String) {
        screenModelScope.launch {
            observeAvailability(userId).collect { result ->
                result.onSuccess { availability ->
                    _state.update { current -> current.copy(mySports = availability.sports) }
                }
            }
        }
    }

    fun onEvent(event: SearchEvents) {
        when (event) {
            is SearchEvents.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                applyFilters()
            }
            is SearchEvents.DateRangeChanged -> {
                _state.update { it.copy(filters = it.filters.copy(startDateMs = event.startDateMs, endDateMs = event.endDateMs)) }
                applyFilters()
            }
            is SearchEvents.SportFilterChanged -> {
                _state.update { it.copy(filters = it.filters.copy(sports = event.sports)) }
                applyFilters()
            }
            is SearchEvents.PriceRangeChanged -> {
                _state.update { it.copy(filters = it.filters.copy(minPrice = event.minPrice, maxPrice = event.maxPrice)) }
                applyFilters()
            }
            SearchEvents.ResetFilters -> {
                _state.update { it.copy(query = "", filters = SearchFilters(), showFiltersPanel = false) }
                applyFilters()
            }
            SearchEvents.ToggleFiltersPanel -> {
                _state.update { it.copy(showFiltersPanel = !it.showFiltersPanel) }
            }
            is SearchEvents.SelectGame -> {
                screenModelScope.launch {
                    _effects.send(SearchEffect.NavigateToMatchDetail(event.gameId))
                }
            }
            SearchEvents.Retry -> loadAllMatches()
            SearchEvents.ToggleMap -> {
                val turningOn = !_state.value.showMap
                _state.update { it.copy(showMap = !it.showMap, selectedMapMatchId = null) }
                if (turningOn && !hasLoadedMapOnce) {
                    hasLoadedMapOnce = true
                    loadMapNearUserLocation()
                }
            }
            is SearchEvents.PinSelected -> {
                _state.update { it.copy(selectedMapMatchId = event.matchId) }
            }
            SearchEvents.MapPreviewDismissed -> {
                _state.update { it.copy(selectedMapMatchId = null) }
            }
            SearchEvents.LoadMoreResults -> {
                _state.update { it.copy(visibleResultsCount = it.visibleResultsCount + SEARCH_RESULTS_PAGE_SIZE) }
            }
            is SearchEvents.MapCameraIdle -> {
                searchMapArea(event.camera)
            }
            SearchEvents.MapRetry -> {
                searchMapArea(_state.value.mapCamera)
            }
            SearchEvents.MapErrorDismissed -> {
                _state.update { it.copy(mapErrorMessage = null) }
            }
        }
    }

    private fun loadMapNearUserLocation() {
        screenModelScope.launch {
            _state.update { it.copy(isMapLoading = true, mapErrorMessage = null) }
            val permissionGranted = locationProvider.requestPermission()
            val coordinates = if (permissionGranted) locationProvider.currentLocation().getOrNull() else null
            val camera =
                coordinates?.let { MapCamera(lat = it.lat, lng = it.lng, zoom = INITIAL_MAP_ZOOM) }
                    ?: DEFAULT_SEARCH_MAP_CAMERA
            searchMapArea(camera)
        }
    }

    private fun searchMapArea(camera: MapCamera) {
        screenModelScope.launch {
            _state.update { it.copy(isMapLoading = true, mapErrorMessage = null, mapCamera = camera) }
            val center = Coordinates(lat = camera.lat, lng = camera.lng)
            repository
                .searchMatchesNear(center, camera.approximateRadiusKm)
                .onSuccess { page ->
                    val nowSeconds =
                        kotlin.time.Clock.System
                            .now()
                            .toEpochMilliseconds() / MILLIS_PER_SECOND
                    val discoverable = page.games.filter { it.isDiscoverable(nowSeconds) }
                    _state.update { it.copy(mapResults = discoverable.toImmutableList(), isMapLoading = false) }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update { it.copy(isMapLoading = false, mapErrorMessage = gamesStrings.search.loadErrorMessage) }
                }
        }
    }

    private fun applyFilters(resetPage: Boolean = true) {
        val state = _state.value
        val trimmedQuery = state.query.trim().lowercase()
        val filters = state.filters

        val nowSeconds =
            kotlin.time.Clock.System
                .now()
                .toEpochMilliseconds() / MILLIS_PER_SECOND
        val filtered =
            allMatches
                .filter { game ->
                    if (!game.isDiscoverable(nowSeconds)) return@filter false

                    val matchesText =
                        if (trimmedQuery.isBlank()) {
                            true
                        } else {
                            game.venueName.lowercase().contains(trimmedQuery) ||
                                game.neighborhood.lowercase().contains(trimmedQuery) ||
                                game.city.lowercase().contains(trimmedQuery) ||
                                game.sport.label
                                    .lowercase()
                                    .contains(trimmedQuery)
                        }

                    val gameStartMs = game.startsAtSeconds * 1000
                    val matchesDateRange =
                        if (filters.startDateMs != null && gameStartMs < filters.startDateMs) {
                            false
                        } else if (filters.endDateMs != null && gameStartMs > filters.endDateMs) {
                            false
                        } else {
                            true
                        }

                    val matchesSport =
                        if (filters.sports.isEmpty()) {
                            true
                        } else {
                            game.sport in filters.sports
                        }

                    val gamePrice = game.priceCents / 100f
                    val matchesPrice =
                        if (filters.minPrice != null && gamePrice < filters.minPrice) {
                            false
                        } else if (filters.maxPrice != null && gamePrice > filters.maxPrice) {
                            false
                        } else {
                            true
                        }

                    matchesText && matchesDateRange && matchesSport && matchesPrice
                }.sortedBy { it.startsAtSeconds }

        _state.update {
            it.copy(
                strings = gamesStrings.search,
                cardStrings = gamesStrings.gameList,
                mapStrings = gamesStrings.map,
                results = filtered.toImmutableList(),
                visibleResultsCount = if (resetPage) SEARCH_RESULTS_PAGE_SIZE else it.visibleResultsCount,
            )
        }
    }
}
