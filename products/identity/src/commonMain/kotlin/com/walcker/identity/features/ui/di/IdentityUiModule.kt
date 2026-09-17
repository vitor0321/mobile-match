package com.walcker.identity.features.ui.di

import com.walcker.identity.IdentityDestinationImpl
import com.walcker.identity.features.data.platform.deviceRegionIsoCode
import com.walcker.identity.features.domain.phone.defaultCountry
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordStepModel
import com.walcker.identity.features.ui.login.LoginStepModel
import com.walcker.identity.features.ui.signup.SignUpStepModel
import com.walcker.identity.features.ui.verification.VerificationStepModel
import com.walcker.identity.features.ui.verification.email.EmailVerificationStepModel
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationMode
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationStepModel
import com.walcker.match.navigator.IdentityDestination
import org.koin.dsl.module

internal val identityUiModule =
    module {
        factory {
            LoginStepModel(
                signUseCase = get(),
                navigatorHolder = get(),
                stringsHolder = get(),
                loginCoordinator = get(),
                analytics = get(),
                crashReporter = get(),
            )
        }
        factory {
            SignUpStepModel(
                signUseCase = get(),
                navigatorHolder = get(),
                stringsHolder = get(),
                analytics = get(),
                crashReporter = get(),
            )
        }
        factory {
            ForgotPasswordStepModel(
                signUseCase = get(),
                navigatorHolder = get(),
                stringsHolder = get(),
                analytics = get(),
                crashReporter = get(),
            )
        }
        factory {
            VerificationStepModel(
                verificationRepository = get(),
                crashReporter = get(),
            )
        }
        factory {
            EmailVerificationStepModel(
                verificationRepository = get(),
                throttle = get(),
                logoutService = get(),
                stringsHolder = get(),
                crashReporter = get(),
            )
        }
        factory { (mode: PhoneVerificationMode) ->
            val regionIsoCode = deviceRegionIsoCode()
            PhoneVerificationStepModel(
                verificationRepository = get(),
                logoutService = get(),
                stringsHolder = get(),
                crashReporter = get(),
                initialCountry = defaultCountry(regionIsoCode),
                mode = mode,
                regionIsoCode = regionIsoCode,
            )
        }
        single<IdentityDestination> { IdentityDestinationImpl() }
    }
