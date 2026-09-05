package com.walcker.games.features.ui.home.map

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.data.home.preferences.GamesPreferences
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.isDiscoverable
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.features.ui.home.map.mapper.toMapPin
import com.walcker.games.features.ui.home.map.model.MapPin
import com.walcker.games.features.ui.home.map.model.MatchPreview
import com.walcker.games.features.ui.home.map.model.NearbyMatch
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.analytics.MatchListSource
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.geo.distanceKm
import com.walcker.match.core.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MILLIS_PER_SECOND = 1000L

internal data class MapState(
    val pins: List<MapPin> = emptyList(),
    val matches: List<Game> = emptyList(),
    val nearbyMatches: List<NearbyMatch> = emptyList(),
    val selectedMatchId: String? = null,
    val isPreviewDismissed: Boolean = false,
    val camera: MapCamera = DEFAULT_CAMERA,
    val userLocation: MapCamera? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val locationUnavailable: Boolean = false,
    val hasLocationPermission: Boolean = false,
) {
    val previewMatch: MatchPreview?
        get() {
            if (isPreviewDismissed) return null
            val selected = selectedMatchId?.let { id -> matches.find { it.id == id } }
            val game = selected ?: nearbyMatches.firstOrNull()?.game ?: matches.firstOrNull() ?: return null
            val distance = nearbyMatches.find { it.game.id == game.id }?.distanceKm
            return MatchPreview(game = game, distanceKm = distance)
        }

    internal companion object {
        val DEFAULT_CAMERA = MapCamera(lat = -23.5505, lng = -46.6333, zoom = 13f)
    }
}

internal class MapStepModel(
    private val repository: GameRepository,
    private val preferences: GamesPreferences,
    private val locationProvider: LocationProvider,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : ScreenModel {
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private val userCoordinates = MutableStateFlow<Coordinates?>(null)

    init {
        analytics.track(AnalyticsEvent.MatchListViewed(MatchListSource.MAP))
        observeMatches()
        requestLocationPermissionAndTrack()
    }

    private fun observeMatches() {
        combine(
            preferences.selectedSport,
            repository.observeMatches(),
            userCoordinates,
        ) { sport, games, userCoords ->
            val nowSeconds =
                kotlin.time.Clock.System
                    .now()
                    .toEpochMilliseconds() / MILLIS_PER_SECOND
            val filtered =
                games.filter { game ->
                    (sport == null || game.sport == sport) && game.isDiscoverable(nowSeconds)
                }

            val nearby =
                if (userCoords == null) {
                    emptyList()
                } else {
                    filtered
                        .map { game ->
                            NearbyMatch(
                                game = game,
                                distanceKm =
                                    distanceKm(
                                        userCoords,
                                        Coordinates(lat = game.lat, lng = game.lng),
                                    ),
                            )
                        }.sortedBy { it.distanceKm }
                }

            Pair(filtered, nearby)
        }.onEach { (games, nearby) ->
            _state.update {
                it.copy(
                    pins = games.map { game -> game.toMapPin() },
                    matches = games,
                    nearbyMatches = nearby,
                    isLoading = false,
                )
            }
        }.launchIn(screenModelScope)
    }

    fun onPinSelected(matchId: String) {
        _state.update { it.copy(selectedMatchId = matchId, isPreviewDismissed = false) }
    }

    fun onPreviewDismissed() {
        _state.update { it.copy(isPreviewDismissed = true) }
    }

    fun onRefresh() {
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            repository.refresh(preferences.radiusKm.first())
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onRetryLocation() {
        requestLocationPermissionAndTrack()
    }

    private fun requestLocationPermissionAndTrack() {
        screenModelScope.launch {
            _state.update { it.copy(locationUnavailable = false) }

            val permissionGranted = locationProvider.requestPermission()
            _state.update { it.copy(hasLocationPermission = permissionGranted) }

            if (!permissionGranted) {
                _state.update { it.copy(locationUnavailable = true) }
                return@launch
            }

            locationProvider
                .currentLocation()
                .onSuccess { coords ->
                    userCoordinates.value = coords
                    val userCamera = MapCamera(lat = coords.lat, lng = coords.lng, zoom = 15f)
                    _state.update {
                        it.copy(
                            camera = userCamera,
                            userLocation = userCamera,
                            locationUnavailable = false,
                        )
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    _state.update { it.copy(locationUnavailable = true) }
                }
        }
    }
}
