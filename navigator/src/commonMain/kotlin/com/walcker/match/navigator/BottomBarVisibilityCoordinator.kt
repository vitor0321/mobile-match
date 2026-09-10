package com.walcker.match.navigator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class BottomBarVisibilityCoordinator {
    private val _isVisible = MutableStateFlow(true)
    public val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    public fun setVisible(value: Boolean) {
        _isVisible.value = value
    }
}
