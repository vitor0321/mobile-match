package com.walcker.games.features.ui.playerProfile

import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.repository.MyMatch

internal data class PlayerProfileState(
    val isLoading: Boolean = false,
    val userId: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val matchesOrganized: Int = 0,
    val matchesParticipated: Int = 0,
    val nextMatch: MyMatch? = null,
    val ratings: List<Rating> = emptyList(),
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val errorMessage: String? = null,
    val isAvailable: Boolean = false,
    val isUpdatingAvailability: Boolean = false,
    val availabilityErrorMessage: String? = null,
    val availableUntilMs: Long? = null,
    val availableSports: Set<Sport> = emptySet(),
    val sportsErrorMessage: String? = null,
)

internal sealed interface PlayerProfileEvent {
    data object Refresh : PlayerProfileEvent

    data object DismissError : PlayerProfileEvent

    data object LogoutRequested : PlayerProfileEvent

    data class AvailabilityChanged(
        val isAvailable: Boolean,
    ) : PlayerProfileEvent

    data object DismissAvailabilityError : PlayerProfileEvent

    data class AvailableUntilTonightToggled(
        val enabled: Boolean,
    ) : PlayerProfileEvent

    data class SportToggled(
        val sport: Sport,
    ) : PlayerProfileEvent

    data object DismissSportsError : PlayerProfileEvent

    data class NextMatchClicked(
        val matchId: String,
    ) : PlayerProfileEvent

    data object ViewPublicProfileClicked : PlayerProfileEvent
}

internal sealed interface PlayerProfileEffect {
    data object RequireLogin : PlayerProfileEffect

    data class NavigateToMatchDetail(
        val matchId: String,
    ) : PlayerProfileEffect

    data class NavigateToOwnPublicProfile(
        val userId: String,
    ) : PlayerProfileEffect
}
