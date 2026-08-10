package com.walcker.identity.strings

internal data class ForgotPasswordStrings(
    val title: String,
    val subtitle: String,
    val emailLabel: String,
    val submitButton: String,
    val submitLoadingButton: String,
    val successMessage: String,
    val blankEmailError: String,
    val sendEmailError: String,
    val backToLoginButton: String,
)

internal val forgotPasswordStringsEn = ForgotPasswordStrings(
    title = "Recover password",
    subtitle = "We will send a password reset link to your email",
    emailLabel = "E-mail",
    submitButton = "Send recovery email",
    submitLoadingButton = "Sending...",
    successMessage = "Recovery email sent successfully! Check your inbox.",
    blankEmailError = "Fill in the e-mail",
    sendEmailError = "Could not send recovery email",
    backToLoginButton = "Back to Sign in",
)

internal val forgotPasswordStringsPt = ForgotPasswordStrings(
    title = "Recuperar senha",
    subtitle = "Enviaremos um link de recuperação para o seu e-mail",
    emailLabel = "E-mail",
    submitButton = "Enviar e-mail de recuperação",
    submitLoadingButton = "Enviando...",
    successMessage = "E-mail de recuperação enviado com sucesso! Verifique sua caixa de entrada.",
    blankEmailError = "Preencha o e-mail",
    sendEmailError = "Não foi possível enviar o e-mail de recuperação",
    backToLoginButton = "Voltar para o Login",
)
