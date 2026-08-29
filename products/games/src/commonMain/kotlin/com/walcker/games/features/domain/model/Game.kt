package com.walcker.games.features.domain.model

internal data class Game(
    val id: String,
    val sport: Sport,
    val venueName: String,
    val neighborhood: String,
    val city: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val geohash: String,
    val startsAtSeconds: Long,
    val durationMin: Int,
    val confirmedPlayers: Int,
    val totalPlayers: Int,
    val pricePerPlayer: String?,
    val organizerName: String,
    val organizerId: String,
    val organizerRating: Double,
    val status: MatchStatus = MatchStatus.OPEN,
    val participants: List<String> = emptyList(),
) {
    val openSlots: Int
        get() = (totalPlayers - confirmedPlayers).coerceAtLeast(0)

    val hasOpenSlots: Boolean
        get() = openSlots > 0

    val endsAtSeconds: Long
        get() = startsAtSeconds + durationMin.coerceAtLeast(0).toLong() * 60

    fun isOver(nowSeconds: Long): Boolean = endsAtSeconds <= nowSeconds
}

internal fun Game.canBeRatedBy(userId: String?, nowSeconds: Long): Boolean =
    isOver(nowSeconds) &&
        status != MatchStatus.CANCELLED &&
        userId != null &&
        userId in participants

internal enum class MatchRole {
    ORGANIZER,
    PARTICIPANT,
}

internal enum class MatchStatus {
    OPEN,
    FULL,
    CANCELLED,
    FINISHED,
}
