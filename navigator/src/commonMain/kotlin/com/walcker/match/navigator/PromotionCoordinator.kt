package com.walcker.match.navigator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

public data class PromotionNotice(
    val matchId: String,
    val matchTitle: String,
    val promotedAtMs: Long,
)

public class PromotionCoordinator {
    private val channel = Channel<PromotionNotice>(Channel.BUFFERED)
    public val promotions: Flow<PromotionNotice> = channel.receiveAsFlow()

    public fun emit(notice: PromotionNotice) {
        channel.trySend(notice)
    }
}
