package com.walcker.identity.features.ui.profile

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.features.domain.usecase.DeleteAccountResult
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCase
import com.walcker.identity.features.domain.usecase.ProfileAccountUseCase
import com.walcker.identity.features.domain.usecase.SignUseCase
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.firestore.FirestoreClient
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
    private val firestore: FirestoreClient,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : StateScreenModel<ProfileState>(ProfileState()),
    KoinComponent {
    private val languageProvider: LanguageProvider? =
        try {
            inject<LanguageProvider>().value
        } catch (e: Exception) {
            crashReporter.log("LanguageProvider indisponível: ${e.message}")
            null
        }

    init {
        screenModelScope.launch {
            signUseCase.observeSession().collect { session ->
                mutableState.value = mutableState.value.copy(userSession = session)
                session?.uid?.let { loadReputation(it) }
            }
        }
        screenModelScope.launch {
            profileAccountUseCase.observeSubscription().collect { subscription ->
                mutableState.value =
                    mutableState.value.copy(
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
        analytics.track(AnalyticsEvent.AccountUpgradeClicked())
        val destination = identityDestination.paywall()
        navigatorHolder.navigator?.push(destination)
    }

    fun onSignOutClicked() {
        val strings = stringsHolder.strings.profile
        mutableState.value = mutableState.value.copy(isLoading = true, error = null, message = null)
        screenModelScope.launch {
            signUseCase
                .signOut()
                .onSuccess {
                    analytics.track(AnalyticsEvent.SignedOut())
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    navigatorHolder.navigator?.pop()
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    mutableState.value =
                        mutableState.value.copy(
                            isLoading = false,
                            error = strings.signOutError,
                        )
                }
        }
    }

    fun onDeleteAccountClicked() {
        mutableState.value =
            mutableState.value.copy(
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
        mutableState.value =
            mutableState.value.copy(
                isDeletingAccount = true,
                showDeleteAccountConfirmation = false,
                error = null,
                message = null,
            )
        screenModelScope.launch {
            when (val result = deleteAccountUseCase()) {
                DeleteAccountResult.Success -> {
                    analytics.track(AnalyticsEvent.AccountDeleted())
                    mutableState.value = mutableState.value.copy(isDeletingAccount = false)
                    navigatorHolder.navigator?.pop()
                }
                DeleteAccountResult.RequiresRecentLogin -> {
                    mutableState.value =
                        mutableState.value.copy(
                            isDeletingAccount = false,
                            error = strings.deleteAccountRequiresRecentLogin,
                        )
                }
                is DeleteAccountResult.RemoteDataFailure -> {
                    crashReporter.recordException(result.cause)
                    mutableState.value =
                        mutableState.value.copy(
                            isDeletingAccount = false,
                            error = strings.deleteAccountError,
                        )
                }
                is DeleteAccountResult.AuthDeletionFailure -> {
                    crashReporter.recordException(result.cause)
                    mutableState.value =
                        mutableState.value.copy(
                            isDeletingAccount = false,
                            error = strings.deleteAccountError,
                        )
                }
                is DeleteAccountResult.LocalCleanupFailure -> {
                    crashReporter.recordException(result.cause)
                    mutableState.value =
                        mutableState.value.copy(
                            isDeletingAccount = false,
                            error = strings.deleteAccountError,
                        )
                }
            }
        }
    }

    fun onRestorePurchasesClicked() {
        val strings = stringsHolder.strings.profile
        mutableState.value =
            mutableState.value.copy(
                isRestoringPurchases = true,
                error = null,
                message = null,
            )
        screenModelScope.launch {
            profileAccountUseCase
                .restorePurchases()
                .onSuccess { hasProAccess ->
                    analytics.track(AnalyticsEvent.PurchaseRestored(success = true))
                    mutableState.value =
                        mutableState.value.copy(
                            isRestoringPurchases = false,
                            message =
                                if (hasProAccess) {
                                    strings.restorePurchasesSuccess
                                } else {
                                    strings.restorePurchasesNoneFound
                                },
                        )
                }.onFailure { error ->
                    analytics.track(AnalyticsEvent.PurchaseRestored(success = false))
                    crashReporter.recordException(error)
                    mutableState.value =
                        mutableState.value.copy(
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

    private fun loadReputation(uid: String) {
        screenModelScope.launch {
            val profile = firestore.document("profiles/$uid").get().getOrNull()
            val count = (profile?.getLong("ratingCount") ?: 0L).toInt()
            if (count > 0) {
                val rating = profile?.getDouble("rating")?.toFloat() ?: 0f
                mutableState.value =
                    mutableState.value.copy(
                        reputationRating = rating,
                        reputationCount = count,
                    )
            }
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
