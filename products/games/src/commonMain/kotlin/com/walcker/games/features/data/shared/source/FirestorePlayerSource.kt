package com.walcker.games.features.data.shared.source

import com.walcker.games.features.domain.shared.model.DimensionAverage
import com.walcker.games.features.domain.shared.model.PROFILE_FIELD_IS_BANNED
import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.RatingDimension
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient

internal class FirestorePlayerSource(
    private val firestore: FirestoreClient,
    private val ratingSource: RatingSource,
) : PlayerSource {
    override suspend fun searchPlayers(
        filters: PlayerSearchFilters,
        limit: Int,
    ): Result<PlayerSearchPageDto> =
        runCatching {
            val sort = filters.sortBy

            val snapshots =
                firestore
                    .collection(PROFILES_COLLECTION)
                    .query()
                    .where(PROFILE_FIELD_IS_BANNED, "==", false)
                    .orderBy(sort.field, if (sort.descending) DESCENDING else ASCENDING)
                    .limit(limit)
                    .get()
                    .getOrThrow()

            PlayerSearchPageDto(
                players =
                    snapshots.mapNotNull { snapshot ->
                        snapshot.toPlayerSearchResultDto()?.takeIf { it.matches(filters) }
                    },
                reachedLimit = snapshots.size >= limit,
            )
        }

    override suspend fun getPlayerDetails(userId: String): Result<PlayerDetailsDto> =
        runCatching {
            val snapshot =
                firestore
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

    private fun DocumentSnapshot.readDimensionAverages(): Map<RatingDimension, DimensionAverage> {
        val count = (getLong(FIELD_RATING_COUNT) ?: 0L).toInt()
        if (count <= 0) return emptyMap()

        return RatingDimension.entries
            .mapNotNull { dimension ->
                val average = getDouble(dimension.averageField)?.toFloat() ?: return@mapNotNull null
                dimension to DimensionAverage(average = average, count = count)
            }.toMap()
    }

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

    private fun DocumentSnapshot.toPlayerSearchResultDto(): PlayerSearchResultDto? =
        try {
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

    private fun DocumentSnapshot.toPlayerDetailsDto(): PlayerDetailsDto? =
        try {
            PlayerDetailsDto(
                userId = id,
                fullName = getString(FIELD_FULL_NAME)?.takeIf { it.isNotBlank() } ?: return null,
                avatarUrl = getString(FIELD_AVATAR_URL),
                rating = getDouble(FIELD_RATING)?.toFloat() ?: 0f,
                ratingCount = (getLong(FIELD_RATING_COUNT) ?: 0L).toInt(),
                sports = readSports(),
                city = getString(FIELD_CITY),
                neighborhood = getString(FIELD_NEIGHBORHOOD),
                createdAtMs = getTimestamp(FIELD_CREATED_AT) ?: 0L,
                dimensionAverages = readDimensionAverages(),
            )
        } catch (e: Exception) {
            null
        }

    private fun DocumentSnapshot.readSports(): List<String> = getList(FIELD_SPORTS)?.filterIsInstance<String>().orEmpty()

    private companion object {
        const val PROFILES_COLLECTION = "profiles"
        const val ASCENDING = "asc"
        const val DESCENDING = "desc"

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
