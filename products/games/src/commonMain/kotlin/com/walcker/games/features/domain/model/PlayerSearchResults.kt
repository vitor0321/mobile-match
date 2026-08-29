package com.walcker.games.features.domain.model

internal data class PlayerSearchResults(
    val players: List<PlayerSearchResult>,
    val reachedLimit: Boolean,
) {
    internal companion object {
        internal val Empty: PlayerSearchResults =
            PlayerSearchResults(players = emptyList(), reachedLimit = false)
    }
}
