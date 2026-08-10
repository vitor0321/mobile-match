package com.walcker.identity.features.data.remote

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIROAuthProvider
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.AppleAuthSource as FeatureAppleAuthSource
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
internal actual fun createAppleAuthSource(stringsHolder: IdentityStringsHolder): FeatureAppleAuthSource {
    return IosAppleAuthSource(
        auth = FIRAuth.auth(),
        stringsHolder = stringsHolder,
    )
}

internal actual val isAppleSignInAvailable: Boolean = true

@OptIn(ExperimentalForeignApi::class)
internal class IosAppleAuthSource(
    private val auth: FIRAuth,
    private val stringsHolder: IdentityStringsHolder,
) : FeatureAppleAuthSource {
    override suspend fun signIn(): Result<UserSession> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        val anchor = currentPresentationAnchor()
            ?: return Result.failure(IllegalStateException(strings.missingPresentationAnchor))

        val rawNonce = randomNonce()
        val hashedNonce = sha256Hex(rawNonce)

        val appleProvider = ASAuthorizationAppleIDProvider()
        val request = appleProvider.createRequest().apply {
            setRequestedScopes(listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail))
            setNonce(hashedNonce)
        }

        return suspendCancellableCoroutine { continuation ->
            val delegate = AppleSignInDelegate(
                anchor = anchor,
                onCredential = { credential ->
                    val identityToken = credential.identityToken?.let { data ->
                        @OptIn(BetaInteropApi::class)
                        NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
                    }
                    if (identityToken.isNullOrBlank()) {
                        continuation.resume(Result.failure(IllegalStateException(strings.invalidAppleIdToken)))
                        return@AppleSignInDelegate
                    }

                    val firCredential = FIROAuthProvider.credentialWithProviderID(
                        providerID = "apple.com",
                        IDToken = identityToken,
                        rawNonce = rawNonce,
                    )
                    auth.signInWithCredential(firCredential) { authResult, authError ->
                        if (authError != null) {
                            continuation.resume(Result.failure(authError.toThrowable(strings.firebaseAppleAuthFailed)))
                            return@signInWithCredential
                        }
                        val session = authResult?.user()?.toUserSession()
                        continuation.resume(
                            if (session != null) {
                                Result.success(session)
                            } else {
                                Result.failure(IllegalStateException(strings.missingAuthenticatedUserAfterAppleSignIn))
                            },
                        )
                    }
                },
                onError = { error ->
                    continuation.resume(Result.failure(error.toAppleSignInError()))
                },
            )

            val controller = ASAuthorizationController(authorizationRequests = listOf(request))
            controller.delegate = delegate
            controller.presentationContextProvider = delegate
            controller.performRequests()

            continuation.invokeOnCancellation {
                delegate.dispose()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class AppleSignInDelegate(
    private val anchor: UIWindow,
    onCredential: (ASAuthorizationAppleIDCredential) -> Unit,
    onError: (NSError) -> Unit,
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    private var onCredential: ((ASAuthorizationAppleIDCredential) -> Unit)? = onCredential
    private var onError: ((NSError) -> Unit)? = onError

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        if (credential != null) {
            onCredential?.invoke(credential)
        } else {
            onError?.invoke(NSError.errorWithDomain("AppleSignIn", -1, null))
        }
        dispose()
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        onError?.invoke(didCompleteWithError)
        dispose()
    }

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController,
    ): ASPresentationAnchor = anchor

    fun dispose() {
        onCredential = null
        onError = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSError.toAppleSignInError(): Throwable =
    if (code == 1001L) IdentityError.Cancelled else IdentityError.ProviderUnavailable

private fun currentPresentationAnchor(): UIWindow? {
    val app = UIApplication.sharedApplication
    @Suppress("DEPRECATION")
    return app.keyWindow ?: app.windows.firstOrNull() as? UIWindow
}

@OptIn(ExperimentalForeignApi::class)
private fun randomNonce(length: Int = 32): String {
    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        SecRandomCopyBytes(kSecRandomDefault, length.toULong(), pinned.addressOf(0))
    }
    return bytes.joinToString("") { byte ->
        charset[(byte.toInt() and 0xFF) % charset.length].toString()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun sha256Hex(input: String): String {
    val data = input.encodeToByteArray()
    val hash = UByteArray(CC_SHA256_DIGEST_LENGTH)
    data.usePinned { pinnedData ->
        hash.usePinned { pinnedHash ->
            CC_SHA256(pinnedData.addressOf(0), data.size.toUInt(), pinnedHash.addressOf(0))
        }
    }
    return hash.joinToString("") { byte ->
        byte.toInt().toString(16).padStart(2, '0')
    }
}
