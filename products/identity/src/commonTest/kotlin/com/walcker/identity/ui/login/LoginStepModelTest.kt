package com.walcker.identity.ui.login

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeAppleAuthSource
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakeGoogleAuthSource
import com.walcker.identity.features.data.repository.AuthRepositoryImpl
import com.walcker.identity.features.data.usecase.SignUseCaseImpl
import com.walcker.identity.features.domain.error.IdentityError
import com.walcker.identity.features.ui.login.LoginInternalRoute
import com.walcker.identity.features.ui.login.LoginStepModel
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.PtBrIdentityStrings
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.navigator.LoginCoordinator
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
class LoginStepModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private val stringsHolder =
        IdentityStringsHolder().apply {
            setStrings(PtBrIdentityStrings)
        }

    private val userSession =
        UserSession(
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
        googleAuthSource: FakeGoogleAuthSource = FakeGoogleAuthSource(),
        appleAuthSource: FakeAppleAuthSource = FakeAppleAuthSource(),
    ): Pair<LoginStepModel, Sources> {
        val authRepository =
            AuthRepositoryImpl(
                firebaseAuthSource = firebaseAuthSource,
                googleAuthSource = googleAuthSource,
                appleAuthSource = appleAuthSource,
            )
        val signUseCase = SignUseCaseImpl(authRepository = authRepository)
        val model =
            LoginStepModel(
                signUseCase = signUseCase,
                navigatorHolder = NavigatorHolder(),
                stringsHolder = stringsHolder,
                loginCoordinator = LoginCoordinator(),
            )
        return model to
            Sources(
                firebase = firebaseAuthSource,
                google = googleAuthSource,
                apple = appleAuthSource,
            )
    }

    private data class Sources(
        val firebase: FakeFirebaseAuthSource,
        val google: FakeGoogleAuthSource,
        val apple: FakeAppleAuthSource,
    )

    @Test
    fun `When email changes should update state then email is stored`() {
        val (model, _) = buildModel()

        model.onEvent(LoginInternalRoute.OnEmailChanged("user@match.app"))

        assertEquals("user@match.app", model.state.value.email)
    }

    @Test
    fun `When password changes should update state then password is stored`() {
        val (model, _) = buildModel()

        model.onEvent(LoginInternalRoute.OnPasswordChanged("123456"))

        assertEquals("123456", model.state.value.password)
    }

    @Test
    fun `When submit is clicked with blank fields should expose validation error then loading stays false`() =
        runTest(testDispatcher) {
            val (model, _) = buildModel()

            model.onEvent(LoginInternalRoute.OnSubmitClicked)
            advanceUntilIdle()

            assertEquals("Preencha e-mail e senha", model.state.value.error)
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `When email sign in succeeds should clear loading then source receives credentials`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource(signInResult = Result.success(userSession))
            val (model, sources) = buildModel(firebaseAuthSource = firebaseSource)

            model.onEvent(LoginInternalRoute.OnEmailChanged("user@match.app"))
            model.onEvent(LoginInternalRoute.OnPasswordChanged("123456"))
            model.onEvent(LoginInternalRoute.OnSubmitClicked)
            advanceUntilIdle()

            assertEquals("user@match.app" to "123456", sources.firebase.lastSignInInput)
            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When email sign in is submitted should trim email then normalized value is stored and sent`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource(signInResult = Result.success(userSession))
            val (model, sources) = buildModel(firebaseAuthSource = firebaseSource)

            model.onEvent(LoginInternalRoute.OnEmailChanged("  user@match.app  "))
            model.onEvent(LoginInternalRoute.OnPasswordChanged("123456"))
            model.onEvent(LoginInternalRoute.OnSubmitClicked)
            advanceUntilIdle()

            assertEquals("user@match.app" to "123456", sources.firebase.lastSignInInput)
            assertEquals("user@match.app", model.state.value.email)
        }

    @Test
    fun `When email sign in fails should expose error then loading finishes`() =
        runTest(testDispatcher) {
            val firebaseSource =
                FakeFirebaseAuthSource(
                    signInResult = Result.failure(IllegalStateException("Falha no login")),
                )
            val (model, _) = buildModel(firebaseAuthSource = firebaseSource)

            model.onEvent(LoginInternalRoute.OnEmailChanged("user@match.app"))
            model.onEvent(LoginInternalRoute.OnPasswordChanged("123456"))
            model.onEvent(LoginInternalRoute.OnSubmitClicked)
            advanceUntilIdle()

            assertEquals("Não foi possível entrar", model.state.value.error)
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `When Google sign in succeeds should clear loading then google source is called once`() =
        runTest(testDispatcher) {
            val googleSource = FakeGoogleAuthSource(signInResult = Result.success(userSession))
            val (model, sources) = buildModel(googleAuthSource = googleSource)

            model.onEvent(LoginInternalRoute.OnGoogleSignInClicked)
            advanceUntilIdle()

            assertEquals(1, sources.google.signInCallCount)
            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When Google sign in fails should expose error then loading finishes`() =
        runTest(testDispatcher) {
            val googleSource =
                FakeGoogleAuthSource(
                    signInResult = Result.failure(IllegalStateException("Falha no Google")),
                )
            val (model, _) = buildModel(googleAuthSource = googleSource)

            model.onEvent(LoginInternalRoute.OnGoogleSignInClicked)
            advanceUntilIdle()

            assertEquals("Não foi possível entrar com Google", model.state.value.error)
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `When Google sign in is cancelled should clear loading without showing an error`() =
        runTest(testDispatcher) {
            val googleSource = FakeGoogleAuthSource(signInResult = Result.failure(IdentityError.Cancelled))
            val (model, _) = buildModel(googleAuthSource = googleSource)

            model.onEvent(LoginInternalRoute.OnGoogleSignInClicked)
            advanceUntilIdle()

            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When Apple sign in succeeds should clear loading then apple source is called once`() =
        runTest(testDispatcher) {
            val appleSource = FakeAppleAuthSource(signInResult = Result.success(userSession))
            val (model, sources) = buildModel(appleAuthSource = appleSource)

            model.onEvent(LoginInternalRoute.OnAppleSignInClicked)
            advanceUntilIdle()

            assertEquals(1, sources.apple.signInCallCount)
            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When Apple sign in is cancelled should clear loading without showing an error`() =
        runTest(testDispatcher) {
            val appleSource = FakeAppleAuthSource(signInResult = Result.failure(IdentityError.Cancelled))
            val (model, _) = buildModel(appleAuthSource = appleSource)

            model.onEvent(LoginInternalRoute.OnAppleSignInClicked)
            advanceUntilIdle()

            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When Apple sign in fails should expose error then loading finishes`() =
        runTest(testDispatcher) {
            val appleSource =
                FakeAppleAuthSource(
                    signInResult = Result.failure(IllegalStateException("Falha no Apple")),
                )
            val (model, _) = buildModel(appleAuthSource = appleSource)

            model.onEvent(LoginInternalRoute.OnAppleSignInClicked)
            advanceUntilIdle()

            assertEquals("Não foi possível entrar com Apple", model.state.value.error)
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `When error is dismissed should clear error then state has no error`() {
        val (model, _) = buildModel()

        model.onEvent(LoginInternalRoute.OnEmailChanged(""))
        model.onEvent(LoginInternalRoute.OnSubmitClicked)
        model.onEvent(LoginInternalRoute.OnErrorDismissed)

        assertNull(model.state.value.error)
    }

    @Test
    fun `When session already exists should keep state stable then form remains idle`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource(initialUser = userSession)
            val (model, _) = buildModel(firebaseAuthSource = firebaseSource)
            advanceUntilIdle()

            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
            assertTrue(
                model.state.value.email
                    .isEmpty(),
            )
        }
}
