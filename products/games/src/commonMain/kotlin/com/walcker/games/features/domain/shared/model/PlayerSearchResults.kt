package com.walcker.games.features.domain.shared.model

internal data class PlayerSearchResults(
    val players: List<PlayerSearchResult>,
    val reachedLimit: Boolean,
) {
    internal companion object {
        internal val Empty: PlayerSearchResults =
            PlayerSearchResults(players = emptyList(), reachedLimit = false)
    }
}
