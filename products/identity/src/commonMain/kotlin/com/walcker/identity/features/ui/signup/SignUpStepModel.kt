package com.walcker.identity.features.ui.signup

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.domain.error.appleSignInMessage
import com.walcker.identity.features.domain.error.googleSignInMessage
import com.walcker.identity.features.domain.error.signUpMessage
import com.walcker.identity.features.domain.usecase.SignUseCase
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.navigation.NavigatorHolder
import kotlinx.coroutines.launch

internal class SignUpStepModel(
    private val signUseCase: SignUseCase,
    private val navigatorHolder: NavigatorHolder,
    private val stringsHolder: IdentityStringsHolder,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : StateScreenModel<SignUpState>(SignUpState()) {
    private var navigationHandled = false

    init {
        screenModelScope.launch {
            signUseCase.observeSession().collect { session ->
                if (session != null && !navigationHandled) {
                    navigateBack()
                }
            }
        }
    }

    fun onEvent(event: SignUpInternalRoute) {
        when (event) {
            SignUpInternalRoute.OnBackClicked -> navigateBack()
            SignUpInternalRoute.OnErrorDismissed -> clearError()
            SignUpInternalRoute.OnLoginClicked -> navigateBack()
            SignUpInternalRoute.OnSubmitClicked -> signUp()
            SignUpInternalRoute.OnGoogleSignInClicked -> signInWithGoogle()
            SignUpInternalRoute.OnAppleSignInClicked -> signInWithApple()
            is SignUpInternalRoute.OnConfirmPasswordChanged -> updateConfirmPassword(event.value)
            is SignUpInternalRoute.OnFullNameChanged -> updateFullName(event.value)
            is SignUpInternalRoute.OnEmailChanged -> updateEmail(event.value)
            is SignUpInternalRoute.OnPasswordChanged -> updatePassword(event.value)
        }
    }

    private fun updateFullName(value: String) {
        mutableState.value = mutableState.value.copy(fullName = value, error = null)
    }

    private fun updateEmail(value: String) {
        mutableState.value = mutableState.value.copy(email = value, error = null)
    }

    private fun updatePassword(value: String) {
        mutableState.value = mutableState.value.copy(password = value, error = null)
    }

    private fun updateConfirmPassword(value: String) {
        mutableState.value = mutableState.value.copy(confirmPassword = value, error = null)
    }

    private fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    private fun signUp() {
        val strings = stringsHolder.strings.signUp
        val fullName = mutableState.value.fullName.trim()
        val email = mutableState.value.email.trim()
        val password = mutableState.value.password
        val confirmPassword = mutableState.value.confirmPassword
        if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            mutableState.value = mutableState.value.copy(error = strings.blankFieldsError)
            return
        }
        if (password != confirmPassword) {
            mutableState.value = mutableState.value.copy(error = strings.passwordMismatchError)
            return
        }
        mutableState.value =
            mutableState.value.copy(isLoading = true, error = null, fullName = fullName, email = email)
        analytics.track(AnalyticsEvent.SignUpAttempted())
        screenModelScope.launch {
            signUseCase
                .signUp(email = email, password = password, displayName = fullName)
                .onSuccess {
                    analytics.track(AnalyticsEvent.SignUpResult(success = true))
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    navigateBack()
                }.onFailure { error ->
                    analytics.track(AnalyticsEvent.SignUpResult(success = false))
                    crashReporter.recordException(error)
                    mutableState.value =
                        mutableState.value.copy(
                            isLoading = false,
                            error =
                                when (error) {
                                    IdentityError.Cancelled -> null
                                    is IdentityError -> error.signUpMessage(strings)
                                    else -> strings.signUpError
                                },
                        )
                }
        }
    }

    private fun signInWithGoogle() {
        val strings = stringsHolder.strings.login
        mutableState.value = mutableState.value.copy(isLoading = true, error = null)
        analytics.track(AnalyticsEvent.LoginAttempted(LOGIN_METHOD_GOOGLE))
        screenModelScope.launch {
            signUseCase
                .signInWithGoogle()
                .onSuccess {
                    analytics.track(AnalyticsEvent.LoginResult(LOGIN_METHOD_GOOGLE, success = true))
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    navigateBack()
                }.onFailure { error ->
                    analytics.track(AnalyticsEvent.LoginResult(LOGIN_METHOD_GOOGLE, success = false))
                    crashReporter.recordException(error)
                    mutableState.value =
                        mutableState.value.copy(
                            isLoading = false,
                            error =
                                when (error) {
                                    IdentityError.Cancelled -> null
                                    is IdentityError -> error.googleSignInMessage(strings)
                                    else -> strings.googleSignInError
                                },
                        )
                }
        }
    }

    private fun signInWithApple() {
        val strings = stringsHolder.strings.login
        mutableState.value = mutableState.value.copy(isLoading = true, error = null)
        analytics.track(AnalyticsEvent.LoginAttempted(LOGIN_METHOD_APPLE))
        screenModelScope.launch {
            signUseCase
                .signInWithApple()
                .onSuccess {
                    analytics.track(AnalyticsEvent.LoginResult(LOGIN_METHOD_APPLE, success = true))
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    navigateBack()
                }.onFailure { error ->
                    analytics.track(AnalyticsEvent.LoginResult(LOGIN_METHOD_APPLE, success = false))
                    crashReporter.recordException(error)
                    mutableState.value =
                        mutableState.value.copy(
                            isLoading = false,
                            error =
                                when (error) {
                                    IdentityError.Cancelled -> null
                                    is IdentityError -> error.appleSignInMessage(strings)
                                    else -> strings.appleSignInError
                                },
                        )
                }
        }
    }

    private fun navigateBack() {
        if (navigationHandled) return
        navigationHandled = true
        navigatorHolder.navigator?.pop()
    }

    private companion object {
        const val LOGIN_METHOD_GOOGLE = "google"
        const val LOGIN_METHOD_APPLE = "apple"
    }
}
