package com.walcker.identity.fake

import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient
import com.walcker.match.firestore.FirestoreCollectionReference
import com.walcker.match.firestore.FirestoreDocumentReference
import com.walcker.match.firestore.FirestoreQueryBuilder
import com.walcker.match.firestore.FirestoreTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeFirestoreClient(
    var documents: Map<String, DocumentSnapshot?> = emptyMap(),
) : FirestoreClient {
    override fun document(path: String): FirestoreDocumentReference = FakeDocumentReference(path, this)

    override fun collection(path: String): FirestoreCollectionReference = error("not used in this fake")

    override fun query(path: String): FirestoreQueryBuilder = error("not used in this fake")

    override suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T> =
        error("not used in this fake")

    override suspend fun callFunction(
        name: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> = error("not used in this fake")

    private class FakeDocumentReference(
        override val path: String,
        private val client: FakeFirestoreClient,
    ) : FirestoreDocumentReference {
        override suspend fun get(): Result<DocumentSnapshot?> = Result.success(client.documents[path])

        override fun snapshots(): Flow<Result<DocumentSnapshot?>> = flowOf(Result.success(client.documents[path]))

        override suspend fun delete(): Result<Unit> = error("not used in this fake")

        override suspend fun set(
            data: Map<String, Any?>,
            merge: Boolean,
        ): Result<Unit> = error("not used in this fake")

        override suspend fun update(data: Map<String, Any?>): Result<Unit> = error("not used in this fake")

        override fun collection(name: String): FirestoreCollectionReference = error("not used in this fake")
    }
}
