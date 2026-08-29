package com.walcker.games.features.domain.model

internal data class Participant(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val joinedAt: Long,
    val isConfirmed: Boolean,
    val positionInWaitlist: Int? = null,
    val hasPaid: Boolean = false,
)

internal data class ParticipantsSummary(
    val confirmed: List<Participant>,
    val waitlist: List<Participant>,
    val confirmedCount: Int,
    val totalSlots: Int,
) {
    val waitlistCount: Int get() = waitlist.size
    val hasOpenSlots: Boolean get() = confirmedCount < totalSlots
    val openSlots: Int get() = (totalSlots - confirmedCount).coerceAtLeast(0)
}
