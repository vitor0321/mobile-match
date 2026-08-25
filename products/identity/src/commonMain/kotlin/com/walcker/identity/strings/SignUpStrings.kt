package com.walcker.identity.strings

internal data class SignUpStrings(
    val title: String,
    val subtitle: String,
    /** Nome acessível do botão voltar da barra de topo. */
    val back: String,
    val emailLabel: String,
    val passwordLabel: String,
    /**
     * O mínimo que o Firebase Auth aceita. Estava só do lado do servidor: o usuário
     * digitava quatro caracteres, tocava em criar conta e recebia o erro depois.
     */
    val passwordHelper: String,
    val confirmPasswordLabel: String,
    /** Nome acessível do olho quando a senha está escondida. */
    val showPassword: String,
    /** Nome acessível do olho quando a senha está à mostra. */
    val hidePassword: String,
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
    back = "Back",
    emailLabel = "E-mail",
    passwordLabel = "Password",
    passwordHelper = "At least 6 characters",
    confirmPasswordLabel = "Confirm password",
    showPassword = "Show password",
    hidePassword = "Hide password",
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
    back = "Voltar",
    emailLabel = "E-mail",
    passwordLabel = "Senha",
    passwordHelper = "No mínimo 6 caracteres",
    confirmPasswordLabel = "Confirmar senha",
    showPassword = "Mostrar senha",
    hidePassword = "Esconder senha",
    submitButton = "Criar conta",
    submitLoadingButton = "Criando conta...",
    loginButton = "Já tenho conta",
    blankFieldsError = "Preencha e-mail, senha e confirmação de senha",
    passwordMismatchError = "As senhas não coincidem",
    signUpError = "Não foi possível criar a conta",
)
