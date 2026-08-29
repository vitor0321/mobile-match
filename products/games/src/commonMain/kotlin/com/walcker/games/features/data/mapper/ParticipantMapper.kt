package com.walcker.games.features.data.mapper

import com.walcker.games.features.domain.model.Participant
import com.walcker.match.firestore.DocumentSnapshot

internal fun DocumentSnapshot.toParticipant(): Participant? {
    return try {
        val userId = getString("userId") ?: id
        val displayName = getString("displayName") ?: "Jogador"
        val joinedAt = getLong("joinedAt") ?: 0L
        val isConfirmed = getBoolean("isConfirmed") ?: true

        Participant(
            userId = userId,
            displayName = displayName,
            photoUrl = getString("photoUrl"),
            joinedAt = joinedAt,
            isConfirmed = isConfirmed,
            positionInWaitlist = (getLong("positionInWaitlist")?.toInt()),
            hasPaid = getBoolean("hasPaid") ?: false,
        )
    } catch (e: Exception) {
        null
    }
}
