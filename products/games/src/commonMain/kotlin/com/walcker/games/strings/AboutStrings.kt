package com.walcker.games.strings

internal data class AboutStrings(
    val title: String,
    val backContentDescription: String,
    val appName: String,
    val appTagline: String,
    val storyTitle: String,
    val storyBody: String,
    val missionTitle: String,
    val missionBody: String,
    val termsLabel: String,
    val contactLabel: String,
)

internal val aboutStringsPt =
    AboutStrings(
        title = "Sobre",
        backContentDescription = "Voltar",
        appName = "Join Play",
        appTagline = "Conectando pessoas através do esporte.",
        storyTitle = "Por que o Join Play existe",
        storyBody =
            "Quem organiza uma partida sabe: o mais difícil raramente é achar quadra — é fechar o " +
                "time. Um cancelamento de última hora, um grupo que não responde, e a reserva que " +
                "custou tempo e dinheiro vira um jogo pela metade, ou nenhum jogo.",
        missionTitle = "Como ajudamos o organizador",
        missionBody =
            "Quando falta gente pra completar a partida, você anuncia a vaga e alcança uma comunidade " +
                "de jogadores perto de você. Quem quer jogar encontra e entra na hora — sem depender " +
                "de indicação, sem grupo de WhatsApp, sem partida cancelada por falta de jogador.",
        termsLabel = "Termos e Política de Privacidade",
        contactLabel = "Contato",
    )

internal val aboutStringsEn =
    AboutStrings(
        title = "About",
        backContentDescription = "Back",
        appName = "Join Play",
        appTagline = "Connecting people through sport.",
        storyTitle = "Why Join Play exists",
        storyBody =
            "Anyone who organizes a pickup game knows: the hard part is rarely finding a court — " +
                "it's filling the team. A last-minute cancellation, a group chat that goes quiet, " +
                "and the booking that cost time and money turns into half a game, or no game at all.",
        missionTitle = "How we help organizers",
        missionBody =
            "When you're short on players, you post the open spot and reach a community of players " +
                "nearby. Whoever wants to play finds it and joins right away — no relying on referrals, " +
                "no group chat, no game cancelled for lack of players.",
        termsLabel = "Terms and Privacy Policy",
        contactLabel = "Contact",
    )
