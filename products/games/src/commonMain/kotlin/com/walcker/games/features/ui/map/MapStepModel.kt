package com.walcker.games.features.ui.map

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.data.preferences.GamesPreferences
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.MatchListSource
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.geo.distanceKm
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
    /**
     * A permissão de localização foi negada, ou o GPS não respondeu.
     *
     * Precisa ser estado, e não silêncio: sem posição a lista de "próximas"
     * fica vazia para sempre e a câmera fica parada na Paulista. Antes disso
     * aparecer aqui, a tela não tinha como dizer se não havia partida perto ou
     * se ela simplesmente não sabia onde o usuário estava.
     */
    val locationUnavailable: Boolean = false,
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
    private val analytics: AnalyticsTracker,
) : ScreenModel {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    /**
     * Posição do usuário como fluxo próprio.
     *
     * Antes o `combine` das partidas incluía o `_state` inteiro só para ler a
     * localização — e o `onEach` dele escrevia de volta no `_state`. Isso é um
     * laço: toda escrita de estado, inclusive ligar e desligar o `isRefreshing`
     * do pull-to-refresh, remontava a lista de pinos e recalculava todas as
     * distâncias. Terminava só porque `MapPin` é data class e o StateFlow
     * conflaciona valor igual — dependia de sorte, não de desenho.
     */
    private val userCoordinates = MutableStateFlow<Coordinates?>(null)

    init {
        analytics.track(AnalyticsEvent.MatchListViewed(MatchListSource.MAP))
        observeMatches()
        refresh()
        requestLocationPermissionAndTrack()
    }

    private fun observeMatches() {
        combine(
            preferences.selectedSport,
            repository.observeMatches(),
            userCoordinates,
        ) { sport, games, userCoords ->
            val filtered = games.filter { game -> sport == null || game.sport == sport }

            // Sem posição não existe "próxima": ordenar por uma distância que
            // não dá para medir seria inventar ordem.
            val nearby = if (userCoords == null) {
                emptyList()
            } else {
                filtered
                    .map { game ->
                        NearbyMatch(
                            game = game,
                            distanceKm = distanceKm(
                                userCoords,
                                Coordinates(lat = game.lat, lng = game.lng),
                            ),
                        )
                    }
                    .sortedBy { it.distanceKm }
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

    /**
     * Nova tentativa de localizar, para quando a pessoa concede a permissão nos
     * ajustes e volta para a tela. Sem isto, negar uma vez deixava o mapa cego
     * até o app ser reiniciado.
     */
    fun onRetryLocation() {
        requestLocationPermissionAndTrack()
    }

    private fun refresh() {
        screenModelScope.launch {
            repository.refresh()
        }
    }

    private fun requestLocationPermissionAndTrack() {
        screenModelScope.launch {
            _state.update { it.copy(locationUnavailable = false) }

            if (!locationProvider.requestPermission()) {
                _state.update { it.copy(locationUnavailable = true) }
                return@launch
            }

            locationProvider.currentLocation()
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
                }
                .onFailure {
                    // Permissão concedida mas o GPS não resolveu: para a tela é
                    // a mesma coisa que negar — ela continua sem saber onde
                    // desenhar o usuário.
                    _state.update { it.copy(locationUnavailable = true) }
                }
        }
    }
}
