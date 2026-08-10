package com.walcker.identity.strings

internal data class NativeAuthStrings(
    val noForegroundActivity: String,
    val missingWebClientId: String,
    val unexpectedCredential: (String?) -> String,
    val invalidGoogleIdToken: String,
    val missingAuthenticatedUserAfterGoogleSignIn: String,
    val googleSignInCancelled: String,
    val androidGoogleNotConfiguredInFirebase: String,
    val androidGoogleDeveloperError: String,
    val androidGoogleNotConfiguredForPackage: (String) -> String,
    val googleCredentialRequestError: (String?) -> String,
    val missingActiveViewController: String,
    val googleUnavailableOrCancelled: String,
    val invalidGoogleTokens: String,
    val firebaseGoogleAuthFailed: String,
    val missingPresentationAnchor: String,
    val appleUnavailableOrCancelled: String,
    val invalidAppleIdToken: String,
    val firebaseAppleAuthFailed: String,
    val missingAuthenticatedUserAfterAppleSignIn: String,
    val signOutFailed: String,
    val authErrorFallback: String,
    val missingAuthenticatedUser: String,
)

internal val nativeAuthStringsEn = NativeAuthStrings(
    noForegroundActivity = "No foreground activity available to start Google Sign-In",
    missingWebClientId = "default_web_client_id is missing. Check google-services.json and the com.google.gms.google-services plugin",
    unexpectedCredential = { credentialName ->
        "Unexpected credential returned by Credential Manager: ${credentialName ?: "unknown"}"
    },
    invalidGoogleIdToken = "Could not read the Google ID token",
    missingAuthenticatedUserAfterGoogleSignIn = "Firebase did not return the authenticated user after Google Sign-In",
    googleSignInCancelled = "Google Sign-In was cancelled by the user",
    androidGoogleNotConfiguredInFirebase = "Android Google Sign-In is not configured in Firebase. Add the current build SHA-1/SHA-256 and download a new google-services.json.",
    androidGoogleDeveloperError = "Google Sign-In returned DEVELOPER_ERROR. Check the package name, SHA-1/SHA-256, and the Firebase Web OAuth client.",
    androidGoogleNotConfiguredForPackage = { packageName ->
        "Android Google Sign-In is not configured for $packageName. In Firebase/Google Cloud, register this package with the current build SHA-1/SHA-256 or run debug with the applicationId already present in google-services.json."
    },
    googleCredentialRequestError = { message ->
        "Could not get the Google credential: ${message ?: "unknown error"}"
    },
    missingActiveViewController = "No active view controller available to start Google Sign-In",
    googleUnavailableOrCancelled = "Google Sign-In was cancelled or is unavailable",
    invalidGoogleTokens = "Google Sign-In did not return valid tokens",
    firebaseGoogleAuthFailed = "Could not authenticate with Firebase",
    missingPresentationAnchor = "No active window available to start Apple Sign-In",
    appleUnavailableOrCancelled = "Apple Sign-In was cancelled or is unavailable",
    invalidAppleIdToken = "Apple Sign-In did not return a valid identity token",
    firebaseAppleAuthFailed = "Could not authenticate with Firebase using Apple",
    missingAuthenticatedUserAfterAppleSignIn = "Firebase did not return the authenticated user after Apple Sign-In",
    signOutFailed = "Could not sign out",
    authErrorFallback = "Authentication error",
    missingAuthenticatedUser = "Firebase did not return the authenticated user",
)

internal val nativeAuthStringsPt = NativeAuthStrings(
    noForegroundActivity = "Nenhuma activity em foreground para iniciar o Google Sign-In",
    missingWebClientId = "default_web_client_id ausente. Verifique google-services.json e o plugin com.google.gms.google-services",
    unexpectedCredential = { credentialName ->
        "Credencial inesperada do Credential Manager: ${credentialName ?: "desconhecida"}"
    },
    invalidGoogleIdToken = "Falha ao ler o ID token do Google",
    missingAuthenticatedUserAfterGoogleSignIn = "Firebase não retornou o usuário autenticado após o Google Sign-In",
    googleSignInCancelled = "Google Sign-In cancelado pelo usuário",
    androidGoogleNotConfiguredInFirebase = "Google Sign-In Android não está configurado no Firebase. Adicione o SHA-1/SHA-256 da build atual e baixe o google-services.json novo.",
    androidGoogleDeveloperError = "Google Sign-In retornou DEVELOPER_ERROR. Verifique package name, SHA-1/SHA-256 e o client OAuth Web do Firebase.",
    androidGoogleNotConfiguredForPackage = { packageName ->
        "Google Sign-In Android não está configurado para $packageName. No Firebase/Google Cloud, cadastre este package com o SHA-1/SHA-256 da build atual ou rode o debug com o applicationId que já existe no google-services.json."
    },
    googleCredentialRequestError = { message ->
        "Não foi possível obter a credencial do Google: ${message ?: "erro desconhecido"}"
    },
    missingActiveViewController = "Nenhuma view controller ativa para iniciar o Google Sign-In",
    googleUnavailableOrCancelled = "Google Sign-In cancelado ou indisponível",
    invalidGoogleTokens = "Google Sign-In não retornou tokens válidos",
    firebaseGoogleAuthFailed = "Não foi possível autenticar no Firebase",
    missingPresentationAnchor = "Nenhuma janela ativa para iniciar o Apple Sign-In",
    appleUnavailableOrCancelled = "Apple Sign-In cancelado ou indisponível",
    invalidAppleIdToken = "Apple Sign-In não retornou um identity token válido",
    firebaseAppleAuthFailed = "Não foi possível autenticar no Firebase com Apple",
    missingAuthenticatedUserAfterAppleSignIn = "Firebase não retornou o usuário autenticado após o Apple Sign-In",
    signOutFailed = "Não foi possível sair",
    authErrorFallback = "Erro de autenticação",
    missingAuthenticatedUser = "Firebase não retornou o usuário autenticado",
)

