package com.walcker.games.features.ui.home

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.strings.GameListStrings
import com.walcker.games.strings.PtBrGamesStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class GameListState(
    val strings: GameListStrings = PtBrGamesStrings.gameList,
    val isLoading: Boolean = true,
    val games: ImmutableList<Game> = persistentListOf(),
    val errorMessage: String? = null,
    val selectedSport: Sport? = null,
    val radiusKm: Double = DEFAULT_RADIUS_KM,
    val preferencesLoaded: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val mySports: Set<Sport> = emptySet(),
    val isShowingNearestFallback: Boolean = false,
) {
    companion object {
        const val DEFAULT_RADIUS_KM: Double = 15.0
        const val MIN_RADIUS_KM: Double = 5.0
        const val MAX_RADIUS_KM: Double = 50.0
    }
}
