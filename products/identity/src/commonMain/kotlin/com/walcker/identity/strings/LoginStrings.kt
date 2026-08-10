package com.walcker.identity.strings

internal data class LoginStrings(
    val title: String,
    val subtitle: String,
    val emailLabel: String,
    val passwordLabel: String,
    val submitButton: String,
    val submitLoadingButton: String,
    val googleButton: String,
    val appleButton: String,
    val signUpButton: String,
    val blankFieldsError: String,
    val signInError: String,
    val googleSignInError: String,
    val appleSignInError: String,
    val forgotPasswordButton: String,
)

internal val loginStringsEn = LoginStrings(
    title = "Sign in",
    subtitle = "Access your Match account",
    emailLabel = "E-mail",
    passwordLabel = "Password",
    submitButton = "Sign in",
    submitLoadingButton = "Signing in...",
    googleButton = "Continue with Google",
    appleButton = "Continue with Apple",
    signUpButton = "Create account",
    blankFieldsError = "Fill in e-mail and password",
    signInError = "Could not sign in",
    googleSignInError = "Could not sign in with Google",
    appleSignInError = "Could not sign in with Apple",
    forgotPasswordButton = "Forgot password?",
)

internal val loginStringsPt = LoginStrings(
    title = "Entrar",
    subtitle = "Acesse sua conta Match",
    emailLabel = "E-mail",
    passwordLabel = "Senha",
    submitButton = "Entrar",
    submitLoadingButton = "Entrando...",
    googleButton = "Continuar com Google",
    appleButton = "Continuar com Apple",
    signUpButton = "Criar conta",
    blankFieldsError = "Preencha e-mail e senha",
    signInError = "Não foi possível entrar",
    googleSignInError = "Não foi possível entrar com Google",
    appleSignInError = "Não foi possível entrar com Apple",
    forgotPasswordButton = "Esqueci minha senha",
)

