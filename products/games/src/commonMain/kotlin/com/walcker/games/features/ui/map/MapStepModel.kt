package com.walcker.games.features.ui.map

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.data.preferences.GamesPreferences
import com.walcker.games.features.domain.repository.GameRepository
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
    val camera: MapCamera = DEFAULT_CAMERA,
    val isLoading: Boolean = true,
) {
    internal companion object {
        // São Paulo center (Av. Paulista area) until the user's GPS resolves (ETAPA2).
        val DEFAULT_CAMERA = MapCamera(lat = -23.5505, lng = -46.6333, zoom = 13f)
    }
}

/**
 * ScreenModel for the map view.
 *
 * Reuses the same cached match stream as the list ([GameRepository.observeMatches])
 * and applies the same sport filter from [GamesPreferences], so map and list stay
 * consistent. Each match becomes a [MapPin].
 */
internal class MapStepModel(
    private val repository: GameRepository,
    private val preferences: GamesPreferences,
) : ScreenModel {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init {
        observeMatches()
        refresh()
    }

    private fun observeMatches() {
        combine(
            preferences.selectedSport,
            repository.observeMatches(),
        ) { sport, games ->
            games.filter { game -> sport == null || game.sport == sport }
        }.onEach { games ->
            _state.update {
                it.copy(
                    pins = games.map { game -> game.toMapPin() },
                    isLoading = false,
                )
            }
        }.launchIn(screenModelScope)
    }

    private fun refresh() {
        screenModelScope.launch {
            repository.refresh()
        }
    }
}
