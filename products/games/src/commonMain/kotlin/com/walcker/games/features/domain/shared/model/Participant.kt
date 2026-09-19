package com.walcker.games.features.domain.shared.model

internal data class Participant(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val joinedAt: Long,
    val isConfirmed: Boolean,
    val positionInWaitlist: Int? = null,
    val hasPaid: Boolean = false,
    val isVip: Boolean = false,
)

internal data class ParticipantsSummary(
    val confirmed: List<Participant>,
    val waitlist: List<Participant>,
    val confirmedCount: Int,
) {
    val waitlistCount: Int get() = waitlist.size
}
