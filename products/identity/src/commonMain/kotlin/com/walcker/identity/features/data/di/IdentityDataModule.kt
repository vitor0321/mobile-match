package com.walcker.identity.features.data.di

import com.walcker.identity.api.LogoutService
import com.walcker.identity.api.ProStateHolder
import com.walcker.identity.api.SessionHolder
import com.walcker.identity.features.data.billing.BillingClient
import com.walcker.identity.features.data.billing.BillingRepositoryImpl
import com.walcker.identity.features.data.billing.RevenueCatBillingClient
import com.walcker.identity.features.data.pro.DataStoreProStateCache
import com.walcker.identity.features.data.pro.ProStateCache
import com.walcker.identity.features.data.pro.ProStateHolderImpl
import com.walcker.identity.features.data.platform.IdentityPlatformServices
import com.walcker.identity.features.data.remote.createAccountDeletionCallableSource
import com.walcker.identity.features.data.remote.createAppleAuthSource
import com.walcker.identity.features.data.remote.createFirebaseAuthSource
import com.walcker.identity.features.data.repository.AccountDeletionRepositoryImpl
import com.walcker.identity.features.data.repository.AuthRepositoryImpl
import com.walcker.identity.features.data.service.LogoutServiceImpl
import com.walcker.identity.features.data.session.SessionHolderImpl
import com.walcker.identity.features.data.usecase.SignUseCaseImpl
import com.walcker.identity.features.domain.billing.BillingRepository
import com.walcker.identity.features.domain.repository.AccountDeletionRepository
import com.walcker.identity.features.domain.repository.AuthRepository
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCase
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCaseImpl
import com.walcker.identity.features.domain.usecase.ProfileAccountUseCase
import com.walcker.identity.features.domain.usecase.ProfileAccountUseCaseImpl
import com.walcker.identity.features.domain.usecase.SignUseCase
import com.walcker.match.core.dispatchers.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val identityDataModule = module {
    single { get<IdentityPlatformServices>().proStateDataStore() }
    single<ProStateCache> { DataStoreProStateCache(dataStore = get()) }
    single<BillingClient> { RevenueCatBillingClient() }
    single<BillingRepository> { BillingRepositoryImpl(billingClient = get()) }
    single { createFirebaseAuthSource(stringsHolder = get()) }
    single { createAccountDeletionCallableSource() }
    single<AccountDeletionRepository> { AccountDeletionRepositoryImpl(source = get()) }
    single { get<IdentityPlatformServices>().googleAuthSource() }
    single { createAppleAuthSource(stringsHolder = get()) }
    single<AuthRepository> {
        AuthRepositoryImpl(
            firebaseAuthSource = get(),
            googleAuthSource = get(),
            appleAuthSource = get(),
        )
    }
    single<SessionHolder> { SessionHolderImpl(authRepository = get()) }
    single<ProStateHolder> {
        ProStateHolderImpl(
            sessionHolder = get(),
            billingClient = get(),
            cache = get(),
            ioDispatcher = get<CoroutineDispatcher>(named(Dispatcher.IO)),
        )
    }
    factory<SignUseCase> { SignUseCaseImpl(authRepository = get()) }
    single<LogoutService> { LogoutServiceImpl(signUseCase = get()) }
    factory<ProfileAccountUseCase> {
        ProfileAccountUseCaseImpl(
            proStateHolder = get(),
            billingRepository = get(),
        )
    }
    factory<DeleteAccountUseCase> {
        DeleteAccountUseCaseImpl(
            accountDeletionRepository = get(),
            authRepository = get(),
            billingClient = get(),
            proStateCache = get(),
        )
    }
}
