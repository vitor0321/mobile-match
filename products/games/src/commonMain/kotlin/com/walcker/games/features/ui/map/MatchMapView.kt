package com.walcker.games.features.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Centro inicial da câmera do mapa.
 */
internal data class MapCamera(
    val lat: Double,
    val lng: Double,
    val zoom: Float = 13f,
)

/**
 * Mapa nativo multiplataforma que renderiza os pinos das partidas.
 *
 * - Android: implementado com `maps-compose` (Google Maps).
 * - iOS: stub até a interop com MapKit (Phase 6-ETAPA2+).
 *
 * @param pins partidas a exibir como marcadores
 * @param camera centro/zoom inicial da câmera
 * @param onPinClick chamado com o matchId quando um marcador é tocado
 * @param modifier modificador do container do mapa
 */
@Composable
internal expect fun MatchMapView(
    pins: List<MapPin>,
    camera: MapCamera,
    onPinClick: (String) -> Unit,
    modifier: Modifier,
)
