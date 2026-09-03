package com.walcker.games.features.domain.shared.model

internal data class Availability(
    val isAvailable: Boolean = false,
    val availableUntilMs: Long? = null,
    val sports: Set<Sport> = emptySet(),
) {
    fun isActiveAt(nowMs: Long): Boolean = isAvailable && (availableUntilMs == null || availableUntilMs > nowMs)

    companion object {
        val Unavailable = Availability()
    }
}
