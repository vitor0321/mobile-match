package com.walcker.identity.ui.signup

import com.walcker.identity.api.UserSession
import com.walcker.identity.features.data.repository.AuthRepositoryImpl
import com.walcker.identity.features.data.usecase.SignUseCaseImpl
import com.walcker.identity.features.ui.signup.SignUpInternalRoute
import com.walcker.identity.features.ui.signup.SignUpStepModel
import com.walcker.identity.fake.FakeAppleAuthSource
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakeGoogleAuthSource
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
class SignUpStepModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val stringsHolder = IdentityStringsHolder().apply {
        setStrings(PtBrIdentityStrings)
    }

    private val userSession = UserSession(
        uid = "uid-1",
        email = "user@match.app",
        displayName = "Match User",
    )

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
    ): Pair<SignUpStepModel, FakeFirebaseAuthSource> {
        val authRepository = AuthRepositoryImpl(
            firebaseAuthSource = firebaseAuthSource,
            googleAuthSource = FakeGoogleAuthSource(),
            appleAuthSource = FakeAppleAuthSource(),
        )
        val signUseCase = SignUseCaseImpl(authRepository = authRepository)
        val model = SignUpStepModel(
            signUseCase = signUseCase,
            navigatorHolder = NavigatorHolder(),
            stringsHolder = stringsHolder,
        )
        return model to firebaseAuthSource
    }

    @Test
    fun `When email changes should update state then email is stored`() {
        val (model, _) = buildModel()

        model.onEvent(SignUpInternalRoute.OnEmailChanged("user@match.app"))

        assertEquals("user@match.app", model.state.value.email)
    }

    @Test
    fun `When password changes should update state then password is stored`() {
        val (model, _) = buildModel()

        model.onEvent(SignUpInternalRoute.OnPasswordChanged("123456"))

        assertEquals("123456", model.state.value.password)
    }

    @Test
    fun `When confirm password changes should update state then confirmation is stored`() {
        val (model, _) = buildModel()

        model.onEvent(SignUpInternalRoute.OnConfirmPasswordChanged("123456"))

        assertEquals("123456", model.state.value.confirmPassword)
    }

    @Test
    fun `When submit is clicked with blank fields should expose validation error then loading stays false`() = runTest(testDispatcher) {
        val (model, _) = buildModel()

        model.onEvent(SignUpInternalRoute.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals("Preencha e-mail, senha e confirmação de senha", model.state.value.error)
        assertFalse(model.state.value.isLoading)
    }

    @Test
    fun `When submit is clicked with different passwords should expose mismatch error then loading stays false`() = runTest(testDispatcher) {
        val (model, _) = buildModel()

        model.onEvent(SignUpInternalRoute.OnEmailChanged("user@match.app"))
        model.onEvent(SignUpInternalRoute.OnPasswordChanged("123456"))
        model.onEvent(SignUpInternalRoute.OnConfirmPasswordChanged("654321"))
        model.onEvent(SignUpInternalRoute.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals("As senhas não coincidem", model.state.value.error)
        assertFalse(model.state.value.isLoading)
    }

    @Test
    fun `When sign up succeeds should clear loading then source receives credentials`() = runTest(testDispatcher) {
        val firebaseSource = FakeFirebaseAuthSource(signUpResult = Result.success(userSession))
        val (model, source) = buildModel(firebaseAuthSource = firebaseSource)

        model.onEvent(SignUpInternalRoute.OnEmailChanged("user@match.app"))
        model.onEvent(SignUpInternalRoute.OnPasswordChanged("123456"))
        model.onEvent(SignUpInternalRoute.OnConfirmPasswordChanged("123456"))
        model.onEvent(SignUpInternalRoute.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals("user@match.app" to "123456", source.lastSignUpInput)
        assertFalse(model.state.value.isLoading)
        assertNull(model.state.value.error)
    }

    @Test
    fun `When sign up is submitted should trim email then normalized value is stored and sent`() = runTest(testDispatcher) {
        val firebaseSource = FakeFirebaseAuthSource(signUpResult = Result.success(userSession))
        val (model, source) = buildModel(firebaseAuthSource = firebaseSource)

        model.onEvent(SignUpInternalRoute.OnEmailChanged("  user@match.app  "))
        model.onEvent(SignUpInternalRoute.OnPasswordChanged("123456"))
        model.onEvent(SignUpInternalRoute.OnConfirmPasswordChanged("123456"))
        model.onEvent(SignUpInternalRoute.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals("user@match.app" to "123456", source.lastSignUpInput)
        assertEquals("user@match.app", model.state.value.email)
    }

    @Test
    fun `When sign up fails should expose error then loading finishes`() = runTest(testDispatcher) {
        val firebaseSource = FakeFirebaseAuthSource(
            signUpResult = Result.failure(IllegalStateException("Falha no cadastro")),
        )
        val (model, _) = buildModel(firebaseAuthSource = firebaseSource)

        model.onEvent(SignUpInternalRoute.OnEmailChanged("user@match.app"))
        model.onEvent(SignUpInternalRoute.OnPasswordChanged("123456"))
        model.onEvent(SignUpInternalRoute.OnConfirmPasswordChanged("123456"))
        model.onEvent(SignUpInternalRoute.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals("Não foi possível criar a conta", model.state.value.error)
        assertFalse(model.state.value.isLoading)
    }

    @Test
    fun `When error is dismissed should clear error then state has no error`() {
        val (model, _) = buildModel()

        model.onEvent(SignUpInternalRoute.OnSubmitClicked)
        model.onEvent(SignUpInternalRoute.OnErrorDismissed)

        assertNull(model.state.value.error)
    }

    @Test
    fun `When session already exists should keep state stable then form remains idle`() = runTest(testDispatcher) {
        val firebaseSource = FakeFirebaseAuthSource(initialUser = userSession)
        val (model, _) = buildModel(firebaseAuthSource = firebaseSource)
        advanceUntilIdle()

        assertFalse(model.state.value.isLoading)
        assertNull(model.state.value.error)
        assertTrue(model.state.value.email.isEmpty())
    }
}
