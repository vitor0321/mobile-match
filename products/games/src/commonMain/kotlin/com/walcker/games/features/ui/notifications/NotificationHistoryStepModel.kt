package com.walcker.games.features.ui.notifications

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.error.GamesError
import com.walcker.games.features.domain.usecase.GetNotificationHistoryUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class NotificationHistoryStepModel(
    private val getNotificationHistory: GetNotificationHistoryUseCase,
    private val sessionHolder: SessionHolder,
    private val stringsHolder: GamesStringsHolder,
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
            is NotificationHistoryEvent.MarkAsRead -> {
                // TODO Phase3-ETAPA3: implement mark as read
                _state.update { state ->
                    state.copy(
                        notifications = state.notifications.map { notif ->
                            if (notif.id == event.id) notif.copy(isRead = true) else notif
                        }
                    )
                }
            }
            is NotificationHistoryEvent.Delete -> {
                // TODO Phase3-ETAPA3: implement delete
                _state.update { state ->
                    state.copy(notifications = state.notifications.filter { it.id != event.id })
                }
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
                }
                .onFailure { error ->
                    val message = (error as? GamesError)?.message ?: error.message ?: "Erro"
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}
