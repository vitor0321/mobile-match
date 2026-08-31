package com.walcker.games.features.ui.creatematch

import com.walcker.games.features.domain.model.Sport

internal data class CreateMatchState(
    val venueName: String = "",
    val venueNameError: String? = null,
    val selectedSport: Sport? = null,
    val sportError: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val neighborhood: String = "",
    val city: String = "",
    val address: String = "",
    val isResolvingLocation: Boolean = false,
    val selectedDate: Long? = null,
    val dateError: String? = null,
    val selectedTime: Pair<Int, Int>? = null,
    val timeError: String? = null,
    val durationMin: Int = 90,
    val totalPlayers: Int = 10,
    val playersError: String? = null,
    val pricePerPlayer: String = "",
    val priceError: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    val isFormValid: Boolean
        get() = venueName.isNotBlank() &&
                selectedSport != null &&
                lat != null &&
                lng != null &&
                selectedDate != null &&
                selectedTime != null &&
                totalPlayers >= 2 &&
                totalPlayers <= 20 &&
                (pricePerPlayer.isEmpty() || pricePerPlayer.toDoubleOrNull() != null)

    companion object {
        const val DEFAULT_DURATION_MIN = 90
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 20
        val AVAILABLE_DURATIONS = listOf(60, 90, 120)
    }
}

internal sealed class CreateMatchEvents {
    data class VenueNameChanged(val name: String) : CreateMatchEvents()
    data class SportSelected(val sport: Sport?) : CreateMatchEvents()
    data class LocationSelected(val lat: Double, val lng: Double) : CreateMatchEvents()
    data class DateSelected(val dateMillis: Long) : CreateMatchEvents()
    data class TimeSelected(val hour: Int, val minute: Int) : CreateMatchEvents()
    data class DurationSelected(val durationMin: Int) : CreateMatchEvents()
    data class PlayersChanged(val totalPlayers: Int) : CreateMatchEvents()
    data class PriceChanged(val price: String) : CreateMatchEvents()
    object Submit : CreateMatchEvents()
}

internal sealed class CreateMatchEffect {
    data class ShowMessage(val message: String) : CreateMatchEffect()
    data class NavigateToMyMatches(val matchId: String) : CreateMatchEffect()
    object RequireLogin : CreateMatchEffect()
}
