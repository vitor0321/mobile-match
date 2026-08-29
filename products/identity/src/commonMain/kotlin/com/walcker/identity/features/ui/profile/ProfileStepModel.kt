package com.walcker.identity.features.ui.profile

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.features.domain.usecase.DeleteAccountResult
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCase
import com.walcker.identity.features.domain.usecase.ProfileAccountUseCase
import com.walcker.identity.features.domain.usecase.SignUseCase
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.navigator.IdentityDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface LanguageProvider {
    val selectedLanguage: Flow<String>
    suspend fun saveLanguage(language: String)
}

internal class ProfileStepModel(
    private val signUseCase: SignUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val profileAccountUseCase: ProfileAccountUseCase,
    private val navigatorHolder: NavigatorHolder,
    private val identityDestination: IdentityDestination,
    private val stringsHolder: IdentityStringsHolder,
) : StateScreenModel<ProfileState>(ProfileState()), KoinComponent {

    private val languageProvider: LanguageProvider? = try {
        inject<LanguageProvider>().value
    } catch (e: Exception) {
        null
    }

    init {
        screenModelScope.launch {
            signUseCase.observeSession().collect { session ->
                mutableState.value = mutableState.value.copy(userSession = session)
            }
        }
        screenModelScope.launch {
            profileAccountUseCase.observeSubscription().collect { subscription ->
                mutableState.value = mutableState.value.copy(
                    isPro = subscription.isPro,
                    managementUrl = subscription.managementUrl,
                )
            }
        }
        if (languageProvider != null) {
            observeLanguageChanges()
        }
    }

    fun onBackClicked() {
        navigatorHolder.navigator?.pop()
    }

    fun onUpgradeToProClicked() {
        val destination = identityDestination.paywall()
        navigatorHolder.navigator?.push(destination)
    }

    fun onSignOutClicked() {
        val strings = stringsHolder.strings.profile
        mutableState.value = mutableState.value.copy(isLoading = true, error = null, message = null)
        screenModelScope.launch {
            signUseCase.signOut()
                .onSuccess {
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    navigatorHolder.navigator?.pop()
                }
                .onFailure {
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        error = strings.signOutError,
                    )
                }
        }
    }

    fun onDeleteAccountClicked() {
        mutableState.value = mutableState.value.copy(
            showDeleteAccountConfirmation = true,
            error = null,
            message = null,
        )
    }

    fun onDeleteAccountConfirmationDismissed() {
        mutableState.value = mutableState.value.copy(showDeleteAccountConfirmation = false)
    }

    fun onDeleteAccountConfirmed() {
        val strings = stringsHolder.strings.profile
        mutableState.value = mutableState.value.copy(
            isDeletingAccount = true,
            showDeleteAccountConfirmation = false,
            error = null,
            message = null,
        )
        screenModelScope.launch {
            when (deleteAccountUseCase()) {
                DeleteAccountResult.Success -> {
                    mutableState.value = mutableState.value.copy(isDeletingAccount = false)
                    navigatorHolder.navigator?.pop()
                }
                DeleteAccountResult.RequiresRecentLogin -> {
                    mutableState.value = mutableState.value.copy(
                        isDeletingAccount = false,
                        error = strings.deleteAccountRequiresRecentLogin,
                    )
                }
                is DeleteAccountResult.RemoteDataFailure,
                is DeleteAccountResult.AuthDeletionFailure,
                is DeleteAccountResult.LocalCleanupFailure,
                -> {
                    mutableState.value = mutableState.value.copy(
                        isDeletingAccount = false,
                        error = strings.deleteAccountError,
                    )
                }
            }
        }
    }

    fun onRestorePurchasesClicked() {
        val strings = stringsHolder.strings.profile
        mutableState.value = mutableState.value.copy(
            isRestoringPurchases = true,
            error = null,
            message = null,
        )
        screenModelScope.launch {
            profileAccountUseCase.restorePurchases()
                .onSuccess { hasProAccess ->
                    mutableState.value = mutableState.value.copy(
                        isRestoringPurchases = false,
                        message = if (hasProAccess) {
                            strings.restorePurchasesSuccess
                        } else {
                            strings.restorePurchasesNoneFound
                        },
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isRestoringPurchases = false,
                        error = strings.restorePurchasesError,
                    )
                }
        }
    }

    fun onLanguageSelected(language: String) {
        screenModelScope.launch {
            mutableState.value = mutableState.value.copy(selectedLanguage = language)
            languageProvider?.saveLanguage(language)
        }
    }

    private fun observeLanguageChanges() {
        screenModelScope.launch {
            languageProvider?.selectedLanguage?.collect { language ->
                mutableState.value = mutableState.value.copy(selectedLanguage = language)
            }
        }
    }
}
