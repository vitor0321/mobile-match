package com.walcker.identity.features.ui.verification.email

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.api.LogoutService
import com.walcker.identity.features.data.verification.EmailVerificationThrottle
import com.walcker.identity.features.domain.error.verificationMessage
import com.walcker.identity.features.domain.repository.VerificationRepository
import com.walcker.identity.features.ui.verification.launchResendCooldown
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class EmailVerificationStepModel(
    private val verificationRepository: VerificationRepository,
    private val throttle: EmailVerificationThrottle,
    private val logoutService: LogoutService,
    private val stringsHolder: IdentityStringsHolder,
    private val crashReporter: CrashReporter,
) : StateScreenModel<EmailVerificationState>(EmailVerificationState()) {
    private var currentUid: String? = null
    private var cooldownJob: Job? = null

    init {
        screenModelScope.launch {
            verificationRepository.currentUser.filterNotNull().collect { session ->
                mutableState.update { it.copy(email = session.email.orEmpty()) }
                if (currentUid == session.uid) return@collect
                currentUid = session.uid
                if (!session.isEmailVerified && throttle.shouldAutoSend(session.uid)) {
                    sendVerification(session.uid)
                }
            }
        }
    }

    fun onEvent(event: EmailVerificationInternalRoute) {
        when (event) {
            EmailVerificationInternalRoute.OnConfirmedClicked -> checkConfirmation()
            EmailVerificationInternalRoute.OnResendClicked -> resend()
            EmailVerificationInternalRoute.OnLogoutClicked -> logout()
            EmailVerificationInternalRoute.OnMessageDismissed -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun resend() {
        val current = mutableState.value
        if (current.resendAvailableInSeconds > 0 || current.isSending) return
        val uid = currentUid ?: return
        screenModelScope.launch { sendVerification(uid) }
    }

    private suspend fun sendVerification(uid: String) {
        val strings = stringsHolder.resolveStringsOrDefault().verification
        mutableState.update { it.copy(isSending = true, message = null) }
        verificationRepository
            .sendEmailVerification()
            .onSuccess {
                throttle.markSent(uid)
                mutableState.update { it.copy(isSending = false, message = strings.emailSent, isMessageError = false) }
                startCooldown()
            }.onFailure { error ->
                crashReporter.recordException(error)
                mutableState.update {
                    it.copy(isSending = false, message = error.verificationMessage(strings), isMessageError = true)
                }
            }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob =
            screenModelScope.launchResendCooldown { remaining ->
                mutableState.update { it.copy(resendAvailableInSeconds = remaining) }
            }
    }

    private fun checkConfirmation() {
        val strings = stringsHolder.resolveStringsOrDefault().verification
        mutableState.update { it.copy(isChecking = true, message = null) }
        screenModelScope.launch {
            verificationRepository
                .refreshSession()
                .onSuccess { session ->
                    mutableState.update {
                        if (session.isEmailVerified) {
                            it.copy(isChecking = false)
                        } else {
                            it.copy(isChecking = false, message = strings.emailNotConfirmedYet, isMessageError = true)
                        }
                    }
                }.onFailure { error ->
                    crashReporter.recordException(error)
                    mutableState.update {
                        it.copy(isChecking = false, message = error.verificationMessage(strings), isMessageError = true)
                    }
                }
        }
    }

    private fun logout() {
        val strings = stringsHolder.resolveStringsOrDefault().verification
        screenModelScope.launch {
            logoutService.logout().onFailure { error ->
                crashReporter.recordException(error)
                mutableState.update { it.copy(message = strings.errorGeneric, isMessageError = true) }
            }
        }
    }
}
