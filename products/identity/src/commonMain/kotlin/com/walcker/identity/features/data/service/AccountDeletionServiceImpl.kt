package com.walcker.identity.features.data.service

import com.walcker.identity.api.AccountDeletionOutcome
import com.walcker.identity.api.AccountDeletionService
import com.walcker.identity.features.domain.usecase.DeleteAccountResult
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCase

internal class AccountDeletionServiceImpl(
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : AccountDeletionService {
    override suspend fun deleteAccount(): AccountDeletionOutcome =
        when (val result = deleteAccountUseCase()) {
            DeleteAccountResult.Success -> AccountDeletionOutcome.Success
            DeleteAccountResult.RequiresRecentLogin -> AccountDeletionOutcome.RequiresRecentLogin
            is DeleteAccountResult.RemoteDataFailure -> AccountDeletionOutcome.Failure(result.cause)
            is DeleteAccountResult.AuthDeletionFailure -> AccountDeletionOutcome.Failure(result.cause)
            is DeleteAccountResult.LocalCleanupFailure -> AccountDeletionOutcome.Failure(result.cause)
        }
}
