package com.walcker.games.strings

internal data class LoginRequiredStrings(
    val title: String,
    val message: String,
    val confirm: String,
    val cancel: String,
)

internal val loginRequiredStringsPt = LoginRequiredStrings(
    title = "Você precisa entrar",
    message = "Para usar esta função, entre na sua conta ou crie uma agora — leva menos de um minuto.",
    confirm = "Entrar",
    cancel = "Agora não",
)

internal val loginRequiredStringsEn = LoginRequiredStrings(
    title = "Sign in required",
    message = "To use this feature, sign in or create an account — it takes less than a minute.",
    confirm = "Sign in",
    cancel = "Not now",
)
