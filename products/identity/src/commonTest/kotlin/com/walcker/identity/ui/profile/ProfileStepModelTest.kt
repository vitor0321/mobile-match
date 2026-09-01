package com.walcker.identity.ui.profile

import com.walcker.identity.api.UserSession
import com.walcker.identity.fake.FakeAccountDeletionRepository
import com.walcker.identity.fake.FakeAppleAuthSource
import com.walcker.identity.fake.FakeBillingClient
import com.walcker.identity.fake.FakeBillingRepository
import com.walcker.identity.fake.FakeFirebaseAuthSource
import com.walcker.identity.fake.FakeGoogleAuthSource
import com.walcker.identity.fake.FakeIdentityDestination
import com.walcker.identity.fake.FakeProStateCache
import com.walcker.identity.fake.FakeProStateHolder
import com.walcker.identity.features.data.repository.AuthRepositoryImpl
import com.walcker.identity.features.data.usecase.SignUseCaseImpl
import com.walcker.identity.features.domain.usecase.DeleteAccountUseCaseImpl
import com.walcker.identity.features.domain.usecase.ProfileAccountUseCaseImpl
import com.walcker.identity.features.domain.usecase.RequiresRecentLoginException
import com.walcker.identity.features.ui.profile.ProfileStepModel
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
class ProfileStepModelTest {
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
        billingRepository: FakeBillingRepository = FakeBillingRepository(),
        accountDeletionRepository: FakeAccountDeletionRepository = FakeAccountDeletionRepository(),
        billingClient: FakeBillingClient = FakeBillingClient(),
        proStateCache: FakeProStateCache = FakeProStateCache(),
        proStateHolder: FakeProStateHolder = FakeProStateHolder(),
        identityDestination: FakeIdentityDestination = FakeIdentityDestination(),
    ): Pair<ProfileStepModel, Dependencies> {
        val authRepository =
            AuthRepositoryImpl(
                firebaseAuthSource = firebaseAuthSource,
                googleAuthSource = FakeGoogleAuthSource(),
                appleAuthSource = FakeAppleAuthSource(),
            )
        val signUseCase = SignUseCaseImpl(authRepository = authRepository)
        val deleteAccountUseCase =
            DeleteAccountUseCaseImpl(
                accountDeletionRepository = accountDeletionRepository,
                authRepository = authRepository,
                billingClient = billingClient,
                proStateCache = proStateCache,
            )
        val profileAccountUseCase =
            ProfileAccountUseCaseImpl(
                proStateHolder = proStateHolder,
                billingRepository = billingRepository,
            )
        val model =
            ProfileStepModel(
                signUseCase = signUseCase,
                deleteAccountUseCase = deleteAccountUseCase,
                profileAccountUseCase = profileAccountUseCase,
                navigatorHolder = NavigatorHolder(),
                identityDestination = identityDestination,
                stringsHolder = stringsHolder,
            )
        return model to
            Dependencies(
                firebaseAuthSource = firebaseAuthSource,
                billingRepository = billingRepository,
                accountDeletionRepository = accountDeletionRepository,
                billingClient = billingClient,
                proStateCache = proStateCache,
                proStateHolder = proStateHolder,
                identityDestination = identityDestination,
            )
    }

    @Test
    fun `When session is observed should update state then current user is exposed`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource(initialUser = userSession)
            val (model, _) = buildModel(firebaseAuthSource = firebaseSource)
            advanceUntilIdle()

            assertEquals(userSession, model.state.value.userSession)
            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When Pro state changes should update state then current plan is exposed`() =
        runTest(testDispatcher) {
            val proStateHolder = FakeProStateHolder(initialIsPro = false)
            val (model, dependencies) = buildModel(proStateHolder = proStateHolder)
            advanceUntilIdle()

            dependencies.proStateHolder.update(true)
            advanceUntilIdle()

            assertTrue(model.state.value.isPro)
        }

    @Test
    fun `When pro turns true should fetch management url then expose it on state`() =
        runTest(testDispatcher) {
            val billingRepository =
                FakeBillingRepository(
                    managementUrlResult = Result.success("https://play.google.com/store/account/subscriptions"),
                )
            val proStateHolder = FakeProStateHolder(initialIsPro = false)
            val (model, dependencies) =
                buildModel(
                    billingRepository = billingRepository,
                    proStateHolder = proStateHolder,
                )
            advanceUntilIdle()

            dependencies.proStateHolder.update(true)
            advanceUntilIdle()

            assertEquals(1, dependencies.billingRepository.managementUrlCallCount)
            assertEquals(
                "https://play.google.com/store/account/subscriptions",
                model.state.value.managementUrl,
            )
        }

    @Test
    fun `When pro turns false should clear management url`() =
        runTest(testDispatcher) {
            val billingRepository =
                FakeBillingRepository(
                    managementUrlResult = Result.success("https://play.google.com/store/account/subscriptions"),
                )
            val proStateHolder = FakeProStateHolder(initialIsPro = true)
            val (model, dependencies) =
                buildModel(
                    billingRepository = billingRepository,
                    proStateHolder = proStateHolder,
                )
            advanceUntilIdle()
            dependencies.proStateHolder.update(false)
            advanceUntilIdle()

            assertNull(model.state.value.managementUrl)
            assertFalse(model.state.value.isPro)
        }

    @Test
    fun `When sign out succeeds should finish loading then auth source is called once`() =
        runTest(testDispatcher) {
            val firebaseSource =
                FakeFirebaseAuthSource(
                    initialUser = userSession,
                    signOutResult = Result.success(Unit),
                )
            val (model, dependencies) = buildModel(firebaseAuthSource = firebaseSource)
            advanceUntilIdle()

            model.onSignOutClicked()
            advanceUntilIdle()

            assertEquals(1, dependencies.firebaseAuthSource.signOutCallCount)
            assertFalse(model.state.value.isLoading)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When sign out fails without message should expose localized fallback then loading finishes`() =
        runTest(testDispatcher) {
            val firebaseSource =
                FakeFirebaseAuthSource(
                    initialUser = userSession,
                    signOutResult = Result.failure(IllegalStateException()),
                )
            val (model, _) = buildModel(firebaseAuthSource = firebaseSource)
            advanceUntilIdle()

            model.onSignOutClicked()
            advanceUntilIdle()

            assertEquals("Não foi possível sair", model.state.value.error)
            assertFalse(model.state.value.isLoading)
        }

    @Test
    fun `When restore purchases returns pro access should expose success message then stop restore loading`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource(initialUser = userSession)
            val billingRepository = FakeBillingRepository(restoreResult = Result.success(true))
            val (model, dependencies) =
                buildModel(
                    firebaseAuthSource = firebaseSource,
                    billingRepository = billingRepository,
                )
            advanceUntilIdle()

            model.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertEquals(1, dependencies.billingRepository.restoreCallCount)
            assertEquals("Compras restauradas.", model.state.value.message)
            assertFalse(model.state.value.isRestoringPurchases)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When restore purchases returns no pro access should expose no purchases message`() =
        runTest(testDispatcher) {
            val firebaseSource = FakeFirebaseAuthSource(initialUser = userSession)
            val billingRepository = FakeBillingRepository(restoreResult = Result.success(false))
            val (model, dependencies) =
                buildModel(
                    firebaseAuthSource = firebaseSource,
                    billingRepository = billingRepository,
                )
            advanceUntilIdle()

            model.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertEquals(1, dependencies.billingRepository.restoreCallCount)
            assertEquals(
                "Nenhuma compra ativa foi encontrada para restaurar.",
                model.state.value.message,
            )
            assertFalse(model.state.value.isRestoringPurchases)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When subscribe Pro is clicked should request paywall destination once`() =
        runTest(testDispatcher) {
            val (model, dependencies) = buildModel()
            advanceUntilIdle()

            model.onUpgradeToProClicked()

            assertEquals(1, dependencies.identityDestination.paywallCallCount)
        }

    @Test
    fun `When account deletion is confirmed should delete remote data then auth and cleanup local state`() =
        runTest(testDispatcher) {
            val firebaseSource =
                FakeFirebaseAuthSource(
                    initialUser = userSession,
                    deleteCurrentUserResult = Result.success(Unit),
                    signOutResult = Result.success(Unit),
                )
            val billingClient = FakeBillingClient(logOutResult = Result.success(Unit))
            val proStateCache = FakeProStateCache(initialIsPro = true)
            val (model, dependencies) =
                buildModel(
                    firebaseAuthSource = firebaseSource,
                    billingClient = billingClient,
                    proStateCache = proStateCache,
                )
            advanceUntilIdle()

            model.onDeleteAccountClicked()
            assertTrue(model.state.value.showDeleteAccountConfirmation)
            assertEquals(0, dependencies.firebaseAuthSource.deleteCurrentUserCallCount)

            model.onDeleteAccountConfirmed()
            advanceUntilIdle()

            assertEquals(1, dependencies.accountDeletionRepository.callCount)
            assertEquals(1, dependencies.firebaseAuthSource.deleteCurrentUserCallCount)
            assertEquals(1, dependencies.billingClient.logOutCallCount)
            assertTrue(dependencies.proStateCache.savedValues.isEmpty())
            assertEquals(0, dependencies.firebaseAuthSource.signOutCallCount)
            assertFalse(model.state.value.isDeletingAccount)
            assertNull(model.state.value.error)
        }

    @Test
    fun `When auth deletion fails should preserve billing and cache then expose fallback`() =
        runTest(testDispatcher) {
            val firebaseSource =
                FakeFirebaseAuthSource(
                    initialUser = userSession,
                    deleteCurrentUserResult = Result.failure(IllegalStateException()),
                )
            val (model, dependencies) = buildModel(firebaseAuthSource = firebaseSource)
            advanceUntilIdle()

            model.onDeleteAccountClicked()
            model.onDeleteAccountConfirmed()
            advanceUntilIdle()

            assertEquals("Não foi possível excluir a conta", model.state.value.error)
            assertFalse(model.state.value.isDeletingAccount)
            assertEquals(1, dependencies.accountDeletionRepository.callCount)
            assertEquals(0, dependencies.billingClient.logOutCallCount)
            assertTrue(dependencies.proStateCache.savedValues.isEmpty())
        }

    @Test
    fun `When remote deletion requires recent login should preserve account state and prompt reauthentication`() =
        runTest(testDispatcher) {
            val accountDeletionRepository =
                FakeAccountDeletionRepository(
                    result = Result.failure(RequiresRecentLoginException()),
                )
            val (model, dependencies) = buildModel(accountDeletionRepository = accountDeletionRepository)
            advanceUntilIdle()

            model.onDeleteAccountClicked()
            model.onDeleteAccountConfirmed()
            advanceUntilIdle()

            assertEquals("Para sua segurança, entre novamente e tente excluir a conta outra vez.", model.state.value.error)
            assertEquals(1, dependencies.accountDeletionRepository.callCount)
            assertEquals(0, dependencies.firebaseAuthSource.deleteCurrentUserCallCount)
            assertEquals(0, dependencies.billingClient.logOutCallCount)
            assertTrue(dependencies.proStateCache.savedValues.isEmpty())
        }

    @Test
    fun `When remote deletion fails should not attempt auth or local cleanup`() =
        runTest(testDispatcher) {
            val accountDeletionRepository =
                FakeAccountDeletionRepository(
                    result = Result.failure(IllegalStateException("network")),
                )
            val (model, dependencies) = buildModel(accountDeletionRepository = accountDeletionRepository)
            advanceUntilIdle()

            model.onDeleteAccountClicked()
            model.onDeleteAccountConfirmed()
            advanceUntilIdle()

            assertEquals(1, dependencies.accountDeletionRepository.callCount)
            assertEquals(0, dependencies.firebaseAuthSource.deleteCurrentUserCallCount)
            assertEquals(0, dependencies.billingClient.logOutCallCount)
            assertTrue(dependencies.proStateCache.savedValues.isEmpty())
        }

    private data class Dependencies(
        val firebaseAuthSource: FakeFirebaseAuthSource,
        val billingRepository: FakeBillingRepository,
        val accountDeletionRepository: FakeAccountDeletionRepository,
        val billingClient: FakeBillingClient,
        val proStateCache: FakeProStateCache,
        val proStateHolder: FakeProStateHolder,
        val identityDestination: FakeIdentityDestination,
    )
}
