package com.walcker.games.strings

/**
 * Copy for the match detail screen.
 *
 * This did not exist: the screen carried its text inline, half of it in English
 * ("Match Details", "Duration", "Price", "Participants", "Free", "Retry") and half
 * in pt-BR ("Sair", "Cancelar Partida", "Entrar na partida"). On a screen that
 * showed both at once.
 */
internal data class MatchDetailStrings(
    val title: String,
    val backContentDescription: String,
    val dismissContentDescription: String,

    val organizer: (name: String, rating: Double) -> String,
    val durationLabel: String,
    val durationValue: (Int) -> String,
    val priceLabel: String,
    val freePrice: String,

    val statusOpen: String,
    val statusFull: String,
    val statusFinished: String,
    val statusCancelled: String,

    val participants: String,
    val confirmedOf: (confirmed: Int, total: Int) -> String,
    val openSlotsRemaining: (Int) -> String,
    val noSlotsRemaining: String,
    val confirmedSection: (Int) -> String,
    val waitlistSection: (Int) -> String,
    val confirmedTag: String,
    val paidTag: String,
    val queuePosition: (Int) -> String,
    val anonymousPlayer: (Int) -> String,
    val rateAction: String,

    val joinMatch: String,
    val joinWaitlist: String,
    val matchClosed: String,
    val leaveMatch: String,
    val cancelMatch: String,

    val promotedFromWaitlist: String,
    val retry: String,
    val notFound: String,

    val leaveDialogTitle: String,
    val leaveDialogBody: String,
    val leaveDialogConfirm: String,
    val cancelDialogTitle: String,
    val cancelDialogBody: String,
    val cancelDialogConfirm: String,
    val dialogDismiss: String,

    // Mensagens de resultado das ações (banner/live region). Antes eram literais
    // pt-BR no StepModel — e os erros vazavam `error.message` cru da exceção.
    // "Confirmado" não tem texto aqui: abre a tela de confirmação, não banner.
    val joinWaitlistSuccess: (position: Int) -> String,
    val joinAlreadyJoined: String,
    val joinError: String,
    val leaveSuccess: String,
    val leaveError: String,
    val cancelSuccess: String,
    val cancelAlreadyCancelled: String,
    val cancelError: String,
    val loadError: String,

    // Aviso de mudança de status da partida.
    val statusChangedToFull: String,
    val statusChangedToFinished: String,
    val statusChangedToCancelled: String,

    // Fallback do título usado no aviso global de promoção quando o esporte
    // ainda não carregou — nunca um nome de enum.
    val unknownMatchTitle: String,
)

internal val matchDetailStringsEn = MatchDetailStrings(
    title = "Match details",
    backContentDescription = "Back",
    dismissContentDescription = "Dismiss",

    organizer = { name, rating -> "Organised by $name · $rating★" },
    durationLabel = "Duration",
    durationValue = { min -> "$min min" },
    priceLabel = "Price",
    freePrice = "Free",

    statusOpen = "Open",
    statusFull = "Full",
    statusFinished = "Finished",
    statusCancelled = "Cancelled",

    participants = "Players",
    confirmedOf = { confirmed, total -> "$confirmed / $total confirmed" },
    openSlotsRemaining = { n -> if (n == 1) "1 slot left" else "$n slots left" },
    noSlotsRemaining = "No slots left",
    confirmedSection = { n -> "Confirmed ($n)" },
    waitlistSection = { n -> "Waitlist ($n)" },
    confirmedTag = "Confirmed",
    paidTag = "Paid",
    queuePosition = { pos -> "#$pos in queue" },
    anonymousPlayer = { n -> "Player $n" },
    rateAction = "Rate",

    joinMatch = "Take my slot",
    joinWaitlist = "Join the waitlist",
    matchClosed = "Match is over",
    leaveMatch = "Leave",
    cancelMatch = "Cancel match",

    promotedFromWaitlist = "You moved up from the waitlist!",
    retry = "Try again",
    notFound = "Match not found.",

    leaveDialogTitle = "Leave this match?",
    leaveDialogBody = "Your slot goes to the next player in the queue.",
    leaveDialogConfirm = "Leave",
    cancelDialogTitle = "Cancel this match?",
    cancelDialogBody = "Everyone who joined will be notified.",
    cancelDialogConfirm = "Cancel match",
    dialogDismiss = "Keep it",

    joinWaitlistSuccess = { pos -> "You're on the waitlist (position #$pos)" },
    joinAlreadyJoined = "You're already in this match",
    joinError = "Couldn't join the match. Try again.",
    leaveSuccess = "You left the match",
    leaveError = "Couldn't leave the match. Try again.",
    cancelSuccess = "Match cancelled",
    cancelAlreadyCancelled = "This match was already cancelled",
    cancelError = "Couldn't cancel the match. Try again.",
    loadError = "Couldn't load the match. Try again.",

    statusChangedToFull = "Match is full 🔴 New joins go to the waitlist.",
    statusChangedToFinished = "Match is over ✓",
    statusChangedToCancelled = "This match was cancelled ✕",

    unknownMatchTitle = "Match",
)

