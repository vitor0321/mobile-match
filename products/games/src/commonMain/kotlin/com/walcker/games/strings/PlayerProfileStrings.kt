package com.walcker.games.strings

internal data class PlayerProfileStrings(
    val title: String,
    val statsOrganized: String,
    val statsParticipated: String,
    val accountSettings: String,
    val loadingError: String,
)

internal val playerProfileStringsPt = PlayerProfileStrings(
    title = "Perfil",
    statsOrganized = "Partidas\norganizadas",
    statsParticipated = "Partidas\nparticipadas",
    accountSettings = "Configurações de conta",
    loadingError = "Erro ao carregar perfil.",
)

internal val playerProfileStringsEn = PlayerProfileStrings(
    title = "Profile",
    statsOrganized = "Matches\norganized",
    statsParticipated = "Matches\nparticipated",
    accountSettings = "Account settings",
    loadingError = "Error loading profile.",
)
