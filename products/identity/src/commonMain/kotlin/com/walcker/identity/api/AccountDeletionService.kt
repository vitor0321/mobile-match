package com.walcker.identity.api

public interface AccountDeletionService {
    suspend fun deleteAccount(): AccountDeletionOutcome
}

public sealed interface AccountDeletionOutcome {
    public data object Success : AccountDeletionOutcome

    public data object RequiresRecentLogin : AccountDeletionOutcome

    public data class Failure(
        val cause: Throwable,
    ) : AccountDeletionOutcome
}
