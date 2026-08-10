package com.walcker.identity.features.domain.usecase

import com.walcker.identity.api.ProStateHolder
import com.walcker.identity.features.domain.billing.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class ProfileSubscriptionState(
    val isPro: Boolean,
    val managementUrl: String?,
)

internal interface ProfileAccountUseCase {
    fun observeSubscription(): Flow<ProfileSubscriptionState>
    suspend fun restorePurchases(): Result<Boolean>
}

internal class ProfileAccountUseCaseImpl(
    private val proStateHolder: ProStateHolder,
    private val billingRepository: BillingRepository,
) : ProfileAccountUseCase {
    override fun observeSubscription(): Flow<ProfileSubscriptionState> = proStateHolder.isPro.map { isPro ->
        ProfileSubscriptionState(
            isPro = isPro,
            managementUrl = if (isPro) billingRepository.managementUrl().getOrNull() else null,
        )
    }

    override suspend fun restorePurchases(): Result<Boolean> = billingRepository.restore()
}
