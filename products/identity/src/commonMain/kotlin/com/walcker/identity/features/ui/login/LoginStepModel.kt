package com.walcker.identity.features.ui.login

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.domain.error.appleSignInMessage
import com.walcker.identity.features.domain.error.googleSignInMessage
import com.walcker.identity.features.domain.error.loginMessage
import com.walcker.identity.features.domain.usecase.SignUseCase
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordStep
import com.walcker.identity.features.ui.signup.SignUpStep
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.analytics.CrashReporter
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.navigator.LoginCoordinator
import kotlinx.coroutines.launch

internal class LoginStepModel(
    private val signUseCase: SignUseCase,
    private val navigatorHolder: NavigatorHolder,
    private val stringsHolder: IdentityStringsHolder,
    private val loginCoordinator: LoginCoordinator,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : StateScreenModel<LoginState>(LoginState()) {
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

    fun onEvent(event: LoginInternalRoute) {
        when (event) {
            LoginInternalRoute.OnBackClicked -> navigateBack()
            LoginInternalRoute.OnErrorDismissed -> clearError()
            LoginInternalRoute.OnGoogleSignInClicked -> signInWithGoogle()
            LoginInternalRoute.OnAppleSignInClicked -> signInWithApple()
            LoginInternalRoute.OnSignUpClicked -> openSignUp()
            LoginInternalRoute.OnForgotPasswordClicked -> openForgotPassword()
            LoginInternalRoute.OnSubmitClicked -> signInWithEmail()
            is LoginInternalRoute.OnEmailChanged -> updateEmail(event.value)
            is LoginInternalRoute.OnPasswordChanged -> updatePassword(event.value)
        }
    }

    private fun updateEmail(value: String) {
        mutableState.value = mutableState.value.copy(email = value, error = null)
    }

    private fun updatePassword(value: String) {
        mutableState.value = mutableState.value.copy(password = value, error = null)
    }

    private fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    private fun signInWithEmail() {
        val strings = stringsHolder.strings.login
        val email = mutableState.value.email.trim()
        val password = mutableState.value.password
        if (email.isBlank() || password.isBlank()) {
            mutableState.value = mutableState.value.copy(error = strings.blankFieldsError)
            return
        }
        mutableState.value = mutableState.value.copy(isLoading = true, error = null, email = email)
        analytics.track(AnalyticsEvent.LoginAttempted(LOGIN_METHOD_EMAIL))
        screenModelScope.launch {
            signUseCase
                .signInWithEmail(email = email, password = password)
                .onSuccess {
                    analytics.track(AnalyticsEvent.LoginResult(LOGIN_METHOD_EMAIL, success = true))
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    navigateBack()
                }.onFailure { error ->
                    analytics.track(AnalyticsEvent.LoginResult(LOGIN_METHOD_EMAIL, success = false))
                    crashReporter.recordException(error)
                    mutableState.value =
                        mutableState.value.copy(
                            isLoading = false,
                            error =
                                (error as? IdentityError)?.loginMessage(strings)
                                    ?: if (error is IdentityError.Cancelled) null else strings.signInError,
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

    private fun openSignUp() {
        navigatorHolder.navigator?.push(SignUpStep())
    }

    private fun openForgotPassword() {
        navigatorHolder.navigator?.push(ForgotPasswordStep())
    }

    private fun navigateBack() {
        if (navigationHandled) return
        navigationHandled = true
        val popped = navigatorHolder.navigator?.pop() ?: false
        if (!popped) {
            loginCoordinator.dismiss()
        }
    }

    private companion object {
        const val LOGIN_METHOD_EMAIL = "email"
        const val LOGIN_METHOD_GOOGLE = "google"
        const val LOGIN_METHOD_APPLE = "apple"
    }
}
