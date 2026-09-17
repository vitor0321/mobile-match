package com.walcker.identity.fake

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.walcker.match.navigator.IdentityDestination

internal class FakeIdentityDestination : IdentityDestination {
    var loginCallCount: Int = 0
        private set

    var signUpCallCount: Int = 0
        private set

    var verificationCallCount: Int = 0
        private set

    var changePhoneCallCount: Int = 0
        private set

    override fun login(): Screen {
        loginCallCount += 1
        return TestIdentityScreen("login")
    }

    override fun signUp(): Screen {
        signUpCallCount += 1
        return TestIdentityScreen("signup")
    }

    override fun verification(): Screen {
        verificationCallCount += 1
        return TestIdentityScreen("verification")
    }

    override fun changePhone(): Screen {
        changePhoneCallCount += 1
        return TestIdentityScreen("change-phone")
    }
}

private data class TestIdentityScreen(
    private val name: String,
) : Screen {
    @Composable
    override fun Content() = Unit
}
