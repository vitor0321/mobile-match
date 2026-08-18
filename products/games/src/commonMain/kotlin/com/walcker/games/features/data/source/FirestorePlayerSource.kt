package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSortBy
import com.walcker.games.features.domain.model.Rating
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient

/**
 * Firestore implementation of [PlayerSource].
 *
 * Queries the `profiles/{uid}` collection for player search and details.
 * Delegates to [RatingSource] for rating queries.
 *
 * Note: For MVP, search is name-only. Sport/stats filtering happens client-side
 * due to Firestore single-field query limitations. This can be optimized in Phase 6
 * by adding denormalized indexes or a separate search index.
 */
internal class FirestorePlayerSource(
    private val firestore: FirestoreClient,
    private val ratingSource: RatingSource,
) : PlayerSource {

    override suspend fun searchPlayers(filters: PlayerSearchFilters): Result<List<PlayerSearchResultDto>> =
        runCatching {
            // Base query: order by sort preference
            val baseQuery = when (filters.sortBy) {
                PlayerSortBy.RATING -> firestore
                    .collection("profiles")
                    .query()
                    .orderBy("rating", "desc")

                PlayerSortBy.NAME -> firestore
                    .collection("profiles")
                    .query()
                    .orderBy("displayName", "asc")

                PlayerSortBy.RECENT_ACTIVITY -> firestore
                    .collection("profiles")
                    .query()
                    .orderBy("lastActivitySeconds", "desc")

                PlayerSortBy.MATCHES_PLAYED -> firestore
                    .collection("profiles")
                    .query()
                    .orderBy("totalMatches", "desc") // matchesOrganized + matchesParticipated
            }

            // Execute query with limit
            val snapshots = baseQuery
                .limit(50)
                .get()
                .getOrThrow()

            // Map to DTOs and apply client-side filters
            snapshots.mapNotNull { snapshot ->
                snapshot.toPlayerSearchResultDto()?.let { dto ->
                    // Client-side filtering: rating range, sports, match counts
                    if (matchesFilters(dto, filters)) dto else null
                }
            }
        }

    override suspend fun getPlayerDetails(userId: String): Result<PlayerDetailsDto> =
        runCatching {
            val snapshot = firestore
                .document("profiles/$userId")
                .get()
                .getOrThrow()
                ?: throw Exception("Player document not found")

            snapshot.toPlayerDetailsDto()
                ?: throw Exception("Player data incomplete")
        }

    override suspend fun getPlayerRatings(
        userId: String,
        limit: Int,
        cursor: String?,
    ): Result<List<Rating>> =
        ratingSource.getUserRatings(userId, limit)

    /**
     * Client-side filter: check if DTO matches all active filters.
     *
     * TODO: Move to server-side queries once composite indexes are available.
     */
    private fun matchesFilters(dto: PlayerSearchResultDto, filters: PlayerSearchFilters): Boolean {
        // Name filter (server-side would be better but complex case-insensitive match)
        if (filters.query.isNotBlank()) {
            if (!dto.displayName.contains(filters.query, ignoreCase = true)) {
                return false
            }
        }

        // Rating filters
        if (filters.minRating != null && dto.rating < filters.minRating) {
            return false
        }
        if (filters.maxRating != null && dto.rating > filters.maxRating) {
            return false
        }

        // Sports filter
        if (filters.favoriteSports.isNotEmpty()) {
            val dtoSports = dto.sports.mapNotNull { sportName ->
                try {
                    com.walcker.games.features.domain.model.Sport.valueOf(sportName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            if (dtoSports.intersect(filters.favoriteSports).isEmpty()) {
                return false
            }
        }

        // Match count filters
        if (filters.minMatchesOrganized != null && dto.matchesOrganized < filters.minMatchesOrganized) {
            return false
        }
        if (filters.minMatchesParticipated != null && dto.matchesParticipated < filters.minMatchesParticipated) {
            return false
        }

        return true
    }

    /**
     * Map Firestore document to PlayerSearchResultDto.
     *
     * Returns null if document is missing required fields.
     */
    private fun DocumentSnapshot.toPlayerSearchResultDto(): PlayerSearchResultDto? = try {
        PlayerSearchResultDto(
            userId = id,
            displayName = getString("displayName") ?: return null,
            photoUrl = getString("photoUrl"),
            rating = getDouble("rating")?.toFloat() ?: 0f,
            ratingCount = (getLong("ratingCount") ?: 0).toInt(),
            sports = (getList("sports") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            matchesOrganized = (getLong("matchesOrganized") ?: 0).toInt(),
            matchesParticipated = (getLong("matchesParticipated") ?: 0).toInt(),
        )
    } catch (e: Exception) {
        null
    }

    /**
     * Map Firestore document to PlayerDetailsDto.
     *
     * Returns null if document is missing required fields.
     */
    private fun DocumentSnapshot.toPlayerDetailsDto(): PlayerDetailsDto? = try {
        PlayerDetailsDto(
            userId = id,
            displayName = getString("displayName") ?: return null,
            photoUrl = getString("photoUrl"),
            email = getString("email") ?: "",
            bio = getString("bio"),
            rating = getDouble("rating")?.toFloat() ?: 0f,
            ratingCount = (getLong("ratingCount") ?: 0).toInt(),
            matchesOrganized = (getLong("matchesOrganized") ?: 0).toInt(),
            matchesParticipated = (getLong("matchesParticipated") ?: 0).toInt(),
            sports = (getList("sports") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            city = getString("city"),
            neighborhood = getString("neighborhood"),
            radiusKm = (getLong("radiusKm") ?: 15).toInt(),
            joinRate = getDouble("joinRate")?.toFloat() ?: 0f,
            cancelRate = getDouble("cancelRate")?.toFloat() ?: 0f,
            memberSinceSeconds = getLong("createdAtSeconds") ?: 0,
        )
    } catch (e: Exception) {
        null
    }
}
