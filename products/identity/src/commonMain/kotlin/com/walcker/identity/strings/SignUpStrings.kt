package com.walcker.identity.strings

internal data class SignUpStrings(
    val title: String,
    val subtitle: String,
    val emailLabel: String,
    val passwordLabel: String,
    val confirmPasswordLabel: String,
    val submitButton: String,
    val submitLoadingButton: String,
    val loginButton: String,
    val blankFieldsError: String,
    val passwordMismatchError: String,
    val signUpError: String,
)

internal val signUpStringsEn = SignUpStrings(
    title = "Create account",
    subtitle = "Sign up to use Pro features",
    emailLabel = "E-mail",
    passwordLabel = "Password",
    confirmPasswordLabel = "Confirm password",
    submitButton = "Create account",
    submitLoadingButton = "Creating account...",
    loginButton = "I already have an account",
    blankFieldsError = "Fill in e-mail, password and password confirmation",
    passwordMismatchError = "Passwords do not match",
    signUpError = "Could not create the account",
)

internal val signUpStringsPt = SignUpStrings(
    title = "Criar conta",
    subtitle = "Cadastre-se para usar recursos Pro",
    emailLabel = "E-mail",
    passwordLabel = "Senha",
    confirmPasswordLabel = "Confirmar senha",
    submitButton = "Criar conta",
    submitLoadingButton = "Criando conta...",
    loginButton = "Já tenho conta",
    blankFieldsError = "Preencha e-mail, senha e confirmação de senha",
    passwordMismatchError = "As senhas não coincidem",
    signUpError = "Não foi possível criar a conta",
)

