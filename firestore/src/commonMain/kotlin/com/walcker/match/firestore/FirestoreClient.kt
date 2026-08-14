package com.walcker.match.firestore

import kotlinx.coroutines.flow.Flow

/**
 * Common Firestore API exposed to all products.
 * Implemented separately for Android (native SDK) and iOS (via Objective-C interop).
 *
 * Principles:
 * - No platform-specific types leak into caller code
 * - Result<T> wraps all network operations
 * - Snapshots are pushed as Flow, never polled
 * - Transactions are at-most-once; failures are retried by the client
 */
public interface FirestoreClient {

    /**
     * Returns a reference to a document at the given path.
     * Path format: "collection/doc" or "collection/doc/subcollection/subdoc" (any depth).
     */
    public fun document(path: String): FirestoreDocumentReference

    /**
     * Returns a reference to a collection.
     * Path format: "collection" or "collection/doc/subcollection" (any depth).
     */
    public fun collection(path: String): FirestoreCollectionReference

    /**
     * Builds a query on a collection.
     * Start with [collection], then chain [where], [orderBy], [limit], etc.
     */
    public fun query(path: String): FirestoreQueryBuilder

    /**
     * Runs a read-write transaction with retry logic.
     * The lambda receives a [FirestoreTransaction] for reads and writes within the transaction.
     * If the lambda throws, the transaction is aborted (no retry by default).
     */
    public suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T>

    /**
     * Calls a Cloud Function callable.
     * @param name function name (e.g., "joinMatch")
     * @param data call payload (serializable to JSON)
     * @return JSON response from the function
     */
    public suspend fun callFunction(name: String, data: Map<String, Any?>): Result<Map<String, Any?>>
}

/**
 * Reference to a single document. Methods return immediate results or flows.
 */
public interface FirestoreDocumentReference {
    public val path: String

    /**
     * Fetches the document once.
     */
    public suspend fun get(): Result<DocumentSnapshot?>

    /**
     * Observes the document in real-time.
     */
    public fun snapshots(): Flow<Result<DocumentSnapshot?>>

    /**
     * Deletes the document.
     */
    public suspend fun delete(): Result<Unit>

    /**
     * Sets the document (full write, overwrites).
     */
    public suspend fun set(data: Map<String, Any?>): Result<Unit>

    /**
     * Merges fields into the document (partial update, preserves missing fields).
     */
    public suspend fun update(data: Map<String, Any?>): Result<Unit>

    /**
     * Returns a reference to a subcollection of this document.
     */
    public fun collection(name: String): FirestoreCollectionReference
}

/**
 * Reference to a collection. Methods build queries and allow listing.
 */
public interface FirestoreCollectionReference {
    public val path: String

    /**
     * Returns a builder for filtering and sorting this collection.
     */
    public fun query(): FirestoreQueryBuilder

    /**
     * Lists all documents in the collection (one-shot, not real-time).
     */
    public suspend fun list(): Result<List<DocumentSnapshot>>

    /**
     * Observes all documents (or filtered subset if a query builder is passed).
     */
    public fun snapshots(query: FirestoreQueryBuilder? = null): Flow<Result<List<DocumentSnapshot>>>

    /**
     * Adds a new document with an auto-generated ID.
     */
    public suspend fun add(data: Map<String, Any?>): Result<String>
}

/**
 * Query builder for chaining where, orderBy, limit conditions.
 * Immutable: each method returns a new builder.
 */
public interface FirestoreQueryBuilder {
    public fun where(field: String, operator: String, value: Any?): FirestoreQueryBuilder
    public fun orderBy(field: String, direction: String = "asc"): FirestoreQueryBuilder
    public fun limit(count: Int): FirestoreQueryBuilder
    public fun startAt(vararg values: Any?): FirestoreQueryBuilder
    public fun endAt(vararg values: Any?): FirestoreQueryBuilder
    public fun startAfter(vararg values: Any?): FirestoreQueryBuilder
    public fun endBefore(vararg values: Any?): FirestoreQueryBuilder

    /**
     * Executes this query (one-shot, not real-time).
     */
    public suspend fun get(): Result<List<DocumentSnapshot>>

    /**
     * Observes this query in real-time.
     */
    public fun snapshots(): Flow<Result<List<DocumentSnapshot>>>
}

/**
 * A snapshot of document data at a point in time.
 */
public data class DocumentSnapshot(
    public val path: String,
    public val id: String,
    public val data: Map<String, Any?>,
    public val exists: Boolean,
    public val metadata: SnapshotMetadata = SnapshotMetadata(),
) {
    public inline fun <reified T> get(field: String): T? {
        return data[field] as? T
    }

    public fun getString(field: String): String? = get(field)
    public fun getDouble(field: String): Double? = get(field)
    public fun getLong(field: String): Long? = get(field)
    public fun getBoolean(field: String): Boolean? = get(field)
    public fun getMap(field: String): Map<String, Any?>? = get(field)
    public fun getList(field: String): List<Any?>? = get(field)
    public fun getTimestamp(field: String): Long? = get(field)
}

/**
 * Metadata about a snapshot (e.g., whether it came from the cache or server).
 */
public data class SnapshotMetadata(
    public val isFromCache: Boolean = false,
    public val hasPendingWrites: Boolean = false,
)

/**
 * Read-write transaction. All operations are buffered and committed atomically.
 */
public interface FirestoreTransaction {
    /**
     * Reads a document within the transaction.
     */
    public suspend fun get(path: String): Result<DocumentSnapshot?>

    /**
     * Writes or merges data within the transaction.
     */
    public suspend fun set(path: String, data: Map<String, Any?>): Result<Unit>

    public suspend fun update(path: String, data: Map<String, Any?>): Result<Unit>
    public suspend fun delete(path: String): Result<Unit>
}

/**
 * Creates a Firestore client for the current platform.
 */
public expect fun createFirestoreClient(): FirestoreClient
