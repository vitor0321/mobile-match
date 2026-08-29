package com.walcker.games.strings

internal interface NotificationHistoryStrings {
    val title: String
    val emptyState: String
    val noNotifications: String
    val newMatch: String
    val youWereAdded: String
    val refreshing: String
    val error: String

    val closeContentDescription: String
    val markAsReadContentDescription: String
    val deleteContentDescription: String
    val unreadContentDescription: String
    val dismissErrorContentDescription: String

    val timeJustNow: String
    val timeMinutesAgo: (Int) -> String
    val timeHoursAgo: (Int) -> String
    val timeDaysAgo: (Int) -> String
    val timeWeeksAgo: (Int) -> String
}

internal data class PtBrNotificationHistoryStrings(
    override val title: String = "Notificações",
    override val emptyState: String = "Sem notificações",
    override val noNotifications: String = "Nenhuma notificação por enquanto",
    override val newMatch: String = "Novo jogo",
    override val youWereAdded: String = "Você foi adicionado",
    override val refreshing: String = "Atualizando...",
    override val error: String = "Erro ao carregar notificações",

    override val closeContentDescription: String = "Fechar",
    override val markAsReadContentDescription: String = "Marcar como lida",
    override val deleteContentDescription: String = "Apagar notificação",
    override val unreadContentDescription: String = "Não lida",
    override val dismissErrorContentDescription: String = "Dispensar",

    override val timeJustNow: String = "Agora",
    override val timeMinutesAgo: (Int) -> String = { n ->
        if (n == 1) "há 1 minuto" else "há $n minutos"
    },
    override val timeHoursAgo: (Int) -> String = { n ->
        if (n == 1) "há 1 hora" else "há $n horas"
    },
    override val timeDaysAgo: (Int) -> String = { n ->
        if (n == 1) "ontem" else "há $n dias"
    },
    override val timeWeeksAgo: (Int) -> String = { n ->
        if (n == 1) "há 1 semana" else "há $n semanas"
    },
) : NotificationHistoryStrings

internal data class EnNotificationHistoryStrings(
    override val title: String = "Notifications",
    override val emptyState: String = "No notifications",
    override val noNotifications: String = "You don't have any notifications yet",
    override val newMatch: String = "New match",
    override val youWereAdded: String = "You were added",
    override val refreshing: String = "Refreshing...",
    override val error: String = "Error loading notifications",

    override val closeContentDescription: String = "Close",
    override val markAsReadContentDescription: String = "Mark as read",
    override val deleteContentDescription: String = "Delete notification",
    override val unreadContentDescription: String = "Unread",
    override val dismissErrorContentDescription: String = "Dismiss",

    override val timeJustNow: String = "Just now",
    override val timeMinutesAgo: (Int) -> String = { n ->
        if (n == 1) "1 minute ago" else "$n minutes ago"
    },
    override val timeHoursAgo: (Int) -> String = { n ->
        if (n == 1) "1 hour ago" else "$n hours ago"
    },
    override val timeDaysAgo: (Int) -> String = { n ->
        if (n == 1) "yesterday" else "$n days ago"
    },
    override val timeWeeksAgo: (Int) -> String = { n ->
        if (n == 1) "1 week ago" else "$n weeks ago"
    },
) : NotificationHistoryStrings
