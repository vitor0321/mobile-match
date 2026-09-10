package com.walcker.match.navigator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

public sealed class DeepLink {
    public data class OpenMatch(
        val matchId: String,
    ) : DeepLink()
}

public class DeepLinkCoordinator {
    private val channel = Channel<DeepLink>(Channel.BUFFERED)
    public val links: Flow<DeepLink> = channel.receiveAsFlow()

    public fun navigate(link: DeepLink) {
        val result = channel.trySend(link)
        println("DEEPLINK_DEBUG DeepLinkCoordinator.navigate link=$link result=$result")
    }
}
