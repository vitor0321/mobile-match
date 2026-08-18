package com.walcker.games.features.ui.map

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.data.preferences.GamesPreferences
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the map screen.
 */
internal data class MapState(
    val pins: List<MapPin> = emptyList(),
    val nearbyMatches: List<NearbyMatch> = emptyList(),
    val camera: MapCamera = DEFAULT_CAMERA,
    val userLocation: MapCamera? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
) {
    internal companion object {
        // São Paulo center (Av. Paulista area) until the user's GPS resolves.
        val DEFAULT_CAMERA = MapCamera(lat = -23.5505, lng = -46.6333, zoom = 13f)
    }
}

/**
 * ScreenModel for the map view.
 *
 * Reuses the same cached match stream as the list ([GameRepository.observeMatches])
 * and applies the same sport filter from [GamesPreferences], so map and list stay
 * consistent. Each match becomes a [MapPin].
 *
 * Also requests GPS permission and tracks user location to center the map.
 */
internal class MapStepModel(
    private val repository: GameRepository,
    private val preferences: GamesPreferences,
    private val locationProvider: LocationProvider,
) : ScreenModel {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init {
        observeMatches()
        refresh()
        requestLocationPermissionAndTrack()
    }

    private fun observeMatches() {
        combine(
            preferences.selectedSport,
            repository.observeMatches(),
            _state,
        ) { sport, games, currentState ->
            val filtered = games.filter { game -> sport == null || game.sport == sport }

            // Calculate distances if user location is available
            val nearby = if (currentState.userLocation != null) {
                val userCoords = Coordinates(lat = currentState.userLocation.lat, lng = currentState.userLocation.lng)
                filtered
                    .map { game ->
                        val gameCoords = Coordinates(lat = game.lat, lng = game.lng)
                        val distance = calculateDistance(userCoords, gameCoords)
                        NearbyMatch(game, distance)
                    }
                    .sortedBy { it.distanceKm }
            } else {
                emptyList()
            }

            Pair(filtered, nearby)
        }.onEach { (games, nearby) ->
            _state.update {
                it.copy(
                    pins = games.map { game -> game.toMapPin() },
                    nearbyMatches = nearby,
                    isLoading = false,
                )
            }
        }.launchIn(screenModelScope)
    }

    fun onRefresh() {
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            repository.refresh()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun refresh() {
        screenModelScope.launch {
            repository.refresh()
        }
    }

    private fun requestLocationPermissionAndTrack() {
        screenModelScope.launch {
            // Request GPS permission
            val granted = locationProvider.requestPermission()
            if (granted) {
                // Get current location and update camera
                locationProvider.currentLocation().onSuccess { coords ->
                    val userCamera = MapCamera(lat = coords.lat, lng = coords.lng, zoom = 15f)
                    _state.update {
                        it.copy(
                            camera = userCamera,
                            userLocation = userCamera,
                        )
                    }
                }.onFailure { e ->
                    // Location unavailable; keep default camera
                }
            }
        }
    }
}
