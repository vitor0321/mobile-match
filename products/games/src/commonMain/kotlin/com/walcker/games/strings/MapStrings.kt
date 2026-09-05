package com.walcker.games.strings

internal data class MapStrings(
    val searchPlaceholder: String,
    val loadingLabel: String,
    val nearbyTitle: String,
    val nearbySubtitle: (Int) -> String,
    val locationUnavailableTitle: String,
    val locationUnavailableBody: String,
    val retry: String,
    val closeContentDescription: String,
    val showListAction: String,
    val previewDetailsAction: String,
    val directionsContentDescription: String,
    val openLocationTitle: String,
    val openInGoogleMaps: String,
    val openInWaze: String,
    val openLocationCancel: String,
)

internal val mapStringsEn =
    MapStrings(
        showListAction = "Show list",
        searchPlaceholder = "Where do you want to play?",
        loadingLabel = "Loading the map…",
        nearbyTitle = "Matches near you",
        nearbySubtitle = { n -> if (n == 1) "1 match nearby" else "$n matches nearby" },
        locationUnavailableTitle = "No access to your location",
        locationUnavailableBody =
            "The map opens in São Paulo and the nearby list stays empty " +
                "until you allow location access.",
        retry = "Try again",
        closeContentDescription = "Close",
        previewDetailsAction = "View details",
        directionsContentDescription = "Get directions",
        openLocationTitle = "Open location in",
        openInGoogleMaps = "Google Maps",
        openInWaze = "Waze",
        openLocationCancel = "Cancel",
    )

internal val mapStringsPt =
    MapStrings(
        showListAction = "Ver em lista",
        searchPlaceholder = "Onde você quer jogar?",
        loadingLabel = "Carregando o mapa…",
        nearbyTitle = "Partidas perto de você",
        nearbySubtitle = { n -> if (n == 1) "1 partida por perto" else "$n partidas por perto" },
        locationUnavailableTitle = "Sem acesso à sua localização",
        locationUnavailableBody =
            "O mapa abre em São Paulo e a lista de partidas próximas " +
                "fica vazia até você liberar a localização.",
        retry = "Tentar de novo",
        closeContentDescription = "Fechar",
        previewDetailsAction = "Ver detalhes",
        directionsContentDescription = "Traçar rota",
        openLocationTitle = "Abrir localização em",
        openInGoogleMaps = "Google Maps",
        openInWaze = "Waze",
        openLocationCancel = "Cancelar",
    )
