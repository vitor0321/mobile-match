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
)

internal val mapStringsEn = MapStrings(
    searchPlaceholder = "Where do you want to play?",
    loadingLabel = "Loading the map…",
    nearbyTitle = "Matches near you",
    nearbySubtitle = { n -> if (n == 1) "1 match nearby" else "$n matches nearby" },
    locationUnavailableTitle = "No access to your location",
    locationUnavailableBody = "The map opens in São Paulo and the nearby list stays empty " +
        "until you allow location access.",
    retry = "Try again",
    closeContentDescription = "Close",
)

internal val mapStringsPt = MapStrings(
    searchPlaceholder = "Onde você quer jogar?",
    loadingLabel = "Carregando o mapa…",
    nearbyTitle = "Partidas perto de você",
    nearbySubtitle = { n -> if (n == 1) "1 partida por perto" else "$n partidas por perto" },
    locationUnavailableTitle = "Sem acesso à sua localização",
    locationUnavailableBody = "O mapa abre em São Paulo e a lista de partidas próximas " +
        "fica vazia até você liberar a localização.",
    retry = "Tentar de novo",
    closeContentDescription = "Fechar",
)
