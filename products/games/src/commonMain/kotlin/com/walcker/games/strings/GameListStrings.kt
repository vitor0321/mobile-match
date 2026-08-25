package com.walcker.games.strings

internal data class GameListStrings(
    val title: String,
    val subtitle: String,
    val allSportsChip: String,
    val radiusLabel: (Int) -> String,
    val emptyMessage: String,
    val loadErrorMessage: String,
    val perPlayer: (String) -> String,
    val playersAndSlots: (confirmed: Int, total: Int, openSlots: Int, slotWord: String) -> String,
    /**
     * Short label for the slot badge on a match card: "2 vagas", "1 vaga", "Lotado".
     *
     * Pluralisation lives here rather than in the design system, which has no
     * strings layer — the old `SlotBadge` hardcoded pt-BR and could not be
     * translated.
     */
    val slotsBadge: (openSlots: Int) -> String,
)

internal val gameListStringsEn = GameListStrings(
    title = "Open slots",
    subtitle = "Find matches near you",
    allSportsChip = "All",
    radiusLabel = { km -> "Radius: $km km" },
    emptyMessage = "No open slots in your area right now.",
    loadErrorMessage = "Could not load matches.",
    perPlayer = { price -> "$price per player" },
    playersAndSlots = { c, t, _, _ -> "$c/$t players" },
    slotsBadge = { open ->
        when (open) {
            0 -> "Full"
            1 -> "1 slot"
            else -> "$open slots"
        }
    },
)

internal val gameListStringsPt = GameListStrings(
    title = "Vagas abertas",
    subtitle = "Encontre partidas perto de você",
    allSportsChip = "Todos",
    radiusLabel = { km -> "Raio: $km km" },
    emptyMessage = "Nenhuma vaga aberta na sua região agora.",
    loadErrorMessage = "Não foi possível carregar as partidas.",
    perPlayer = { price -> "$price por jogador" },
    playersAndSlots = { c, t, open, word -> "$c/$t jogadores · $open $word" },
    slotsBadge = { open ->
        when (open) {
            0 -> "Lotado"
            1 -> "1 vaga"
            else -> "$open vagas"
        }
    },
)
