package com.walcker.games.features.domain.shared.model

internal data class Availability(
    val isAvailable: Boolean = false,
    val availableUntilMs: Long? = null,
) {
    fun isActiveAt(nowMs: Long): Boolean = isAvailable && (availableUntilMs == null || availableUntilMs > nowMs)

    companion object {
        val Unavailable = Availability()
    }
}
