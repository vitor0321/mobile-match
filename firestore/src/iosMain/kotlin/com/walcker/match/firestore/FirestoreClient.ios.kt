@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.walcker.match.firestore

import cocoapods.FirebaseFirestoreInternal.FIRCollectionReference
import cocoapods.FirebaseFirestoreInternal.FIRDocumentReference
import cocoapods.FirebaseFirestoreInternal.FIRDocumentSnapshot
import cocoapods.FirebaseFirestoreInternal.FIRFirestore
import cocoapods.FirebaseFirestoreInternal.FIRQuery
import cocoapods.FirebaseFunctions.FIRFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSNull
import kotlin.coroutines.resume

private const val FUNCTIONS_REGION = "southamerica-east1"

private val firestore: FIRFirestore by lazy { FIRFirestore.firestore() }

public actual fun createFirestoreClient(): FirestoreClient = IosFirestoreClient()

private fun NSError.toThrowable(): Throwable = IllegalStateException(localizedDescription)

private fun FIRDocumentSnapshot.toDocumentSnapshot(): DocumentSnapshot =
    DocumentSnapshot(
        path = reference.path,
        id = documentID,
        data = (data() as? Map<String, Any?>).orEmpty(),
        exists = exists,
        metadata = SnapshotMetadata(isFromCache = metadata.fromCache, hasPendingWrites = metadata.pendingWrites),
    )

private class IosFirestoreClient : FirestoreClient {
    override fun document(path: String): FirestoreDocumentReference = IosDocumentReference(firestore.documentWithPath(path))

    override fun collection(path: String): FirestoreCollectionReference = IosCollectionReference(firestore.collectionWithPath(path))

    override fun query(path: String): FirestoreQueryBuilder = IosQueryBuilder(firestore.collectionWithPath(path))

    override suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T> = Result.failure(NotImplementedError("iOS Firestore transaction not implemented"))

    override suspend fun callFunction(
        name: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> =
        suspendCancellableCoroutine { continuation ->
            FIRFunctions
                .functionsForRegion(FUNCTIONS_REGION)
                .HTTPSCallableWithName(name)
                .callWithObject(data, completion = { result, error ->
                    continuation.resume(
                        if (error != null) {
                            Result.failure(error.toThrowable())
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            Result.success((result?.data() as? Map<String, Any?>).orEmpty())
                        },
                    )
                })
        }
}

private class IosDocumentReference(
    private val ref: FIRDocumentReference,
) : FirestoreDocumentReference {
    override val path: String get() = ref.path

    override suspend fun get(): Result<DocumentSnapshot?> =
        suspendCancellableCoroutine { continuation ->
            ref.getDocumentWithCompletion { snapshot, error ->
                continuation.resume(
                    if (error != null) {
                        Result.failure(error.toThrowable())
                    } else {
                        Result.success(snapshot?.takeIf { it.exists }?.toDocumentSnapshot())
                    },
                )
            }
        }

    override fun snapshots(): Flow<Result<DocumentSnapshot?>> =
        callbackFlow {
            val registration =
                ref.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error.toThrowable()))
                    } else {
                        trySend(Result.success(snapshot?.takeIf { it.exists }?.toDocumentSnapshot()))
                    }
                }
            awaitClose { registration.remove() }
        }

    override suspend fun delete(): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            ref.deleteDocumentWithCompletion { error ->
                continuation.resume(if (error != null) Result.failure(error.toThrowable()) else Result.success(Unit))
            }
        }

    override suspend fun set(
        data: Map<String, Any?>,
        merge: Boolean,
    ): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            @Suppress("UNCHECKED_CAST")
            ref.setData(data as Map<Any?, *>, merge = merge, completion = { error ->
                continuation.resume(if (error != null) Result.failure(error.toThrowable()) else Result.success(Unit))
            })
        }

    override suspend fun update(data: Map<String, Any?>): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            @Suppress("UNCHECKED_CAST")
            ref.updateData(data as Map<Any?, *>, completion = { error ->
                continuation.resume(if (error != null) Result.failure(error.toThrowable()) else Result.success(Unit))
            })
        }

    override fun collection(name: String): FirestoreCollectionReference = IosCollectionReference(ref.collectionWithPath(name))
}

