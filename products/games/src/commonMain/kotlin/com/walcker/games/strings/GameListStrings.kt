package com.walcker.games.strings

internal data class GameListStrings(
    val title: String,
    val subtitle: String,
    val allSportsChip: String,
    val radiusLabel: (Int) -> String,
    val emptyMessage: String,
    val loadErrorMessage: String,
    val loadingLabel: String,
    val perPlayer: (String) -> String,
    val playersAndSlots: (confirmed: Int, total: Int, openSlots: Int, slotWord: String) -> String,
    val slotsBadge: (openSlots: Int) -> String,
    val showMapAction: String,
    val showListAction: String,
    val ratingsCount: (Int) -> String,
)

internal val gameListStringsEn =
    GameListStrings(
        title = "Open slots",
        subtitle = "Find matches near you",
        allSportsChip = "All",
        showMapAction = "Show map",
        showListAction = "Show list",
        radiusLabel = { km -> "Radius: $km km" },
        emptyMessage = "No open slots in your area right now.",
        loadErrorMessage = "Could not load matches.",
        loadingLabel = "Loading matches…",
        perPlayer = { price -> "$price per player" },
        playersAndSlots = { c, t, _, _ -> "$c/$t players" },
        slotsBadge = { open ->
            when (open) {
                0 -> "Full"
                1 -> "1 slot"
                else -> "$open slots"
            }
        },
        ratingsCount = { n -> if (n == 1) "1 review" else "$n reviews" },
    )

internal val gameListStringsPt =
    GameListStrings(
        title = "Vagas abertas",
        subtitle = "Encontre partidas perto de você",
        allSportsChip = "Todos",
        showMapAction = "Ver no mapa",
        showListAction = "Ver em lista",
        radiusLabel = { km -> "Raio: $km km" },
        emptyMessage = "Nenhuma vaga aberta na sua região agora.",
        loadErrorMessage = "Não foi possível carregar as partidas.",
        loadingLabel = "Carregando partidas…",
        perPlayer = { price -> "$price por jogador" },
        playersAndSlots = { c, t, open, word -> "$c/$t jogadores · $open $word" },
        slotsBadge = { open ->
            when (open) {
                0 -> "Lotado"
                1 -> "1 vaga"
                else -> "$open vagas"
            }
        },
        ratingsCount = { n -> if (n == 1) "1 avaliação" else "$n avaliações" },
    )
