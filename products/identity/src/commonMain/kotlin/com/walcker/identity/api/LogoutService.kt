package com.walcker.identity.api

/**
 * Public API for logging out the current user.
 *
 * Calling [logout] signs out the current session and clears the auth state.
 * This is meant to be used by products that need a generic logout button
 * without importing identity's internal use cases.
 */
public interface LogoutService {
    /**
     * Sign out the currently authenticated user.
     *
     * On success, [SessionHolder.currentUser] emits null and the auth gate
     * navigates back to the login screen.
     */
    suspend fun logout(): Result<Unit>
}
