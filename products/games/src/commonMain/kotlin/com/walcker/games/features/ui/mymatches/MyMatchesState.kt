package com.walcker.games.features.ui.mymatches

import com.walcker.games.features.domain.repository.MyMatch

internal enum class MyMatchesTab { ACTIVE, PAST }

internal data class MyMatchesState(
    val activeTab: MyMatchesTab = MyMatchesTab.ACTIVE,
    val isLoading: Boolean = false,
    val active: List<MyMatch> = emptyList(),
    val past: List<MyMatch> = emptyList(),
    val errorMessage: String? = null,
)

internal sealed interface MyMatchesEvent {
    data class TabSelected(val tab: MyMatchesTab) : MyMatchesEvent
    data object Refresh : MyMatchesEvent
    data object DismissError : MyMatchesEvent
    data class CancelRequested(val gameId: String) : MyMatchesEvent
    data class LeaveRequested(val gameId: String) : MyMatchesEvent
}

internal sealed interface MyMatchesEffect {
    data class ShowMessage(val message: String) : MyMatchesEffect
}
