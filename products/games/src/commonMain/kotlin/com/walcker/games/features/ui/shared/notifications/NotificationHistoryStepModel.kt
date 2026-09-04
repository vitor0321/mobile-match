package com.walcker.games.features.ui.shared.notifications

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.shared.error.GamesError
import com.walcker.games.features.domain.shared.usecase.DeleteNotificationUseCase
import com.walcker.games.features.domain.shared.usecase.GetNotificationHistoryUseCase
import com.walcker.games.features.domain.shared.usecase.MarkNotificationAsReadUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class NotificationHistoryStepModel(
    private val getNotificationHistory: GetNotificationHistoryUseCase,
    private val markNotificationAsRead: MarkNotificationAsReadUseCase,
    private val deleteNotification: DeleteNotificationUseCase,
    private val sessionHolder: SessionHolder,
    private val stringsHolder: GamesStringsHolder,
    private val crashReporter: CrashReporter,
) : ScreenModel {
    private val _state = MutableStateFlow(NotificationHistoryState())
    val state: StateFlow<NotificationHistoryState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: NotificationHistoryEvent) {
        when (event) {
            NotificationHistoryEvent.Refresh -> refresh()
            NotificationHistoryEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
            is NotificationHistoryEvent.MarkAsRead -> markAsReadAction(event.id)
            is NotificationHistoryEvent.Delete -> deleteAction(event.id)
        }
    }

    private fun markAsReadAction(notificationId: String) {
        screenModelScope.launch {
            val session = sessionHolder.currentUser.first()
            if (session == null) return@launch

            markNotificationAsRead(session.uid, notificationId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            notifications =
                                state.notifications.map { notif ->
                                    if (notif.id == notificationId) notif.copy(isRead = true) else notif
                                },
                        )
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    val message = (error as? GamesError)?.message ?: error.message ?: "Erro"
                    _state.update { it.copy(errorMessage = message) }
                }
        }
    }

    private fun deleteAction(notificationId: String) {
        screenModelScope.launch {
            val session = sessionHolder.currentUser.first()
            if (session == null) return@launch

            deleteNotification(session.uid, notificationId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(notifications = state.notifications.filter { it.id != notificationId })
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    val message = (error as? GamesError)?.message ?: error.message ?: "Erro"
                    _state.update { it.copy(errorMessage = message) }
                }
        }
    }

    private fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val session = sessionHolder.currentUser.first()
            if (session == null) {
                _state.update { it.copy(isLoading = false, notifications = emptyList()) }
                return@launch
            }

            getNotificationHistory(session.uid)
                .onSuccess { notifications ->
                    _state.update { it.copy(isLoading = false, notifications = notifications) }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    val message = (error as? GamesError)?.message ?: error.message ?: "Erro"
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}
