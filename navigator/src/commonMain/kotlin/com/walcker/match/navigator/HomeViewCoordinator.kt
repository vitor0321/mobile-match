package com.walcker.match.navigator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class HomeViewCoordinator {
    private val _showMap = MutableStateFlow(false)
    public val showMap: StateFlow<Boolean> = _showMap.asStateFlow()

    private val _isHomeDataReady = MutableStateFlow(false)
    public val isHomeDataReady: StateFlow<Boolean> = _isHomeDataReady.asStateFlow()

    public fun setShowMap(value: Boolean) {
        _showMap.value = value
    }

    public fun markHomeDataReady() {
        _isHomeDataReady.value = true
    }
}
