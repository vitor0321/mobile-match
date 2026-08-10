package com.walcker.identity.features.ui.signup

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.domain.error.signUpMessage
import com.walcker.identity.features.domain.usecase.SignUseCase
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.navigation.NavigatorHolder
import kotlinx.coroutines.launch

internal class SignUpStepModel(
    private val signUseCase: SignUseCase,
    private val navigatorHolder: NavigatorHolder,
    private val stringsHolder: IdentityStringsHolder,
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
            is SignUpInternalRoute.OnConfirmPasswordChanged -> updateConfirmPassword(event.value)
            is SignUpInternalRoute.OnEmailChanged -> updateEmail(event.value)
            is SignUpInternalRoute.OnPasswordChanged -> updatePassword(event.value)
        }
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
        val email = mutableState.value.email.trim()
        val password = mutableState.value.password
        val confirmPassword = mutableState.value.confirmPassword
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            mutableState.value = mutableState.value.copy(error = strings.blankFieldsError)
            return
        }
        if (password != confirmPassword) {
            mutableState.value = mutableState.value.copy(error = strings.passwordMismatchError)
            return
        }
        mutableState.value = mutableState.value.copy(isLoading = true, error = null, email = email)
        screenModelScope.launch {
            signUseCase.signUp(email = email, password = password)
                .onSuccess {
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    navigateBack()
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        error = when (error) {
                            IdentityError.Cancelled -> null
                            is IdentityError -> error.signUpMessage(strings)
                            else -> strings.signUpError
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
}
