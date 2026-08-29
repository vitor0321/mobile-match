package com.walcker.match.navigator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

public enum class MainTab(val index: Int) {
    Home(0),
    Search(1),
    Create(2),
    MyMatches(3),
    PlayerProfile(4),
}

public class TabCoordinator {
    private val channel = Channel<MainTab>(Channel.BUFFERED)
    public val tabs: Flow<MainTab> = channel.receiveAsFlow()

    public fun requestTab(tab: MainTab) {
        channel.trySend(tab)
    }
}
