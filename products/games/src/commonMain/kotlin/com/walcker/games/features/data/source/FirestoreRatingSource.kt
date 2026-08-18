package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.model.RATING_FIELD_CREATED_AT_MS
import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient
import com.walcker.match.firestore.FirestoreQueryBuilder

/**
 * Firestore implementation of [RatingSource].
 *
 * Ratings are stored in: users/{userId}/ratings subcollection
 * Each rating doc has: matchId, ratedUserId, raterUserId, rating, comment, createdAtMs
 *
 * `createdAtMs` is a plain number (epoch millis), not a Firestore `Timestamp`:
 * numbers survive the Android/iOS interop boundary unchanged and can be used
 * directly as a `startAfter` cursor. Documents written before that convention
 * are still read through the legacy `createdAt` timestamp field.
 */
internal class FirestoreRatingSource(
    private val firestore: FirestoreClient,
) : RatingSource {

    override suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
    ): Result<Unit> = runCatching {
        firestore.callFunction(
            "submitPlayerRating",
            mapOf(
                "matchId" to matchId,
                "ratedUserId" to ratedUserId,
                "rating" to rating,
                "comment" to comment,
            ),
        )
        Unit
    }

    override suspend fun getUserRatings(userId: String, limit: Int): Result<List<Rating>> =
        getUserRatingsPage(userId = userId, limit = limit).map { it.ratings }

    override suspend fun getUserRatingsPage(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> = runCatching {
        val ratings = firestore
            .collection("users/$userId/ratings")
            .query()
            .applySort(sort)
            .applyCursor(cursor, sort)
            .limit(limit)
            .get()
            .getOrThrow()
            .mapNotNull { snapshot -> snapshot.toRating() }

        RatingsPage(
            ratings = ratings,
            // A short page means Firestore had nothing else to give: stop paging.
            nextCursor = if (ratings.size < limit) {
                null
            } else {
                RatingCursor.encode(ratings.last(), sort)
            },
        )
    }

    override suspend fun getMatchLocationRatings(matchId: String, limit: Int): Result<List<Rating>> =
        runCatching {
            firestore
                .collection("matches/$matchId/locationRatings")
                .query()
                .orderBy(RATING_FIELD_CREATED_AT_MS, DESCENDING)
                .limit(limit)
                .get()
                .getOrThrow()
                .mapNotNull { snapshot -> snapshot.toRating() }
        }

    /**
     * `createdAtMs` is always the last `orderBy` so every ordering is total —
     * without it two ratings with the same star count could swap places between
     * pages and be shown twice (or skipped).
     */
    private fun FirestoreQueryBuilder.applySort(sort: RatingSort): FirestoreQueryBuilder {
        val primary = orderBy(sort.primaryField, if (sort.descending) DESCENDING else ASCENDING)
        return if (sort.primaryField == RATING_FIELD_CREATED_AT_MS) {
            primary
        } else {
            primary.orderBy(RATING_FIELD_CREATED_AT_MS, DESCENDING)
        }
    }

    private fun FirestoreQueryBuilder.applyCursor(
        cursor: String?,
        sort: RatingSort,
    ): FirestoreQueryBuilder {
        val values = RatingCursor.decode(cursor, sort)
        return if (values.isEmpty()) this else startAfter(*values.toTypedArray())
    }

    private fun DocumentSnapshot.toRating(): Rating? = try {
        Rating(
            id = id,
            matchId = getString("matchId") ?: return null,
            ratedUserId = getString("ratedUserId") ?: return null,
            raterUserId = getString("raterUserId") ?: return null,
            rating = getLong("rating")?.toInt() ?: return null,
            comment = getString("comment") ?: "",
            createdAtMs = getLong(RATING_FIELD_CREATED_AT_MS)
                ?: getTimestamp(LEGACY_CREATED_AT_FIELD)
                ?: 0L,
        )
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val ASCENDING = "asc"
        const val DESCENDING = "desc"

        /** Pre-`createdAtMs` documents stored a Firestore `Timestamp` here. */
        const val LEGACY_CREATED_AT_FIELD = "createdAt"
    }
}
