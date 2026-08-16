package com.walcker.games.strings

/**
 * Localized strings for notification history screen.
 */
internal interface NotificationHistoryStrings {
    val title: String
    val emptyState: String
    val noNotifications: String
    val newMatch: String
    val youWereAdded: String
    val refreshing: String
    val error: String
}

internal data class PtBrNotificationHistoryStrings(
    override val title: String = "Notificações",
    override val emptyState: String = "Sem notificações",
    override val noNotifications: String = "Nenhuma notificação por enquanto",
    override val newMatch: String = "Novo jogo",
    override val youWereAdded: String = "Você foi adicionado",
    override val refreshing: String = "Atualizando...",
    override val error: String = "Erro ao carregar notificações",
) : NotificationHistoryStrings

internal data class EnNotificationHistoryStrings(
    override val title: String = "Notifications",
    override val emptyState: String = "No notifications",
    override val noNotifications: String = "You don't have any notifications yet",
    override val newMatch: String = "New match",
    override val youWereAdded: String = "You were added",
    override val refreshing: String = "Refreshing...",
    override val error: String = "Error loading notifications",
) : NotificationHistoryStrings
