package com.walcker.games.features.ui.player_details

internal sealed interface PlayerDetailsEvents {
    data object DismissError : PlayerDetailsEvents
    data object RetryLoading : PlayerDetailsEvents
    data object SeeAllRatingsClicked : PlayerDetailsEvents
}

internal sealed interface PlayerDetailsEffect {
    data class ShowMessage(val message: String) : PlayerDetailsEffect

    data class NavigateToRatings(
        val userId: String,
        val playerName: String,
    ) : PlayerDetailsEffect
}
