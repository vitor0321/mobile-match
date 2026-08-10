package com.walcker.identity.features.data.remote

import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRGoogleAuthProvider
import cocoapods.GoogleSignIn.GIDSignIn
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.remote.GoogleAuthSource as FeatureGoogleAuthSource
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
internal class IosGoogleAuthSource(
    private val auth: FIRAuth,
    private val signIn: GIDSignIn,
    private val stringsHolder: IdentityStringsHolder,
) : FeatureGoogleAuthSource {
    override suspend fun signIn(): Result<UserSession> {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        val presentingViewController = topViewController()
            ?: return Result.failure(IllegalStateException(strings.missingActiveViewController))

        return suspendCancellableCoroutine { continuation ->
            signIn.signInWithPresentingViewController(presentingViewController) { result, signInError ->
                if (signInError != null) {
                    continuation.resume(Result.failure(signInError.toGoogleSignInError()))
                    return@signInWithPresentingViewController
                }

                val user = result?.user
                val idToken = user?.idToken?.tokenString
                val accessToken = user?.accessToken?.tokenString

                if (idToken.isNullOrBlank() || accessToken.isNullOrBlank()) {
                    continuation.resume(Result.failure(IllegalStateException(strings.invalidGoogleTokens)))
                    return@signInWithPresentingViewController
                }

                val credential = FIRGoogleAuthProvider.credentialWithIDToken(
                    idToken = idToken,
                    accessToken = accessToken,
                )
                auth.signInWithCredential(credential) { authResult, authError ->
                    if (authError != null) {
                        continuation.resume(Result.failure(authError.toThrowable(strings.firebaseGoogleAuthFailed)))
                        return@signInWithCredential
                    }

                    val session = authResult?.user()?.toUserSession()
                    continuation.resume(
                        if (session != null) {
                            Result.success(session)
                        } else {
                            Result.failure(IllegalStateException(strings.missingAuthenticatedUserAfterGoogleSignIn))
                        }
                    )
                }
            }
        }
    }
}

private fun topViewController(): UIViewController? {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: return null
    return root.topMostViewController()
}

private fun UIViewController.topMostViewController(): UIViewController {
    val presented = presentedViewController
    if (presented != null) return presented.topMostViewController()

    val navigationController = this as? UINavigationController
    val visible = navigationController?.visibleViewController
    if (visible != null) return visible.topMostViewController()

    val tabBarController = this as? UITabBarController
    val selected = tabBarController?.selectedViewController
    if (selected != null) return selected.topMostViewController()

    return this
}

private fun NSError.toGoogleSignInError(): Throwable =
    if (code == -5L) IdentityError.Cancelled else IdentityError.ProviderUnavailable
