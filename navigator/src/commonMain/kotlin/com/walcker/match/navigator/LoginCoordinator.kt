package com.walcker.match.navigator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

public class LoginCoordinator {
    private val channel = Channel<Unit>(Channel.BUFFERED)
    public val requests: Flow<Unit> = channel.receiveAsFlow()

    public fun requestLogin() {
        channel.trySend(Unit)
    }
}
