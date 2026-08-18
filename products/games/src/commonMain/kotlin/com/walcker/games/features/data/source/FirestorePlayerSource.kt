package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.PROFILE_FIELD_IS_BANNED
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient

/**
 * Firestore implementation of [PlayerSource]. Reads `profiles/{uid}`.
 *
 * Only the sort field and the ban check run server-side. Name, rating range and
 * sport are matched here, on the [PlayerSource.DEFAULT_SEARCH_LIMIT] documents
 * the query returned: Firestore has no case-insensitive contains, and every
 * extra inequality would need its own composite index. The consequence is
 * honest but real — a search can miss someone ranked below the cap, which is
 * why the UI surfaces "narrow your search" when the cap is reached.
 *
 * Delegates rating queries to [RatingSource].
 */
internal class FirestorePlayerSource(
    private val firestore: FirestoreClient,
    private val ratingSource: RatingSource,
) : PlayerSource {

    override suspend fun searchPlayers(
        filters: PlayerSearchFilters,
        limit: Int,
    ): Result<PlayerSearchPageDto> = runCatching {
        val sort = filters.sortBy

        val snapshots = firestore
            .collection(PROFILES_COLLECTION)
            .query()
            // Banned players never surface in search.
            .where(PROFILE_FIELD_IS_BANNED, "==", false)
            .orderBy(sort.field, if (sort.descending) DESCENDING else ASCENDING)
            .limit(limit)
            .get()
            .getOrThrow()

        PlayerSearchPageDto(
            players = snapshots.mapNotNull { snapshot ->
                snapshot.toPlayerSearchResultDto()?.takeIf { it.matches(filters) }
            },
            // A full page means Firestore had more to give.
            reachedLimit = snapshots.size >= limit,
        )
    }

    override suspend fun getPlayerDetails(userId: String): Result<PlayerDetailsDto> =
        runCatching {
            val snapshot = firestore
                .document("$PROFILES_COLLECTION/$userId")
                .get()
                .getOrThrow()
                ?: throw NoSuchElementException("Player $userId not found.")

            snapshot.toPlayerDetailsDto()
                ?: throw NoSuchElementException("Player $userId has no name on the profile.")
        }

    override suspend fun getPlayerRatings(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> =
        ratingSource.getUserRatingsPage(
            userId = userId,
            limit = limit,
            sort = sort,
            cursor = cursor,
        )

    /** Client-side leg of the filtering — see the class docs for why. */
    private fun PlayerSearchResultDto.matches(filters: PlayerSearchFilters): Boolean {
        val query = filters.query.trim()
        if (query.isNotEmpty() && !fullName.contains(query, ignoreCase = true)) return false

        filters.minRating?.let { if (rating < it) return false }
        filters.maxRating?.let { if (rating > it) return false }

        if (filters.favoriteSports.isNotEmpty()) {
            val wanted = filters.favoriteSports.map { it.name }.toSet()
            if (sports.none { it in wanted }) return false
        }

        return true
    }

    /** A profile with no name is unusable in a list — skip it rather than render a blank row. */
    private fun DocumentSnapshot.toPlayerSearchResultDto(): PlayerSearchResultDto? = try {
        PlayerSearchResultDto(
            userId = id,
            fullName = getString(FIELD_FULL_NAME)?.takeIf { it.isNotBlank() } ?: return null,
            avatarUrl = getString(FIELD_AVATAR_URL),
            rating = getDouble(FIELD_RATING)?.toFloat() ?: 0f,
            ratingCount = (getLong(FIELD_RATING_COUNT) ?: 0L).toInt(),
            sports = readSports(),
        )
    } catch (e: Exception) {
        null
    }

    private fun DocumentSnapshot.toPlayerDetailsDto(): PlayerDetailsDto? = try {
        PlayerDetailsDto(
            userId = id,
            fullName = getString(FIELD_FULL_NAME)?.takeIf { it.isNotBlank() } ?: return null,
            avatarUrl = getString(FIELD_AVATAR_URL),
            rating = getDouble(FIELD_RATING)?.toFloat() ?: 0f,
            ratingCount = (getLong(FIELD_RATING_COUNT) ?: 0L).toInt(),
            sports = readSports(),
            city = getString(FIELD_CITY),
            neighborhood = getString(FIELD_NEIGHBORHOOD),
            // createdAt is a Firestore Timestamp; getTimestamp unwraps it to millis.
            createdAtMs = getTimestamp(FIELD_CREATED_AT) ?: 0L,
        )
    } catch (e: Exception) {
        null
    }

    private fun DocumentSnapshot.readSports(): List<String> =
        getList(FIELD_SPORTS)?.filterIsInstance<String>().orEmpty()

    private companion object {
        const val PROFILES_COLLECTION = "profiles"
        const val ASCENDING = "asc"
        const val DESCENDING = "desc"

        // Field names of profiles/{uid} — see onUserCreate in functions/src/index.ts.
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_AVATAR_URL = "avatarUrl"
        const val FIELD_RATING = "rating"
        const val FIELD_RATING_COUNT = "ratingCount"
        const val FIELD_SPORTS = "sports"
        const val FIELD_CITY = "city"
        const val FIELD_NEIGHBORHOOD = "neighborhood"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
