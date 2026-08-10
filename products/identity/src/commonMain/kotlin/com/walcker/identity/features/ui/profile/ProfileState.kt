package com.walcker.identity.features.ui.profile

import com.walcker.identity.api.UserSession

internal data class ProfileState(
    val userSession: UserSession? = null,
    val isPro: Boolean = false,
    val isLoading: Boolean = false,
    val isRestoringPurchases: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showDeleteAccountConfirmation: Boolean = false,
    val managementUrl: String? = null,
    val message: String? = null,
    val error: String? = null,
    val selectedLanguage: String = "pt",
)
