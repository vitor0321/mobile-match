package com.walcker.games.features.domain.shared.model

internal data class NearbyMatchesPage(
    val games: List<Game>,
    val rangeCursors: List<String?>,
) {
    val hasMore: Boolean get() = rangeCursors.any { it != null }
}
