package com.walcker.identity

import com.walcker.identity.features.ui.login.LoginStep
import com.walcker.identity.features.ui.paywall.PaywallStep
import com.walcker.identity.features.ui.profile.ProfileStep
import com.walcker.identity.features.ui.signup.SignUpStep
import com.walcker.match.navigator.IdentityDestination

internal class IdentityDestinationImpl : IdentityDestination {
    override fun login() = LoginStep()

    override fun signUp() = SignUpStep()

    override fun paywall() = PaywallStep()

    override fun profile() = ProfileStep()
}
