package com.walcker.games.strings

internal data class MyMatchesStrings(
    val title: String,
    val activeTab: String,
    val pastTab: String,
    val emptyActive: String,
    val emptyActiveSubtitle: String,
    val emptyPast: String,
    val organizerBadge: String,
    val participantBadge: String,
    val cancelAction: String,
    val leaveAction: String,
    val cancelError: String,
    val leaveError: String,
    val statusCancelled: String,
    val statusFinished: String,
    /** "8/10 jogadores" — the raw "8/10" said nothing on its own. */
    val playersCount: (confirmed: Int, total: Int) -> String,
    /** Way out of the empty active tab: the screen used to be a dead end. */
    val emptyActiveAction: String,
)

internal val myMatchesStringsPt = MyMatchesStrings(
    title = "Minhas partidas",
    activeTab = "Ativas",
    pastTab = "Passadas",
    emptyActive = "Você ainda não organizou ou entrou em nenhuma partida.",
    emptyActiveSubtitle = "Explore partidas abertas próximas ou crie a sua.",
    emptyPast = "Nenhuma partida no histórico.",
    organizerBadge = "Organizador",
    participantBadge = "Participante",
    cancelAction = "Cancelar partida",
    leaveAction = "Sair da partida",
    cancelError = "Não foi possível cancelar a partida.",
    leaveError = "Não foi possível sair da partida.",
    statusCancelled = "Cancelada",
    statusFinished = "Finalizada",
    playersCount = { confirmed, total -> "$confirmed/$total jogadores" },
    emptyActiveAction = "Buscar partidas",
)

internal val myMatchesStringsEn = MyMatchesStrings(
    title = "My matches",
    activeTab = "Active",
    pastTab = "Past",
    emptyActive = "You haven't organized or joined any matches yet.",
    emptyActiveSubtitle = "Explore nearby open matches or create your own.",
    emptyPast = "No past matches.",
    organizerBadge = "Organizer",
    participantBadge = "Participant",
    cancelAction = "Cancel match",
    leaveAction = "Leave match",
    cancelError = "Could not cancel the match.",
    leaveError = "Could not leave the match.",
    statusCancelled = "Cancelled",
    statusFinished = "Finished",
    playersCount = { confirmed, total -> "$confirmed/$total players" },
    emptyActiveAction = "Find matches",
)
