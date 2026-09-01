@file:Suppress("CAST_NEVER_SUCCEEDS", "UNUSED_PARAMETER", "CONFLICTING_OVERLOADS")

package com.walcker.match.firestore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

public actual fun createFirestoreClient(): FirestoreClient = IosFirestoreClient()

private class IosFirestoreClient : FirestoreClient {
    private val db: Any? = null

    override fun document(path: String): FirestoreDocumentReference = IosDocumentReference(path, db)

    override fun collection(path: String): FirestoreCollectionReference = IosCollectionReference(path, db)

    override fun query(path: String): FirestoreQueryBuilder = IosQueryBuilder(path, db)

    override suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T> = Result.failure(NotImplementedError("iOS Firestore transaction not implemented"))

    override suspend fun callFunction(
        name: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> = Result.failure(NotImplementedError("iOS Firestore callFunction not implemented"))
}

private class IosDocumentReference(
    override val path: String,
    private val db: Any?,
) : FirestoreDocumentReference {
    override suspend fun get(): Result<DocumentSnapshot?> = Result.failure(NotImplementedError("iOS Firestore document.get not implemented"))

    override fun snapshots(): Flow<Result<DocumentSnapshot?>> = flowOf(Result.failure(NotImplementedError("iOS Firestore document.snapshots not implemented")))

    override suspend fun delete(): Result<Unit> = Result.failure(NotImplementedError("iOS Firestore document.delete not implemented"))

    override suspend fun set(data: Map<String, Any?>): Result<Unit> = Result.failure(NotImplementedError("iOS Firestore document.set not implemented"))

    override suspend fun update(data: Map<String, Any?>): Result<Unit> = Result.failure(NotImplementedError("iOS Firestore document.update not implemented"))

    override fun collection(name: String): FirestoreCollectionReference = IosCollectionReference("$path/$name", db)
}

private class IosCollectionReference(
    override val path: String,
    private val db: Any?,
) : FirestoreCollectionReference {
    override fun query(): FirestoreQueryBuilder = IosQueryBuilder(path, db)

    override suspend fun list(): Result<List<DocumentSnapshot>> = Result.failure(NotImplementedError("iOS Firestore collection.list not implemented"))

    override fun snapshots(query: FirestoreQueryBuilder?): Flow<Result<List<DocumentSnapshot>>> = flowOf(Result.failure(NotImplementedError("iOS Firestore collection.snapshots not implemented")))

    override suspend fun add(data: Map<String, Any?>): Result<String> = Result.failure(NotImplementedError("iOS Firestore collection.add not implemented"))
}

private class IosQueryBuilder(
    private val path: String,
    private val db: Any?,
) : FirestoreQueryBuilder {
    override fun where(
        field: String,
        operator: String,
        value: Any?,
    ): FirestoreQueryBuilder = this

    override fun orderBy(
        field: String,
        direction: String,
    ): FirestoreQueryBuilder = this

    override fun limit(count: Int): FirestoreQueryBuilder = this

    override fun startAt(vararg values: Any?): FirestoreQueryBuilder = this

    override fun endAt(vararg values: Any?): FirestoreQueryBuilder = this

    override fun startAfter(vararg values: Any?): FirestoreQueryBuilder = this

    override fun endBefore(vararg values: Any?): FirestoreQueryBuilder = this

    override suspend fun get(): Result<List<DocumentSnapshot>> = Result.failure(NotImplementedError("iOS Firestore query.get not implemented"))

    override fun snapshots(): Flow<Result<List<DocumentSnapshot>>> = flowOf(Result.failure(NotImplementedError("iOS Firestore query.snapshots not implemented")))
}
