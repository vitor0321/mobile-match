package com.walcker.games.fake

import com.walcker.identity.api.AccountDeletionOutcome
import com.walcker.identity.api.AccountDeletionService
import com.walcker.identity.api.ReauthenticationOutcome

internal class FakeAccountDeletionService(
    var outcome: AccountDeletionOutcome = AccountDeletionOutcome.Success,
    var outcomeAfterReauthentication: AccountDeletionOutcome = AccountDeletionOutcome.Success,
    var passwordOutcome: ReauthenticationOutcome = ReauthenticationOutcome.Success,
    var providerOutcome: ReauthenticationOutcome = ReauthenticationOutcome.Success,
) : AccountDeletionService {
    var deleteCallCount: Int = 0
        private set
    var providerCallCount: Int = 0
        private set
    var lastPassword: String? = null
        private set
    private var isReauthenticated: Boolean = false

    override suspend fun deleteAccount(): AccountDeletionOutcome {
        deleteCallCount++
        return if (isReauthenticated) outcomeAfterReauthentication else outcome
    }

    override suspend fun reauthenticateWithPassword(password: String): ReauthenticationOutcome {
        lastPassword = password
        return passwordOutcome.also { isReauthenticated = it == ReauthenticationOutcome.Success }
    }

    override suspend fun reauthenticateWithProvider(): ReauthenticationOutcome {
        providerCallCount++
        return providerOutcome.also { isReauthenticated = it == ReauthenticationOutcome.Success }
    }
}
