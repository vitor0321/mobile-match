package com.walcker.match.firestore

import kotlinx.coroutines.flow.Flow

public interface FirestoreClient {
    public fun document(path: String): FirestoreDocumentReference

    public fun collection(path: String): FirestoreCollectionReference

    public fun query(path: String): FirestoreQueryBuilder

    public suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T>

    public suspend fun callFunction(
        name: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>>
}

public interface FirestoreDocumentReference {
    public val path: String

    public suspend fun get(): Result<DocumentSnapshot?>

    public fun snapshots(): Flow<Result<DocumentSnapshot?>>

    public suspend fun delete(): Result<Unit>

    public suspend fun set(
        data: Map<String, Any?>,
        merge: Boolean = false,
    ): Result<Unit>

    public suspend fun update(data: Map<String, Any?>): Result<Unit>

    public fun collection(name: String): FirestoreCollectionReference
}

public interface FirestoreCollectionReference {
    public val path: String

    public fun query(): FirestoreQueryBuilder

    public suspend fun list(): Result<List<DocumentSnapshot>>

    public fun snapshots(query: FirestoreQueryBuilder? = null): Flow<Result<List<DocumentSnapshot>>>

    public suspend fun add(data: Map<String, Any?>): Result<String>
}

public interface FirestoreQueryBuilder {
    public fun where(
        field: String,
        operator: String,
        value: Any?,
    ): FirestoreQueryBuilder

    public fun orderBy(
        field: String,
        direction: String = "asc",
    ): FirestoreQueryBuilder

    public fun limit(count: Int): FirestoreQueryBuilder

    public fun startAt(vararg values: Any?): FirestoreQueryBuilder

    public fun endAt(vararg values: Any?): FirestoreQueryBuilder

    public fun startAfter(vararg values: Any?): FirestoreQueryBuilder

    public fun endBefore(vararg values: Any?): FirestoreQueryBuilder

    public suspend fun get(): Result<List<DocumentSnapshot>>

    public fun snapshots(): Flow<Result<List<DocumentSnapshot>>>
}

public data class DocumentSnapshot(
    public val path: String,
    public val id: String,
    public val data: Map<String, Any?>,
    public val exists: Boolean,
    public val metadata: SnapshotMetadata = SnapshotMetadata(),
) {
    public inline fun <reified T> get(field: String): T? = data[field] as? T

    public fun getString(field: String): String? = get(field)

    public fun getDouble(field: String): Double? = get(field)

    public fun getLong(field: String): Long? = get(field)

    public fun getBoolean(field: String): Boolean? = get(field)

    public fun getMap(field: String): Map<String, Any?>? = get(field)

    public fun getList(field: String): List<Any?>? = get(field)

    public fun getTimestamp(field: String): Long? = normalizeTimestampMillis(data[field])
}

public data class SnapshotMetadata(
    public val isFromCache: Boolean = false,
    public val hasPendingWrites: Boolean = false,
)

public interface FirestoreTransaction {
    public suspend fun get(path: String): Result<DocumentSnapshot?>

    public suspend fun set(
        path: String,
        data: Map<String, Any?>,
    ): Result<Unit>

    public suspend fun update(
        path: String,
        data: Map<String, Any?>,
    ): Result<Unit>

    public suspend fun delete(path: String): Result<Unit>
}

public expect fun createFirestoreClient(): FirestoreClient
