package com.walcker.identity.api

public interface AccountDeletionService {
    suspend fun deleteAccount(): AccountDeletionOutcome

    suspend fun reauthenticateWithPassword(password: String): ReauthenticationOutcome

    suspend fun reauthenticateWithProvider(): ReauthenticationOutcome
}

public enum class ReauthenticationMethod { Password, Google, Apple }

public sealed interface AccountDeletionOutcome {
    public data object Success : AccountDeletionOutcome

    public data class RequiresReauthentication(
        val method: ReauthenticationMethod,
    ) : AccountDeletionOutcome

    public data class Failure(
        val cause: Throwable,
    ) : AccountDeletionOutcome
}

public sealed interface ReauthenticationOutcome {
    public data object Success : ReauthenticationOutcome

    public data object WrongPassword : ReauthenticationOutcome

    public data object Cancelled : ReauthenticationOutcome

    public data class Failure(
        val cause: Throwable,
    ) : ReauthenticationOutcome
}
