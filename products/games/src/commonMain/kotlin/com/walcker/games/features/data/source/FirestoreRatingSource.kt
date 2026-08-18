package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Rating
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient

/**
 * Firestore implementation of [RatingSource].
 *
 * Ratings are stored in: users/{userId}/ratings subcollection
 * Each rating doc has: matchId, ratedUserId, raterUserId, rating, comment, createdAt
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
        runCatching {
            firestore
                .collection("users/$userId/ratings")
                .query()
                .orderBy("createdAt", "desc")
                .limit(limit)
                .get()
                .getOrThrow()
                .mapNotNull { snapshot ->
                    snapshot.toRating()
                }
        }

    override suspend fun getMatchLocationRatings(matchId: String, limit: Int): Result<List<Rating>> =
        runCatching {
            firestore
                .collection("matches/$matchId/locationRatings")
                .query()
                .orderBy("createdAt", "desc")
                .limit(limit)
                .get()
                .getOrThrow()
                .mapNotNull { snapshot ->
                    snapshot.toRating()
                }
        }

    private fun DocumentSnapshot.toRating(): Rating? {
        return try {
            Rating(
                id = id,
                matchId = getString("matchId") ?: return null,
                ratedUserId = getString("ratedUserId") ?: return null,
                raterUserId = getString("raterUserId") ?: return null,
                rating = getLong("rating")?.toInt() ?: return null,
                comment = getString("comment") ?: "",
                createdAtMs = getTimestamp("createdAt") ?: 0L,
            )
        } catch (e: Exception) {
            null
        }
    }
}
