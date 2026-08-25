package com.walcker.identity.strings

internal data class LoginStrings(
    val title: String,
    val subtitle: String,
    /** Nome acessível do botão voltar da barra de topo. */
    val back: String,
    val emailLabel: String,
    val passwordLabel: String,
    /** Nome acessível do olho quando a senha está escondida. */
    val showPassword: String,
    /** Nome acessível do olho quando a senha está à mostra. */
    val hidePassword: String,
    val submitButton: String,
    val submitLoadingButton: String,
    /** Separa o formulário de e-mail dos provedores sociais. */
    val socialDivider: String,
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
    back = "Back",
    emailLabel = "E-mail",
    passwordLabel = "Password",
    showPassword = "Show password",
    hidePassword = "Hide password",
    submitButton = "Sign in",
    submitLoadingButton = "Signing in...",
    socialDivider = "or continue with",
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
    back = "Voltar",
    emailLabel = "E-mail",
    passwordLabel = "Senha",
    showPassword = "Mostrar senha",
    hidePassword = "Esconder senha",
    submitButton = "Entrar",
    submitLoadingButton = "Entrando...",
    socialDivider = "ou continue com",
    googleButton = "Continuar com Google",
    appleButton = "Continuar com Apple",
    signUpButton = "Criar conta",
    blankFieldsError = "Preencha e-mail e senha",
    signInError = "Não foi possível entrar",
    googleSignInError = "Não foi possível entrar com Google",
    appleSignInError = "Não foi possível entrar com Apple",
    forgotPasswordButton = "Esqueci minha senha",
)
