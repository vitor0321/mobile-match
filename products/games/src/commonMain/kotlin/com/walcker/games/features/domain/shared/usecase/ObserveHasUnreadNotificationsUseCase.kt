package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.repository.NotificationRepository
import com.walcker.identity.api.SessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

internal interface ObserveHasUnreadNotificationsUseCase {
    operator fun invoke(): Flow<Boolean>
}

internal class ObserveHasUnreadNotificationsUseCaseImpl(
    private val repository: NotificationRepository,
    private val sessionHolder: SessionHolder,
) : ObserveHasUnreadNotificationsUseCase {
    override fun invoke(): Flow<Boolean> =
        sessionHolder.currentUser.flatMapLatest { session ->
            if (session == null) flowOf(false) else repository.observeHasUnread(session.uid)
        }
}
