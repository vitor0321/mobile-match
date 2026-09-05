package com.walcker.match.navigator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class MatchDetailCoordinator {
    private val _selectedMatchId = MutableStateFlow<String?>(null)
    public val selectedMatchId: StateFlow<String?> = _selectedMatchId.asStateFlow()

    public fun open(matchId: String) {
        _selectedMatchId.value = matchId
    }

    public fun close() {
        _selectedMatchId.value = null
    }
}
