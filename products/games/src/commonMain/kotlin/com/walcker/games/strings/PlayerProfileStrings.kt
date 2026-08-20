package com.walcker.games.strings

internal data class PlayerProfileStrings(
    val title: String,
    val statsOrganized: String,
    val statsParticipated: String,
    val accountSettings: String,
    val loadingError: String,
    /** Cabeçalho do bloco do toggle "estou disponível" (regra B5). */
    val availabilityTitle: String,
    /**
     * O texto tem de dizer o que o toggle FAZ, não só o que ele é: desligado
     * significa não receber aviso de partida nenhuma, e isso precisa estar
     * claro antes do toque, não depois do silêncio.
     */
    val availabilityOnDescription: String,
    val availabilityOffDescription: String,
    val availabilityError: String,
)

internal val playerProfileStringsPt = PlayerProfileStrings(
    title = "Perfil",
    statsOrganized = "Partidas\norganizadas",
    statsParticipated = "Partidas\nparticipadas",
    accountSettings = "Configurações de conta",
    loadingError = "Erro ao carregar perfil.",
    availabilityTitle = "Estou disponível para jogar",
    availabilityOnDescription = "Você recebe aviso quando abrir partida perto de você.",
    availabilityOffDescription = "Você não recebe aviso de partida nova.",
    availabilityError = "Não foi possível mudar sua disponibilidade. Tente de novo.",
)

internal val playerProfileStringsEn = PlayerProfileStrings(
    title = "Profile",
    statsOrganized = "Matches\norganized",
    statsParticipated = "Matches\nparticipated",
    accountSettings = "Account settings",
    loadingError = "Error loading profile.",
    availabilityTitle = "I'm available to play",
    availabilityOnDescription = "You'll be notified when a match opens near you.",
    availabilityOffDescription = "You won't be notified about new matches.",
    availabilityError = "Could not change your availability. Please try again.",
)
