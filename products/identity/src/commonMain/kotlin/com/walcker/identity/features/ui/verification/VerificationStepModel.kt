package com.walcker.identity.features.ui.verification

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.identity.api.VerificationStage
import com.walcker.identity.api.verificationStage
import com.walcker.identity.features.domain.repository.VerificationRepository
import com.walcker.match.core.analytics.CrashReporter
import kotlinx.coroutines.launch

internal class VerificationStepModel(
    private val verificationRepository: VerificationRepository,
    private val crashReporter: CrashReporter,
) : StateScreenModel<VerificationStage?>(null) {
    init {
        screenModelScope.launch {
            verificationRepository.currentUser.collect { session ->
                mutableState.value = session?.verificationStage()
            }
        }
    }

    fun onResumed() {
        screenModelScope.launch {
            verificationRepository.refreshSession().onFailure { error ->
                crashReporter.recordException(error)
            }
        }
    }
}