internal val matchDetailStringsPt = MatchDetailStrings(
    title = "Detalhes da partida",
    backContentDescription = "Voltar",
    dismissContentDescription = "Dispensar",

    organizer = { name, rating -> "Organizada por $name · $rating★" },
    durationLabel = "Duração",
    durationValue = { min -> "$min min" },
    priceLabel = "Preço",
    freePrice = "Grátis",

    statusOpen = "Aberta",
    statusFull = "Lotada",
    statusFinished = "Encerrada",
    statusCancelled = "Cancelada",

    participants = "Participantes",
    confirmedOf = { confirmed, total -> "$confirmed / $total confirmados" },
    openSlotsRemaining = { n -> if (n == 1) "1 vaga restante" else "$n vagas restantes" },
    noSlotsRemaining = "Sem vagas",
    confirmedSection = { n -> "Confirmados ($n)" },
    waitlistSection = { n -> "Fila de espera ($n)" },
    confirmedTag = "Confirmado",
    paidTag = "Pago",
    queuePosition = { pos -> "#$pos na fila" },
    anonymousPlayer = { n -> "Jogador $n" },
    rateAction = "Avaliar",

    joinMatch = "Garantir minha vaga",
    joinWaitlist = "Entrar na fila de espera",
    matchClosed = "Partida encerrada",
    leaveMatch = "Sair",
    cancelMatch = "Cancelar partida",

    promotedFromWaitlist = "Você foi promovido da fila!",
    retry = "Tentar de novo",
    notFound = "Partida não encontrada.",

    leaveDialogTitle = "Sair da partida?",
    leaveDialogBody = "Sua vaga vai para o próximo da fila.",
    leaveDialogConfirm = "Sair",
    cancelDialogTitle = "Cancelar a partida?",
    cancelDialogBody = "Todo mundo que entrou será avisado.",
    cancelDialogConfirm = "Cancelar partida",
    dialogDismiss = "Manter",

    joinWaitlistSuccess = { pos -> "Você foi adicionado à fila de espera (posição #$pos)" },
    joinAlreadyJoined = "Você já é participante desta partida",
    joinError = "Não foi possível entrar na partida. Tente de novo.",
    leaveSuccess = "Você saiu da partida",
    leaveError = "Não foi possível sair da partida. Tente de novo.",
    cancelSuccess = "Partida cancelada",
    cancelAlreadyCancelled = "Partida já foi cancelada",
    cancelError = "Não foi possível cancelar a partida. Tente de novo.",
    loadError = "Não foi possível carregar a partida. Tente de novo.",

    statusChangedToFull = "Partida lotada! 🔴 Novas entradas serão na fila de espera.",
    statusChangedToFinished = "Partida encerrada ✓",
    statusChangedToCancelled = "Partida foi cancelada ✕",

    unknownMatchTitle = "Partida",
)
