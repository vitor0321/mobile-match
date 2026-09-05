package com.walcker.identity.ui.forgotpassword

import com.walcker.identity.fake.FakeAnalyticsTracker
import com.walcker.identity.fake.FakeAppleAuthSource
import com.walcker.identity.fake.FakeCrashReporter
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakeGoogleAuthSource
import com.walcker.identity.features.data.repository.AuthRepositoryImpl
import com.walcker.identity.features.data.usecase.SignUseCaseImpl
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordInternalRoute
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordStepModel
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.PtBrIdentityStrings
import com.walcker.match.core.navigation.NavigatorHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordStepModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val stringsHolder =
        IdentityStringsHolder().apply {
            setStrings(PtBrIdentityStrings)
        }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(
        firebaseAuthSource: FakeFirebaseAuthSource = FakeFirebaseAuthSource(),
    ): Pair<ForgotPasswordStepModel, FakeFirebaseAuthSource> {
        val authRepository =
            AuthRepositoryImpl(
                firebaseAuthSource = firebaseAuthSource,
                googleAuthSource = FakeGoogleAuthSource(),
                appleAuthSource = FakeAppleAuthSource(),
            )
        val signUseCase = SignUseCaseImpl(authRepository = authRepository)
        val model =
            ForgotPasswordStepModel(
                signUseCase = signUseCase,
                navigatorHolder = NavigatorHolder(),
                stringsHolder = stringsHolder,
                analytics = FakeAnalyticsTracker(),
                crashReporter = FakeCrashReporter(),
            )
        return model to firebaseAuthSource
    }

    @Test
    fun `When email changes should update state then email is stored`() {
        val (model, _) = buildModel()

        model.onEvent(ForgotPasswordInternalRoute.OnEmailChanged("user@match.app"))

        assertEquals("user@match.app", model.state.value.email)
        assertFalse(model.state.value.isSuccess)
    }

    @Test
    fun `When submit is clicked with blank email should expose validation error then loading stays false`() =
        runTest(testDispatcher) {
            val (model, _) = buildModel()

            model.onEvent(ForgotPasswordInternalRoute.OnSubmitClicked)
            advanceUntilIdle()

            assertEquals("Preencha o e-mail", model.state.value.error)
            assertFalse(model.state.value.isLoading)
            assertFalse(model.state.value.isSuccess)
        }

    @Test
    fun `When send recovery email succeeds should set success state and clear loading`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource()
            firebaseSource.setSendPasswordResetEmailResult(Result.success(Unit))
            val (model, source) = buildModel(firebaseAuthSource = firebaseSource)

            model.onEvent(ForgotPasswordInternalRoute.OnEmailChanged("user@match.app"))
            model.onEvent(ForgotPasswordInternalRoute.OnSubmitClicked)
            advanceUntilIdle()

            assertEquals("user@match.app", source.lastSendPasswordResetEmailInput)
            assertFalse(model.state.value.isLoading)
            assertTrue(model.state.value.isSuccess)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When send recovery email fails should expose error then loading finishes`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource()
            firebaseSource.setSendPasswordResetEmailResult(Result.failure(IllegalStateException("Erro ao enviar email")))
            val (model, _) = buildModel(firebaseAuthSource = firebaseSource)

            model.onEvent(ForgotPasswordInternalRoute.OnEmailChanged("user@match.app"))
            model.onEvent(ForgotPasswordInternalRoute.OnSubmitClicked)
            advanceUntilIdle()

            assertEquals("Não foi possível enviar o e-mail de recuperação", model.state.value.error)
            assertFalse(model.state.value.isLoading)
            assertFalse(model.state.value.isSuccess)
        }

    @Test
    fun `When error is dismissed should clear error then state has no error`() {
        val (model, _) = buildModel()

        model.onEvent(ForgotPasswordInternalRoute.OnEmailChanged(""))
        model.onEvent(ForgotPasswordInternalRoute.OnSubmitClicked)
        model.onEvent(ForgotPasswordInternalRoute.OnErrorDismissed)

        assertNull(model.state.value.error)
    }
}
