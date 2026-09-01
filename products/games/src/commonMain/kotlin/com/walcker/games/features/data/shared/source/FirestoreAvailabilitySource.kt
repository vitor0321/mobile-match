package com.walcker.games.features.data.shared.source

import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class FirestoreAvailabilitySource(
    private val firestore: FirestoreClient,
) : AvailabilitySource {
    override fun observe(userId: String): Flow<Result<Availability>> =
        firestore
            .document(privatePath(userId))
            .snapshots()
            .map { result -> result.map { snapshot -> snapshot.toAvailability() } }

    override suspend fun setAvailable(
        userId: String,
        availability: Availability,
    ): Result<Unit> =
        firestore.document(privatePath(userId)).update(
            mapOf(
                FIELD_IS_AVAILABLE to availability.isAvailable,
                FIELD_AVAILABLE_UNTIL to availability.availableUntilMs,
            ),
        )

    private fun DocumentSnapshot?.toAvailability(): Availability {
        if (this == null) return Availability.Unavailable

        return Availability(
            isAvailable = getBoolean(FIELD_IS_AVAILABLE) ?: false,
            availableUntilMs = getTimestamp(FIELD_AVAILABLE_UNTIL),
        )
    }

    private companion object {
        fun privatePath(userId: String) = "profiles/$userId/private/data"

        const val FIELD_IS_AVAILABLE = "isAvailable"
        const val FIELD_AVAILABLE_UNTIL = "availableUntil"
    }
}
