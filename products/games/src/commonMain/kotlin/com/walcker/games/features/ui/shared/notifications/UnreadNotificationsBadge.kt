package com.walcker.games.features.ui.shared.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.walcker.games.features.domain.shared.usecase.ObserveHasUnreadNotificationsUseCase
import org.koin.compose.koinInject

@Composable
public fun rememberHasUnreadNotifications(): State<Boolean> {
    val observeHasUnread: ObserveHasUnreadNotificationsUseCase = koinInject()
    val flow = remember(observeHasUnread) { observeHasUnread() }
    return flow.collectAsState(initial = false)
}
