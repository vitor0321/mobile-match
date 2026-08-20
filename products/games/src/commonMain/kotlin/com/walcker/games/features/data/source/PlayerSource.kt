package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.DimensionAverage
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.RatingDimension
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage

/**
 * Data source interface for player-related remote operations.
 *
 * Implemented by [FirestorePlayerSource] for Android and iOS.
 */
internal interface PlayerSource {
    /**
     * Search for players matching the given filters.
     *
     * @param filters Search and filter criteria (name, rating, sports)
     * @param limit Hard cap on documents read from Firestore
     * @return Raw DTOs from Firestore to be mapped by the repository
     */
    suspend fun searchPlayers(
        filters: PlayerSearchFilters,
        limit: Int = DEFAULT_SEARCH_LIMIT,
    ): Result<PlayerSearchPageDto>

    /**
     * Fetch detailed player profile from Firestore.
     *
     * @param userId Player ID
     * @return Raw DTO to be mapped by the repository
     */
    suspend fun getPlayerDetails(userId: String): Result<PlayerDetailsDto>

    /**
     * Fetch a page of ratings received by a player.
     *
     * @param userId Player being rated
     * @param limit Page size
     * @param sort Server-side ordering
     * @param cursor Opaque cursor from the previous page; `null` for the first
     * @return A page of ratings plus the cursor for the next one
     */
    suspend fun getPlayerRatings(
        userId: String,
        limit: Int = 20,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>

    companion object {
        /**
         * How many profiles a single search reads.
         *
         * Rating range and sport are still filtered client-side, so this is a
         * ceiling on documents read, not on results shown. The UI tells the
         * user to narrow the search when the cap is hit — see
         * [com.walcker.games.features.ui.player_search.PlayerSearchState].
         */
        const val DEFAULT_SEARCH_LIMIT: Int = 50
    }
}

/**
 * One search response: the matches plus whether the read cap was reached.
 *
 * @param reachedLimit the query returned as many documents as it asked for, so
 *        matching players may exist beyond them
 */
internal data class PlayerSearchPageDto(
    val players: List<PlayerSearchResultDto>,
    val reachedLimit: Boolean,
)

/**
 * DTO for a player search result.
 *
 * Field names mirror `profiles/{uid}` exactly — `fullName`, `avatarUrl` — which
 * is what `onUserCreate` writes and what `profileEditableFields()` in
 * firestore.rules allows the owner to edit.
 */
internal data class PlayerSearchResultDto(
    val userId: String,
    val fullName: String,
    val avatarUrl: String?,
    val rating: Float,
    val ratingCount: Int,
    val sports: List<String>, // Sport enum names
)

/**
 * DTO for a detailed player profile.
 */
internal data class PlayerDetailsDto(
    val userId: String,
    val fullName: String,
    val avatarUrl: String?,
    val rating: Float,
    val ratingCount: Int,
    val sports: List<String>, // Sport enum names
    val city: String?,
    val neighborhood: String?,
    val createdAtMs: Long,
    /**
     * Médias por dimensão lidas de `profiles/{uid}`. Vem vazio para perfil sem
     * avaliação e para perfil avaliado antes das dimensões existirem — nesse
     * caso os campos simplesmente não estão no documento.
     */
    val dimensionAverages: Map<RatingDimension, DimensionAverage> = emptyMap(),
)
