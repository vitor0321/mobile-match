package com.walcker.games.features.ui.playerprofile

internal data class PlayerProfileState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val matchesOrganized: Int = 0,
    val matchesParticipated: Int = 0,
    val errorMessage: String? = null,
)

internal sealed interface PlayerProfileEvent {
    data object Refresh : PlayerProfileEvent
    data object DismissError : PlayerProfileEvent
    data object LogoutRequested : PlayerProfileEvent
}
