package com.walcker.match.navigator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Sealed class representing deep link navigation events.
 * Emitted from anywhere in the app, captured by the navigation shell.
 */
public sealed class DeepLink {
    /**
     * Navigate to match detail screen.
     * @param matchId The ID of the match to display.
     */
    public data class OpenMatch(val matchId: String) : DeepLink()

    /**
     * More link types can be added here as needed:
     * - OpenUser(userId)
     * - OpenChat(chatId)
     * - etc.
     */
}

/**
 * Coordinator for deep link navigation events.
 * Similar to [TabCoordinator], but handles arbitrary navigation targets.
 *
 * Any composable can inject this, call [navigate], and the shell will
 * handle the actual screen transition.
 *
 * Buffered so emitting does not block if the shell has not started
 * collecting yet.
 */
public class DeepLinkCoordinator {
    private val channel = Channel<DeepLink>(Channel.BUFFERED)
    public val links: Flow<DeepLink> = channel.receiveAsFlow()

    public fun navigate(link: DeepLink) {
        channel.trySend(link)
    }
}
