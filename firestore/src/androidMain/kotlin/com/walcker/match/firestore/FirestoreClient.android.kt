package com.walcker.match.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private val firestore: FirebaseFirestore by lazy {
    FirebaseFirestore.getInstance()
}

public actual fun createFirestoreClient(): FirestoreClient = AndroidFirestoreClient()

private class AndroidFirestoreClient : FirestoreClient {
    override fun document(path: String): FirestoreDocumentReference =
        AndroidDocumentReference(firestore.document(path))

    override fun collection(path: String): FirestoreCollectionReference =
        AndroidCollectionReference(firestore.collection(path))

    override fun query(path: String): FirestoreQueryBuilder =
        AndroidQueryBuilder(firestore.collection(path))

    override suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T> =
        runCatching {
            firestore.runTransaction { txn ->
                val wrappedTxn = AndroidTransaction(txn)
                // Note: Firestore's runTransaction doesn't support suspend, so we'd need a workaround.
                // For now, this is a placeholder. Production would use Tasks.await or similar.
                throw NotImplementedError("Transaction with suspend not yet implemented for Android")
            }.await()
        }

    override suspend fun callFunction(name: String, data: Map<String, Any?>): Result<Map<String, Any?>> =
        runCatching {
            val functions = FirebaseFunctions.getInstance()
            val result = functions.getHttpsCallable(name).call(data).await()
            @Suppress("UNCHECKED_CAST")
            (result.data as Map<String, Any?>) ?: emptyMap()
        }.map { it }
}

