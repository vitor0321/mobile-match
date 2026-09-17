package com.walcker.games.fake

import com.walcker.identity.api.AccountDeletionOutcome
import com.walcker.identity.api.AccountDeletionService

internal class FakeAccountDeletionService(
    var outcome: AccountDeletionOutcome = AccountDeletionOutcome.Success,
) : AccountDeletionService {
    var deleteCallCount: Int = 0
        private set

    override suspend fun deleteAccount(): AccountDeletionOutcome {
        deleteCallCount++
        return outcome
    }
}
