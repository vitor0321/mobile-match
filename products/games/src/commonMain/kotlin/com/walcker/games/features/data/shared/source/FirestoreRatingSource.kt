package com.walcker.games.features.data.shared.source

import com.walcker.games.features.domain.shared.model.RATING_FIELD_CREATED_AT_MS
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.RatingDimension
import com.walcker.games.features.domain.shared.model.RatingDimensions
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage
import com.walcker.games.features.domain.shared.model.SubmitRatingOutcome
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient
import com.walcker.match.firestore.FirestoreQueryBuilder

internal class FirestoreRatingSource(
    private val firestore: FirestoreClient,
) : RatingSource {
    override suspend fun submitPlayerRating(
        matchId: String,
        ratedUserId: String,
        rating: Int,
        comment: String,
        dimensions: RatingDimensions,
    ): Result<SubmitRatingOutcome> =
        firestore
            .callFunction(
                SUBMIT_RATING_FUNCTION,
                buildMap {
                    put("matchId", matchId)
                    put("ratedUserId", ratedUserId)
                    put("rating", rating)
                    put("comment", comment)
                    dimensions.answers.forEach { (dimension, stars) ->
                        put(dimension.wireName, stars)
                    }
                },
            ).mapCatching { payload -> payload.toSubmitRatingOutcome() }

    private fun Map<String, Any?>.toSubmitRatingOutcome(): SubmitRatingOutcome {
        val averageRating = (this["averageRating"] as? Number)?.toFloat() ?: 0f
        val ratingCount = (this["ratingCount"] as? Number)?.toInt() ?: 0

        return when (val status = this["status"]) {
            "recorded" -> SubmitRatingOutcome.Recorded(averageRating, ratingCount)
            "already_rated" -> SubmitRatingOutcome.AlreadyRated(averageRating, ratingCount)
            else -> throw IllegalStateException(
                "Unexpected submitPlayerRating response status: $status",
            )
        }
    }

    override suspend fun getUserRatings(
        userId: String,
        limit: Int,
    ): Result<List<Rating>> = getUserRatingsPage(userId = userId, limit = limit).map { it.ratings }

    override suspend fun getUserRatingsPage(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> =
        runCatching {
            val ratings =
                firestore
                    .collection("profiles/$userId/ratings")
                    .query()
                    .applySort(sort)
                    .applyCursor(cursor, sort)
                    .limit(limit)
                    .get()
                    .getOrThrow()
                    .mapNotNull { snapshot -> snapshot.toRating() }

            RatingsPage(
                ratings = ratings,
                nextCursor =
                    if (ratings.size < limit) {
                        null
                    } else {
                        RatingCursor.encode(ratings.last(), sort)
                    },
            )
        }

    override suspend fun getMatchLocationRatings(
        matchId: String,
        limit: Int,
    ): Result<List<Rating>> =
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

    private fun DocumentSnapshot.toRating(): Rating? =
        try {
            Rating(
                id = id,
                matchId = getString("matchId") ?: return null,
                ratedUserId = getString("ratedUserId") ?: return null,
                raterUserId = getString("raterUserId") ?: return null,
                rating = getLong("rating")?.toInt() ?: return null,
                comment = getString("comment") ?: "",
                createdAtMs =
                    getLong(RATING_FIELD_CREATED_AT_MS)
                        ?: getTimestamp(LEGACY_CREATED_AT_FIELD)
                        ?: 0L,
                dimensions = readDimensions(),
            )
        } catch (e: Exception) {
            null
        }

    private fun DocumentSnapshot.readDimensions(): RatingDimensions =
        RatingDimensions(
            answers =
                RatingDimension.entries
                    .mapNotNull { dimension ->
                        val stars = getLong(dimension.wireName)?.toInt()
                        if (stars != null && stars in RatingDimensions.VALID_RANGE) dimension to stars else null
                    }.toMap(),
        )

    private companion object {
        const val SUBMIT_RATING_FUNCTION = "submitPlayerRating"
        const val ASCENDING = "asc"
        const val DESCENDING = "desc"

        const val LEGACY_CREATED_AT_FIELD = "createdAt"
    }
}