private class IosCollectionReference(
    private val ref: FIRCollectionReference,
) : FirestoreCollectionReference {
    override val path: String get() = ref.path

    override fun query(): FirestoreQueryBuilder = IosQueryBuilder(ref)

    override suspend fun list(): Result<List<DocumentSnapshot>> =
        suspendCancellableCoroutine { continuation ->
            ref.getDocumentsWithCompletion { snapshot, error ->
                continuation.resume(
                    if (error != null) {
                        Result.failure(error.toThrowable())
                    } else {
                        Result.success(snapshot?.documents.orEmpty().map { (it as FIRDocumentSnapshot).toDocumentSnapshot() })
                    },
                )
            }
        }

    override fun snapshots(query: FirestoreQueryBuilder?): Flow<Result<List<DocumentSnapshot>>> = (query as? IosQueryBuilder ?: IosQueryBuilder(ref)).snapshots()

    override suspend fun add(data: Map<String, Any?>): Result<String> =
        suspendCancellableCoroutine { continuation ->
            lateinit var newRef: FIRDocumentReference
            @Suppress("UNCHECKED_CAST")
            newRef =
                ref.addDocumentWithData(data as Map<Any?, *>, completion = { error ->
                    continuation.resume(
                        if (error != null) Result.failure(error.toThrowable()) else Result.success(newRef.documentID),
                    )
                })
        }
}

private class IosQueryBuilder(
    private val currentQuery: FIRQuery,
) : FirestoreQueryBuilder {
    override fun where(
        field: String,
        operator: String,
        value: Any?,
    ): FirestoreQueryBuilder {
        val newQuery =
            when (operator.lowercase()) {
                "==" -> currentQuery.queryWhereField(field, isEqualTo = value ?: NSNull())
                "!=" -> currentQuery.queryWhereField(field, isNotEqualTo = value ?: NSNull())
                "<" -> if (value != null) currentQuery.queryWhereField(field, isLessThan = value) else currentQuery
                "<=" -> if (value != null) currentQuery.queryWhereField(field, isLessThanOrEqualTo = value) else currentQuery
                ">" -> if (value != null) currentQuery.queryWhereField(field, isGreaterThan = value) else currentQuery
                ">=" -> if (value != null) currentQuery.queryWhereField(field, isGreaterThanOrEqualTo = value) else currentQuery
                "in" -> currentQuery.queryWhereField(field, `in` = (value as? List<Any> ?: emptyList<Any>()))
                "array-contains" -> if (value != null) currentQuery.queryWhereField(field, arrayContains = value) else currentQuery
                "array-contains-any" ->
                    currentQuery.queryWhereField(field, arrayContainsAny = (value as? List<Any> ?: emptyList<Any>()))
                else -> throw IllegalArgumentException("Unknown operator: $operator")
            }
        return IosQueryBuilder(newQuery)
    }

    override fun orderBy(
        field: String,
        direction: String,
    ): FirestoreQueryBuilder = IosQueryBuilder(currentQuery.queryOrderedByField(field, descending = direction.lowercase() == "desc"))

    override fun limit(count: Int): FirestoreQueryBuilder = IosQueryBuilder(currentQuery.queryLimitedTo(count.toLong()))

    override fun startAt(vararg values: Any?): FirestoreQueryBuilder = IosQueryBuilder(currentQuery.queryStartingAtValues(values.filterNotNull()))

    override fun endAt(vararg values: Any?): FirestoreQueryBuilder = IosQueryBuilder(currentQuery.queryEndingAtValues(values.filterNotNull()))

    override fun startAfter(vararg values: Any?): FirestoreQueryBuilder = IosQueryBuilder(currentQuery.queryStartingAfterValues(values.filterNotNull()))

    override fun endBefore(vararg values: Any?): FirestoreQueryBuilder = IosQueryBuilder(currentQuery.queryEndingBeforeValues(values.filterNotNull()))

    override suspend fun get(): Result<List<DocumentSnapshot>> =
        suspendCancellableCoroutine { continuation ->
            currentQuery.getDocumentsWithCompletion { snapshot, error ->
                continuation.resume(
                    if (error != null) {
                        Result.failure(error.toThrowable())
                    } else {
                        Result.success(snapshot?.documents.orEmpty().map { (it as FIRDocumentSnapshot).toDocumentSnapshot() })
                    },
                )
            }
        }

    override fun snapshots(): Flow<Result<List<DocumentSnapshot>>> =
        callbackFlow {
            val registration =
                currentQuery.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error.toThrowable()))
                    } else if (snapshot != null) {
                        trySend(Result.success(snapshot.documents.map { (it as FIRDocumentSnapshot).toDocumentSnapshot() }))
                    }
                }
            awaitClose { registration.remove() }
        }
}
