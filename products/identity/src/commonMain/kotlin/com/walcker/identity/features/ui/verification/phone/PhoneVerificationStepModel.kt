package com.walcker.identity.features.ui.verification.phone

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.api.LogoutService
import com.walcker.identity.features.domain.error.verificationMessage
import com.walcker.identity.features.domain.phone.CountryDialCode
import com.walcker.identity.features.domain.phone.PhoneCodeResult
import com.walcker.identity.features.domain.phone.PhoneCredentialPurpose
import com.walcker.identity.features.domain.phone.PhoneVerificationSession
import com.walcker.identity.features.domain.phone.countryByIsoCode
import com.walcker.identity.features.domain.phone.countryForE164
import com.walcker.identity.features.domain.phone.digitsOnly
import com.walcker.identity.features.domain.phone.maxNationalDigits
import com.walcker.identity.features.domain.phone.nationalDigits
import com.walcker.identity.features.domain.phone.toE164
import com.walcker.identity.features.domain.repository.VerificationRepository
import com.walcker.identity.features.ui.verification.launchResendCooldown
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.resolveStringsOrDefault
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PhoneVerificationStepModel(
    private val verificationRepository: VerificationRepository,
    private val logoutService: LogoutService,
    private val stringsHolder: IdentityStringsHolder,
    private val crashReporter: CrashReporter,
    initialCountry: CountryDialCode,
    private val mode: PhoneVerificationMode,
    private val regionIsoCode: String?,
) : StateScreenModel<PhoneVerificationState>(PhoneVerificationState(country = initialCountry)) {
    private var verificationSession: PhoneVerificationSession? = null
    private var cooldownJob: Job? = null
    private var requestJob: Job? = null
    private var currentE164: String? = null

    init {
        if (mode == PhoneVerificationMode.Change) observeCurrentPhone()
    }

    private fun observeCurrentPhone() {
        screenModelScope.launch {
            var countryApplied = false
            verificationRepository.currentUser.collect { session ->
                currentE164 = session?.phoneNumber
                if (countryApplied) return@collect
                val country = countryForE164(currentE164, regionIsoCode) ?: return@collect
                countryApplied = true
                mutableState.update { it.copy(country = country) }
            }
        }
    }

    fun onEvent(event: PhoneVerificationInternalRoute) {
        when (event) {
            is PhoneVerificationInternalRoute.OnCountrySelected -> selectCountry(event.isoCode)
            is PhoneVerificationInternalRoute.OnNationalNumberChanged -> updateNationalNumber(event.value)
            PhoneVerificationInternalRoute.OnSendCodeClicked -> sendCodeForTypedNumber()
            is PhoneVerificationInternalRoute.OnCodeChanged ->
                mutableState.update { it.copy(code = digitsOnly(event.value).take(PHONE_CODE_LENGTH), message = null) }
            PhoneVerificationInternalRoute.OnConfirmCodeClicked -> confirmCode()
            PhoneVerificationInternalRoute.OnResendClicked -> resend()
            PhoneVerificationInternalRoute.OnChangeNumberClicked -> changeNumber()
            PhoneVerificationInternalRoute.OnLogoutClicked -> logout()
            PhoneVerificationInternalRoute.OnMessageDismissed -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun selectCountry(isoCode: String) {
        val country = countryByIsoCode(isoCode) ?: return
        mutableState.update {
            val digits = nationalDigits(country, it.nationalNumber)
            it.copy(
                country = country,
                nationalNumber = if (digits.length > maxNationalDigits(country)) "" else digits,
                message = null,
            )
        }
    }

    private fun updateNationalNumber(value: String) {
        mutableState.update {
            val digits = nationalDigits(it.country, value)
            if (digits.length > maxNationalDigits(it.country)) it else it.copy(nationalNumber = digits, message = null)
        }
    }

    private fun sendCodeForTypedNumber() {
        val current = mutableState.value
        if (current.phase != PhoneVerificationPhase.Number || current.isLoading) return
        val e164 = toE164(current.country, current.nationalNumber)
        val strings = stringsHolder.resolveStringsOrDefault().verification
        if (e164 == null) {
            mutableState.update { it.copy(message = strings.errorInvalidPhoneNumber) }
            return
        }
        if (mode == PhoneVerificationMode.Change && e164 == currentE164) {
            mutableState.update { it.copy(message = strings.errorSamePhoneNumber) }
            return
        }
        sendCode(e164 = e164, resend = null)
    }

    private fun resend() {
        val current = mutableState.value
        if (current.resendAvailableInSeconds > 0 || current.isLoading) return
        val e164 = current.sentToE164 ?: return
        sendCode(e164 = e164, resend = verificationSession)
    }

    private fun sendCode(
        e164: String,
        resend: PhoneVerificationSession?,
    ) {
        val strings = stringsHolder.resolveStringsOrDefault().verification
        mutableState.update { it.copy(isLoading = true, message = null) }
        requestJob =
            screenModelScope.launch {
                verificationRepository
                    .sendPhoneCode(e164, resend, credentialPurpose())
                    .onSuccess { result ->
                        when (result) {
                            is PhoneCodeResult.CodeSent -> {
                                verificationSession = result.session
                                mutableState.update {
                                    it.copy(isLoading = false, phase = PhoneVerificationPhase.Code, sentToE164 = e164, code = "")
                                }
                                startCooldown()
                            }
                            is PhoneCodeResult.AutoVerified -> markVerified()
                        }
                    }.onFailure { error ->
                        crashReporter.recordException(error)
                        mutableState.update { it.copy(isLoading = false, message = error.verificationMessage(strings)) }
                    }
            }
    }

    private fun confirmCode() {
        val strings = stringsHolder.resolveStringsOrDefault().verification
        val current = mutableState.value
        if (current.phase != PhoneVerificationPhase.Code || current.isLoading) return
        val session = verificationSession ?: return
        if (current.code.length != PHONE_CODE_LENGTH) {
            mutableState.update { it.copy(message = strings.errorIncompleteCode) }
            return
        }
        mutableState.update { it.copy(isLoading = true, message = null) }
        requestJob =
            screenModelScope.launch {
                verificationRepository
                    .confirmPhoneCode(session, current.code)
                    .onSuccess { markVerified() }
                    .onFailure { error ->
                        crashReporter.recordException(error)
                        mutableState.update { it.copy(isLoading = false, message = error.verificationMessage(strings)) }
                    }
            }
    }

    private fun credentialPurpose(): PhoneCredentialPurpose =
        when (mode) {
            PhoneVerificationMode.Verify -> PhoneCredentialPurpose.Link
            PhoneVerificationMode.Change -> PhoneCredentialPurpose.Replace
        }

    private fun markVerified() {
        mutableState.update { it.copy(isLoading = false, isPhoneChanged = mode == PhoneVerificationMode.Change) }
    }

    private fun changeNumber() {
        requestJob?.cancel()
        requestJob = null
        cooldownJob?.cancel()
        cooldownJob = null
        verificationSession = null
        mutableState.update {
            it.copy(
                phase = PhoneVerificationPhase.Number,
                code = "",
                isLoading = false,
                sentToE164 = null,
                resendAvailableInSeconds = 0,
                message = null,
            )
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob =
            screenModelScope.launchResendCooldown { remaining ->
                mutableState.update { it.copy(resendAvailableInSeconds = remaining) }
            }
    }

    private fun logout() {
        val strings = stringsHolder.resolveStringsOrDefault().verification
        screenModelScope.launch {
            logoutService.logout().onFailure { error ->
                crashReporter.recordException(error)
                mutableState.update { it.copy(message = strings.errorGeneric) }
            }
        }
    }
}
