package com.walcker.games.features.domain.model

/**
 * Outcome of a player search.
 *
 * [reachedLimit] is not decoration: the query reads a capped number of profiles
 * and filters the rest in memory, so when the cap is hit the list genuinely may
 * be missing people. The UI has to say so instead of presenting a partial
 * result as if it were complete.
 */
internal data class PlayerSearchResults(
    val players: List<PlayerSearchResult>,
    val reachedLimit: Boolean,
) {
    internal companion object {
        internal val Empty: PlayerSearchResults =
            PlayerSearchResults(players = emptyList(), reachedLimit = false)
    }
}
