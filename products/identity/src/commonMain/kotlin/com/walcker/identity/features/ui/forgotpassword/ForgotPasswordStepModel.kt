package com.walcker.identity.features.ui.forgotpassword

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.domain.error.passwordResetMessage
import com.walcker.identity.features.domain.usecase.SignUseCase
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.match.core.navigation.NavigatorHolder
import kotlinx.coroutines.launch

internal class ForgotPasswordStepModel(
    private val signUseCase: SignUseCase,
    private val navigatorHolder: NavigatorHolder,
    private val stringsHolder: IdentityStringsHolder,
) : StateScreenModel<ForgotPasswordState>(ForgotPasswordState()) {

    fun onEvent(event: ForgotPasswordInternalRoute) {
        when (event) {
            ForgotPasswordInternalRoute.OnBackClicked -> navigateBack()
            ForgotPasswordInternalRoute.OnErrorDismissed -> clearError()
            ForgotPasswordInternalRoute.OnSubmitClicked -> sendRecoveryEmail()
            is ForgotPasswordInternalRoute.OnEmailChanged -> updateEmail(event.value)
        }
    }

    private fun updateEmail(value: String) {
        mutableState.value = mutableState.value.copy(email = value, error = null, isSuccess = false)
    }

    private fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    private fun sendRecoveryEmail() {
        val strings = stringsHolder.strings.forgotPassword
        val email = mutableState.value.email.trim()
        if (email.isBlank()) {
            mutableState.value = mutableState.value.copy(error = strings.blankEmailError)
            return
        }
        mutableState.value = mutableState.value.copy(isLoading = true, error = null, isSuccess = false, email = email)
        screenModelScope.launch {
            signUseCase.sendPasswordResetEmail(email = email)
                .onSuccess {
                    mutableState.value = mutableState.value.copy(isLoading = false, isSuccess = true)
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        error = when (error) {
                            IdentityError.Cancelled -> null
                            is IdentityError -> error.passwordResetMessage(strings)
                            else -> strings.sendEmailError
                        },
                    )
                }
        }
    }

    private fun navigateBack() {
        navigatorHolder.navigator?.pop()
    }
}
