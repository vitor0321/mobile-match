package com.walcker.games.features.ui.creatematch

import com.walcker.games.features.domain.model.Sport
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class CreateMatchState(
    // Form fields
    val venueName: String = "",
    val venueNameError: String? = null,
    val selectedSport: Sport? = null,
    val sportError: String? = null,
    val neighborhood: String = "",
    val neighborhoodError: String? = null,
    val city: String = "",
    val cityError: String? = null,
    val address: String = "",
    val addressError: String? = null,
    val selectedDate: Long? = null, // Unix millis
    val dateError: String? = null,
    val selectedTime: Pair<Int, Int>? = null, // (hour, minute)
    val timeError: String? = null,
    val durationMin: Int = 90,
    val totalPlayers: Int = 10,
    val playersError: String? = null,
    val pricePerPlayer: String = "",
    val priceError: String? = null,
    // UI state
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    val isFormValid: Boolean
        get() = venueName.isNotBlank() &&
                selectedSport != null &&
                neighborhood.isNotBlank() &&
                city.isNotBlank() &&
                address.isNotBlank() &&
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
    data class NeighborhoodChanged(val neighborhood: String) : CreateMatchEvents()
    data class CityChanged(val city: String) : CreateMatchEvents()
    data class AddressChanged(val address: String) : CreateMatchEvents()
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
}
