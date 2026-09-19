package com.walcker.match.app.notifications

import com.walcker.identity.api.SessionHolder
import com.walcker.identity.api.UserSession
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient
import com.walcker.match.firestore.FirestoreCollectionReference
import com.walcker.match.firestore.FirestoreDocumentReference
import com.walcker.match.firestore.FirestoreQueryBuilder
import com.walcker.match.firestore.FirestoreTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceTokenRegistrarTest {
    private val sessionHolder = FakeSessionHolder()
    private val push = FakePushNotificationService()
    private val firestore = InMemoryFirestore()
    private val registrar = DeviceTokenRegistrar(sessionHolder, push, firestore)

    private fun TestScope.startRegistrar() {
        backgroundScope.launch { registrar.start() }
        runCurrent()
    }

    @Test
    fun `the phone is registered under the signed in account`() =
        runTest {
            startRegistrar()

            sessionHolder.signIn("ana")
            push.token.value = "phone-1"
            runCurrent()

            assertEquals(mapOf<String, Map<String, Any?>>("users/ana/devices/phone-1" to mapOf("userId" to "ana", "platform" to "android")), firestore.documents)
        }

    @Test
    fun `two phones of the same account are both registered`() =
        runTest {
            val otherPhone = DeviceTokenRegistrar(sessionHolder, FakePushNotificationService("phone-2"), firestore)
            startRegistrar()
            backgroundScope.launch { otherPhone.start() }

            sessionHolder.signIn("ana")
            push.token.value = "phone-1"
            runCurrent()

            assertEquals(setOf("users/ana/devices/phone-1", "users/ana/devices/phone-2"), firestore.documents.keys)
        }

    @Test
    fun `a refreshed token replaces the old one`() =
        runTest {
            startRegistrar()
            sessionHolder.signIn("ana")
            push.token.value = "old-token"
            runCurrent()

            push.token.value = "new-token"
            runCurrent()

            assertEquals(setOf("users/ana/devices/new-token"), firestore.documents.keys)
        }

    @Test
    fun `signing out removes this phone so it stops getting the account notifications`() =
        runTest {
            startRegistrar()
            sessionHolder.signIn("ana")
            push.token.value = "phone-1"
            runCurrent()

            registrar.beforeSignOut()

            assertEquals(emptyMap<String, Map<String, Any?>>(), firestore.documents)
        }

    @Test
    fun `another account on the same phone takes over its notifications`() =
        runTest {
            startRegistrar()
            sessionHolder.signIn("ana")
            push.token.value = "phone-1"
            runCurrent()

            registrar.beforeSignOut()
            sessionHolder.signOut()
            runCurrent()
            sessionHolder.signIn("bia")
            runCurrent()

            assertEquals(setOf("users/bia/devices/phone-1"), firestore.documents.keys)
        }

    @Test
    fun `signing out before the phone got a token removes nothing`() =
        runTest {
            startRegistrar()
            sessionHolder.signIn("ana")
            runCurrent()

            registrar.beforeSignOut()

            assertEquals(emptyList<String>(), firestore.deletedPaths)
        }
}

private class FakeSessionHolder : SessionHolder {
    private val session = MutableStateFlow<UserSession?>(null)
    override val currentUser: Flow<UserSession?> = session
    override val isAuthenticated: Flow<Boolean> = session.map { it != null }

    fun signIn(uid: String) {
        session.value = UserSession(uid = uid, email = null, displayName = null)
    }

    fun signOut() {
        session.value = null
    }
}

private class FakePushNotificationService(
    initialToken: String? = null,
) : PushNotificationService {
    val token = MutableStateFlow(initialToken)
    override val deviceToken: Flow<String?> = token
    override val platform: String = "android"

    override suspend fun requestNotificationPermission(): Result<Boolean> = Result.success(true)
}

private class InMemoryFirestore : FirestoreClient {
    val documents = mutableMapOf<String, Map<String, Any?>>()
    val deletedPaths = mutableListOf<String>()

    override fun document(path: String): FirestoreDocumentReference = Document(path)

    override fun collection(path: String): FirestoreCollectionReference = unused()

    override fun query(path: String): FirestoreQueryBuilder = unused()

    override suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T> = unused()

    override suspend fun callFunction(
        name: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> = unused()

    private inner class Document(
        override val path: String,
    ) : FirestoreDocumentReference {
        override suspend fun set(
            data: Map<String, Any?>,
            merge: Boolean,
        ): Result<Unit> {
            documents[path] = data
            return Result.success(Unit)
        }

        override suspend fun delete(): Result<Unit> {
            documents.remove(path)
            deletedPaths += path
            return Result.success(Unit)
        }

        override suspend fun get(): Result<DocumentSnapshot?> = unused()

        override fun snapshots(): Flow<Result<DocumentSnapshot?>> = unused()

        override suspend fun update(data: Map<String, Any?>): Result<Unit> = unused()

        override fun collection(name: String): FirestoreCollectionReference = unused()
    }

    private fun unused(): Nothing = throw UnsupportedOperationException("DeviceTokenRegistrar does not use this")
}
