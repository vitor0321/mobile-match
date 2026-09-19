package com.walcker.games.strings

internal data class ErrorStrings(
    val noConnection: String,
)

internal val errorStringsPt =
    ErrorStrings(
        noConnection = "Sem conexão. Confira sua internet e tente de novo.",
    )

internal val errorStringsEn =
    ErrorStrings(
        noConnection = "No connection. Check your internet and try again.",
    )
