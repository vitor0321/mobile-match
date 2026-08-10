package com.walcker.identity.features.domain.usecase

import com.walcker.identity.features.data.billing.BillingClient
import com.walcker.identity.features.data.pro.ProStateCache
import com.walcker.identity.features.domain.repository.AccountDeletionRepository
import com.walcker.identity.features.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first

internal interface DeleteAccountUseCase {
    suspend operator fun invoke(): DeleteAccountResult
}

internal class DeleteAccountUseCaseImpl(
    private val accountDeletionRepository: AccountDeletionRepository,
    private val authRepository: AuthRepository,
    private val billingClient: BillingClient,
    private val proStateCache: ProStateCache,
) : DeleteAccountUseCase {
    override suspend fun invoke(): DeleteAccountResult {
        accountDeletionRepository.deleteRemoteData()
            .onFailure { cause ->
                return if (cause is RequiresRecentLoginException) {
                    DeleteAccountResult.RequiresRecentLogin
                } else {
                    DeleteAccountResult.RemoteDataFailure(cause)
                }
            }
        val uid = authRepository.currentUser.first()?.uid
            ?: return DeleteAccountResult.AuthDeletionFailure(IllegalStateException("No authenticated user"))
        authRepository.deleteAccount()
            .onFailure { cause ->
                return if (cause is RequiresRecentLoginException) {
                    DeleteAccountResult.RequiresRecentLogin
                } else {
                    DeleteAccountResult.AuthDeletionFailure(cause)
                }
            }
        billingClient.logOut()
            .onFailure { return DeleteAccountResult.LocalCleanupFailure(it) }
        return runCatching { proStateCache.clear(uid) }
            .fold(
                onSuccess = { DeleteAccountResult.Success },
                onFailure = { DeleteAccountResult.LocalCleanupFailure(it) },
            )
    }
}

internal sealed interface DeleteAccountResult {
    data object Success : DeleteAccountResult
    data object RequiresRecentLogin : DeleteAccountResult
    data class RemoteDataFailure(val cause: Throwable) : DeleteAccountResult
    data class AuthDeletionFailure(val cause: Throwable) : DeleteAccountResult
    data class LocalCleanupFailure(val cause: Throwable) : DeleteAccountResult
}

internal class RequiresRecentLoginException(cause: Throwable? = null) : Exception(cause)
