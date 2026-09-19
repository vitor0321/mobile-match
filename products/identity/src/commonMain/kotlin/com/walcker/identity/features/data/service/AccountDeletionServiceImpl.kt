package com.walcker.identity.features.data.service

import com.walcker.identity.api.AccountDeletionOutcome
import com.walcker.identity.api.AccountDeletionService
import com.walcker.identity.api.ReauthenticationMethod
import com.walcker.identity.api.ReauthenticationOutcome
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.domain.repository.AuthRepository
import com.walcker.identity.features.domain.usecase.DeleteAccountResult
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCase

private const val PASSWORD_PROVIDER = "password"
private const val GOOGLE_PROVIDER = "google.com"
private const val APPLE_PROVIDER = "apple.com"

internal class AccountDeletionServiceImpl(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val authRepository: AuthRepository,
) : AccountDeletionService {
    override suspend fun deleteAccount(): AccountDeletionOutcome =
        when (val result = deleteAccountUseCase()) {
            DeleteAccountResult.Success -> AccountDeletionOutcome.Success
            DeleteAccountResult.RequiresRecentLogin ->
                reauthenticationMethod().fold(
                    onSuccess = { AccountDeletionOutcome.RequiresReauthentication(it) },
                    onFailure = { AccountDeletionOutcome.Failure(it) },
                )
            is DeleteAccountResult.RemoteDataFailure -> AccountDeletionOutcome.Failure(result.cause)
            is DeleteAccountResult.AuthDeletionFailure -> AccountDeletionOutcome.Failure(result.cause)
            is DeleteAccountResult.LocalCleanupFailure -> AccountDeletionOutcome.Failure(result.cause)
        }

    override suspend fun reauthenticateWithPassword(password: String): ReauthenticationOutcome = authRepository.reauthenticateWithPassword(password).toReauthenticationOutcome()

    override suspend fun reauthenticateWithProvider(): ReauthenticationOutcome {
        val method = reauthenticationMethod().getOrElse { return ReauthenticationOutcome.Failure(it) }
        val result =
            when (method) {
                ReauthenticationMethod.Google -> authRepository.reauthenticateWithGoogle()
                ReauthenticationMethod.Apple -> authRepository.reauthenticateWithApple()
                ReauthenticationMethod.Password ->
                    return ReauthenticationOutcome.Failure(IllegalStateException("A password account confirms with its password"))
            }
        return result.toReauthenticationOutcome()
    }

    private suspend fun reauthenticationMethod(): Result<ReauthenticationMethod> =
        authRepository.signInProvider().mapCatching { provider ->
            when (provider) {
                PASSWORD_PROVIDER -> ReauthenticationMethod.Password
                GOOGLE_PROVIDER -> ReauthenticationMethod.Google
                APPLE_PROVIDER -> ReauthenticationMethod.Apple
                else -> throw IllegalStateException("Unsupported sign-in provider: $provider")
            }
        }
}

private fun Result<Unit>.toReauthenticationOutcome(): ReauthenticationOutcome =
    fold(
        onSuccess = { ReauthenticationOutcome.Success },
        onFailure = { error ->
            when (error) {
                IdentityError.InvalidCredentials -> ReauthenticationOutcome.WrongPassword
                IdentityError.Cancelled -> ReauthenticationOutcome.Cancelled
                else -> ReauthenticationOutcome.Failure(error)
            }
        },
    )