private class AndroidDocumentReference(private val ref: com.google.firebase.firestore.DocumentReference) :
    FirestoreDocumentReference {
    override val path: String get() = ref.path

    override suspend fun get(): Result<DocumentSnapshot?> =
        runCatching {
            val doc = ref.get().await()
            if (doc.exists()) {
                DocumentSnapshot(
                    path = doc.reference.path,
                    id = doc.id,
                    data = doc.data ?: emptyMap(),
                    exists = true,
                    metadata = SnapshotMetadata(
                        isFromCache = doc.metadata.isFromCache,
                        hasPendingWrites = false, // Firebase SDK hasPendingWrites is private
                    ),
                )
            } else {
                null
            }
        }

    override fun snapshots(): Flow<Result<DocumentSnapshot?>> = callbackFlow {
        val listener = ref.addSnapshotListener { doc, error ->
            if (error != null) {
                trySend(Result.failure(error))
            } else if (doc != null) {
                val snapshot = if (doc.exists()) {
                    DocumentSnapshot(
                        path = doc.reference.path,
                        id = doc.id,
                        data = doc.data ?: emptyMap(),
                        exists = true,
                        metadata = SnapshotMetadata(
                            isFromCache = doc.metadata.isFromCache,
                            hasPendingWrites = false,
                        ),
                    )
                } else {
                    null
                }
                trySend(Result.success(snapshot))
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    override suspend fun delete(): Result<Unit> =
        runCatching {
            ref.delete().await()
        }

    override suspend fun set(data: Map<String, Any?>): Result<Unit> =
        runCatching {
            ref.set(data).await()
        }

    override suspend fun update(data: Map<String, Any?>): Result<Unit> =
        runCatching {
            ref.update(data).await()
        }

    override fun collection(name: String): FirestoreCollectionReference =
        AndroidCollectionReference(ref.collection(name))
}

private class AndroidCollectionReference(private val ref: com.google.firebase.firestore.CollectionReference) :
    FirestoreCollectionReference {
    override val path: String get() = ref.path

    override fun query(): FirestoreQueryBuilder = AndroidQueryBuilder(ref)

    override suspend fun list(): Result<List<DocumentSnapshot>> =
        runCatching {
            val docs = ref.get().await()
            docs.map { doc ->
                DocumentSnapshot(
                    path = doc.reference.path,
                    id = doc.id,
                    data = doc.data,
                    exists = true,
                )
            }
        }

    override fun snapshots(query: FirestoreQueryBuilder?): Flow<Result<List<DocumentSnapshot>>> = callbackFlow {
        val fbQuery = if (query is AndroidQueryBuilder) query.build() else ref
        val listener = fbQuery.addSnapshotListener { docs, error ->
            if (error != null) {
                trySend(Result.failure(error))
            } else if (docs != null) {
                val snapshots = docs.map { doc ->
                    DocumentSnapshot(
                        path = doc.reference.path,
                        id = doc.id,
                        data = doc.data,
                        exists = true,
                    )
                }
                trySend(Result.success(snapshots))
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    override suspend fun add(data: Map<String, Any?>): Result<String> =
        runCatching {
            val docRef = ref.add(data).await()
            docRef.id
        }
}

private class AndroidQueryBuilder(private val baseQuery: com.google.firebase.firestore.Query) :
    FirestoreQueryBuilder {
    private var currentQuery: com.google.firebase.firestore.Query = baseQuery

    override fun where(field: String, operator: String, value: Any?): FirestoreQueryBuilder {
        val newBuilder = AndroidQueryBuilder(baseQuery)
        @Suppress("UNCHECKED_CAST")
        newBuilder.currentQuery = when (operator.lowercase()) {
            "==" -> currentQuery.whereEqualTo(field, value)
            "!=" -> currentQuery.whereNotEqualTo(field, value)
            "<" -> if (value != null) currentQuery.whereLessThan(field, value) else currentQuery
            "<=" -> if (value != null) currentQuery.whereLessThanOrEqualTo(field, value) else currentQuery
            ">" -> if (value != null) currentQuery.whereGreaterThan(field, value) else currentQuery
            ">=" -> if (value != null) currentQuery.whereGreaterThanOrEqualTo(field, value) else currentQuery
            "in" -> currentQuery.whereIn(field, value as? List<Any> ?: emptyList())
            "array-contains" -> if (value != null) currentQuery.whereArrayContains(field, value) else currentQuery
            "array-contains-any" -> currentQuery.whereArrayContainsAny(field, value as? List<Any> ?: emptyList())
            else -> throw IllegalArgumentException("Unknown operator: $operator")
        }
        return newBuilder
    }

    override fun orderBy(field: String, direction: String): FirestoreQueryBuilder {
        val newBuilder = AndroidQueryBuilder(baseQuery)
        newBuilder.currentQuery = if (direction.lowercase() == "desc") {
            currentQuery.orderBy(field, Query.Direction.DESCENDING)
        } else {
            currentQuery.orderBy(field, Query.Direction.ASCENDING)
        }
        return newBuilder
    }

    override fun limit(count: Int): FirestoreQueryBuilder {
        val newBuilder = AndroidQueryBuilder(baseQuery)
        newBuilder.currentQuery = currentQuery.limit(count.toLong())
        return newBuilder
    }

    override fun startAt(vararg values: Any?): FirestoreQueryBuilder {
        val newBuilder = AndroidQueryBuilder(baseQuery)
        @Suppress("UNCHECKED_CAST")
        newBuilder.currentQuery = currentQuery.startAt(*(values.filterNotNull().toTypedArray() as Array<Any>))
        return newBuilder
    }

    override fun endAt(vararg values: Any?): FirestoreQueryBuilder {
        val newBuilder = AndroidQueryBuilder(baseQuery)
        @Suppress("UNCHECKED_CAST")
        newBuilder.currentQuery = currentQuery.endAt(*(values.filterNotNull().toTypedArray() as Array<Any>))
        return newBuilder
    }

    override fun startAfter(vararg values: Any?): FirestoreQueryBuilder {
        val newBuilder = AndroidQueryBuilder(baseQuery)
        @Suppress("UNCHECKED_CAST")
        newBuilder.currentQuery = currentQuery.startAfter(*(values.filterNotNull().toTypedArray() as Array<Any>))
        return newBuilder
    }

    override fun endBefore(vararg values: Any?): FirestoreQueryBuilder {
        val newBuilder = AndroidQueryBuilder(baseQuery)
        @Suppress("UNCHECKED_CAST")
        newBuilder.currentQuery = currentQuery.endBefore(*(values.filterNotNull().toTypedArray() as Array<Any>))
        return newBuilder
    }

    override suspend fun get(): Result<List<DocumentSnapshot>> =
        runCatching {
            val docs = currentQuery.get().await()
            docs.map { doc ->
                DocumentSnapshot(
                    path = doc.reference.path,
                    id = doc.id,
                    data = doc.data,
                    exists = true,
                )
            }
        }

    override fun snapshots(): Flow<Result<List<DocumentSnapshot>>> = callbackFlow {
        val listener = currentQuery.addSnapshotListener { docs, error ->
            if (error != null) {
                trySend(Result.failure(error))
            } else if (docs != null) {
                val snapshots = docs.map { doc ->
                    DocumentSnapshot(
                        path = doc.reference.path,
                        id = doc.id,
                        data = doc.data,
                        exists = true,
                    )
                }
                trySend(Result.success(snapshots))
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    internal fun build(): com.google.firebase.firestore.Query = currentQuery
}

private class AndroidTransaction(private val txn: Transaction) : FirestoreTransaction {
    override suspend fun get(path: String): Result<DocumentSnapshot?> =
        runCatching {
            val doc = txn.get(FirebaseFirestore.getInstance().document(path))
            if (doc.exists()) {
                DocumentSnapshot(
                    path = doc.reference.path,
                    id = doc.id,
                    data = doc.data ?: emptyMap(),
                    exists = true,
                )
            } else {
                null
            }
        }

    override suspend fun set(path: String, data: Map<String, Any?>): Result<Unit> =
        runCatching {
            txn.set(FirebaseFirestore.getInstance().document(path), data)
        }

    override suspend fun update(path: String, data: Map<String, Any?>): Result<Unit> =
        runCatching {
            txn.update(FirebaseFirestore.getInstance().document(path), data)
        }

    override suspend fun delete(path: String): Result<Unit> =
        runCatching {
            txn.delete(FirebaseFirestore.getInstance().document(path))
        }
}
