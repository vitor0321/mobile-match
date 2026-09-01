package com.walcker.identity.features.data.remote

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.walcker.identity.api.UserSession
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault
import com.walcker.match.core.navigation.CurrentActivityHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import com.walcker.identity.features.data.remote.GoogleAuthSource as FeatureGoogleAuthSource

private const val TAG = "GoogleAuthSource"

internal class AndroidGoogleAuthSource(
    application: Application,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val activityHolder: CurrentActivityHolder,
    private val stringsHolder: IdentityStringsHolder,
) : FeatureGoogleAuthSource {
    private val applicationContext = application.applicationContext

    override suspend fun signIn(): Result<UserSession> =
        try {
            val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
            val activity =
                activityHolder.currentActivity()
                    ?: error(strings.noForegroundActivity)
            val serverClientId =
                resolveServerClientId()
                    ?: error(strings.missingWebClientId)

            val idToken = requestGoogleIdToken(activity, serverClientId)
            val user = signInToFirebase(idToken)
            Result.success(user)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Log.w(TAG, "Google Sign-In failed: ${error.message}", error)
            Result.failure(mapError(error))
        }

    private suspend fun requestGoogleIdToken(
        activity: Activity,
        serverClientId: String,
    ): String {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        val response =
            try {
                credentialManager.getCredential(
                    context = activity,
                    request = buildRequest(serverClientId, filterByAuthorizedAccounts = true),
                )
            } catch (_: NoCredentialException) {
                credentialManager.getCredential(
                    context = activity,
                    request = buildRequest(serverClientId, filterByAuthorizedAccounts = false),
                )
            }

        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error(strings.unexpectedCredential(credential::class.simpleName))
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (parsing: GoogleIdTokenParsingException) {
            throw IllegalStateException(strings.invalidGoogleIdToken, parsing)
        }
    }

    private suspend fun signInToFirebase(idToken: String): UserSession {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(authCredential).await()
        val firebaseUser =
            authResult.user
                ?: error(strings.missingAuthenticatedUserAfterGoogleSignIn)
        return firebaseUser.toUserSession()
    }

    private fun buildRequest(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean,
    ): GetCredentialRequest {
        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setAutoSelectEnabled(false)
                .build()
        return GetCredentialRequest
            .Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    private fun resolveServerClientId(): String? {
        val resourceId =
            applicationContext.resources.getIdentifier(
                "default_web_client_id",
                "string",
                applicationContext.packageName,
            )
        if (resourceId == 0) return null
        return applicationContext
            .getString(resourceId)
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun mapError(error: Throwable): Throwable {
        val strings = stringsHolder.resolveStringsOrDefault().nativeAuth
        if (error is GetCredentialCancellationException) {
            return IdentityError.Cancelled
        }
        val message = error.message.orEmpty()
        if (error is SecurityException &&
            message.contains("Unknown calling package name 'com.google.android.gms'")
        ) {
            return IdentityError.Configuration
        }
        if (message.contains("DEVELOPER_ERROR")) {
            return IllegalStateException(
                strings.androidGoogleDeveloperError,
                error,
            )
        }
        if (message.contains("Developer console is not set up correctly", ignoreCase = true) ||
            message.contains("28444")
        ) {
            return IllegalStateException(
                strings.androidGoogleNotConfiguredForPackage(applicationContext.packageName),
                error,
            )
        }
        if (error is GetCredentialException) {
            return IllegalStateException(
                strings.googleCredentialRequestError(error.errorMessage?.toString() ?: error.message),
                error,
            )
        }
        return error
    }
}

private fun FirebaseUser.toUserSession(): UserSession =
    UserSession(
        uid = uid,
        email = email,
        displayName = displayName,
        creationTimestamp = metadata?.creationTimestamp,
    )
