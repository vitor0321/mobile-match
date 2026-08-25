package com.walcker.games.strings

internal data class PlayerProfileStrings(
    val title: String,
    val statsOrganized: String,
    val statsParticipated: String,
    /** Third stat card in the redesign: the player's average rating. */
    val statsRating: String,
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
    // Below: copy that used to be hardcoded pt-BR inside the screen.
    val ratingsReceived: String,
    val ratingsCount: (Int) -> String,
    val ratingContentDescription: (Float) -> String,
    val settings: String,
    val logout: String,
    val noRatingYet: String,
)

internal val playerProfileStringsPt = PlayerProfileStrings(
    title = "Meu perfil",
    statsOrganized = "Organizadas",
    statsParticipated = "Participadas",
    statsRating = "Nota média",
    accountSettings = "Configurações de conta",
    loadingError = "Erro ao carregar perfil.",
    availabilityTitle = "Estou disponível para jogar",
    availabilityOnDescription = "Você recebe aviso quando abrir partida perto de você.",
    availabilityOffDescription = "Você não recebe aviso de partida nova.",
    availabilityError = "Não foi possível mudar sua disponibilidade. Tente de novo.",
    ratingsReceived = "Avaliações recebidas",
    ratingsCount = { n -> if (n == 1) "1 avaliação" else "$n avaliações" },
    ratingContentDescription = { value -> "Nota $value de 5" },
    settings = "Configurações",
    logout = "Sair da conta",
    noRatingYet = "—",
)

internal val playerProfileStringsEn = PlayerProfileStrings(
    title = "My profile",
    statsOrganized = "Organised",
    statsParticipated = "Played",
    statsRating = "Rating",
    accountSettings = "Account settings",
    loadingError = "Error loading profile.",
    availabilityTitle = "I'm available to play",
    availabilityOnDescription = "You'll be notified when a match opens near you.",
    availabilityOffDescription = "You won't be notified about new matches.",
    availabilityError = "Could not change your availability. Please try again.",
    ratingsReceived = "Ratings received",
    ratingsCount = { n -> if (n == 1) "1 rating" else "$n ratings" },
    ratingContentDescription = { value -> "Rated $value out of 5" },
    settings = "Settings",
    logout = "Log out",
    noRatingYet = "—",
)
