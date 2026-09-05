package com.walcker.games.strings

import com.walcker.games.features.domain.shared.model.ReportReason

internal data class ReportStrings(
    val title: (String) -> String,
    val subtitle: String,
    val reasonLabel: (ReportReason) -> String,
    val detailsLabel: String,
    val detailsPlaceholder: String,
    val submit: String,
    val cancel: String,
    val reportAction: String,
    val success: String,
    val alreadyReported: String,
    val error: String,
)

internal val reportStringsEn =
    ReportStrings(
        title = { name -> "Report $name" },
        subtitle = "Reports are reviewed by moderation. The player is not told who reported them.",
        reasonLabel = { reason ->
            when (reason) {
                ReportReason.NO_SHOW -> "Didn't show up"
                ReportReason.LATE -> "Repeatedly late"
                ReportReason.NO_PAYMENT -> "Didn't pay"
                ReportReason.AGGRESSIVE_BEHAVIOR -> "Aggressive behaviour"
                ReportReason.VERBAL_ABUSE -> "Insults or verbal abuse"
                ReportReason.DISCRIMINATION -> "Discrimination"
                ReportReason.HARASSMENT -> "Harassment"
                ReportReason.DANGEROUS_PLAY -> "Dangerous play"
                ReportReason.FAKE_PROFILE -> "Fake profile"
                ReportReason.OTHER -> "Something else"
            }
        },
        detailsLabel = "What happened? (optional)",
        detailsPlaceholder = "Anything that helps moderation understand",
        submit = "Send report",
        cancel = "Cancel",
        reportAction = "Report",
        success = "Report sent. Moderation will take it from here.",
        alreadyReported = "You already reported this player for this match.",
        error = "Could not send the report. Please try again.",
    )

internal val reportStringsPt =
    ReportStrings(
        title = { name -> "Denunciar $name" },
        subtitle = "As denúncias são analisadas pela moderação. O jogador não sabe quem denunciou.",
        reasonLabel = { reason ->
            when (reason) {
                ReportReason.NO_SHOW -> "Não apareceu"
                ReportReason.LATE -> "Atrasa sempre"
                ReportReason.NO_PAYMENT -> "Não pagou"
                ReportReason.AGGRESSIVE_BEHAVIOR -> "Comportamento agressivo"
                ReportReason.VERBAL_ABUSE -> "Ofensas ou xingamentos"
                ReportReason.DISCRIMINATION -> "Discriminação"
                ReportReason.HARASSMENT -> "Assédio"
                ReportReason.DANGEROUS_PLAY -> "Jogo violento"
                ReportReason.FAKE_PROFILE -> "Perfil falso"
                ReportReason.OTHER -> "Outro motivo"
            }
        },
        detailsLabel = "O que aconteceu? (opcional)",
        detailsPlaceholder = "Qualquer coisa que ajude a moderação a entender",
        submit = "Enviar denúncia",
        cancel = "Cancelar",
        reportAction = "Denunciar",
        success = "Denúncia enviada. A moderação assume daqui.",
        alreadyReported = "Você já denunciou esse jogador nessa partida.",
        error = "Não foi possível enviar a denúncia. Tente novamente.",
    )
