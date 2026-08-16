package com.walcker.match.navigator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Tab index identifiers used by the bottom-bar navigation shell.
 *
 * Kept here so every product can refer to tabs without hardcoding indices,
 * which makes adding or reordering tabs a one-line change.
 */
public enum class MainTab(val index: Int) {
    Home(0),
    Search(1),
    Create(2),
    MyMatches(3),
    PlayerProfile(4),
}

/**
 * Tiny coordinator that lets one Screen ask the navigation shell to switch
 * to another tab. The shell owns the selected-tab state; producers just emit
 * a [MainTab] through [requestTab].
 *
 * Buffered so emitting from a ScreenModel does not block if the shell has
 * not started collecting yet.
 */
public class TabCoordinator {
    private val channel = Channel<MainTab>(Channel.BUFFERED)
    public val tabs: Flow<MainTab> = channel.receiveAsFlow()

    public fun requestTab(tab: MainTab) {
        channel.trySend(tab)
    }
}
