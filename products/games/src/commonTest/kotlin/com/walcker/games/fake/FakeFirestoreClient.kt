package com.walcker.games.fake

import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient
import com.walcker.match.firestore.FirestoreCollectionReference
import com.walcker.match.firestore.FirestoreDocumentReference
import com.walcker.match.firestore.FirestoreQueryBuilder
import com.walcker.match.firestore.FirestoreTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal data class FunctionCall(
    val name: String,
    val data: Map<String, Any?>,
)

internal data class DocumentWrite(
    val path: String,
    val data: Map<String, Any?>,
    val kind: Kind,
) {
    enum class Kind { SET, MERGE, UPDATE, ADD, DELETE }
}

internal class FakeFirestoreClient : FirestoreClient {
    private val documents = linkedMapOf<String, Map<String, Any?>>()
    private val version = MutableStateFlow(0)
    private var nextGeneratedId = 1

    val functionCalls: MutableList<FunctionCall> = mutableListOf()
    val writes: MutableList<DocumentWrite> = mutableListOf()
    val functionResults: MutableMap<String, Result<Map<String, Any?>>> = mutableMapOf()
    val failingPaths: MutableMap<String, Throwable> = mutableMapOf()

    fun seed(
        path: String,
        data: Map<String, Any?>,
    ) {
        documents[path] = data
        version.value++
    }

    fun data(path: String): Map<String, Any?>? = documents[path]

    override fun document(path: String): FirestoreDocumentReference = Document(path)

    override fun collection(path: String): FirestoreCollectionReference = Collection(path)

    override fun query(path: String): FirestoreQueryBuilder = Query(path, collectionGroup = true)

    override suspend fun <T> runTransaction(block: suspend (FirestoreTransaction) -> T): Result<T> = Result.failure(UnsupportedOperationException("transactions are not used by the games sources"))

    override suspend fun callFunction(
        name: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> {
        functionCalls += FunctionCall(name, data)
        return functionResults[name] ?: Result.success(emptyMap())
    }

    private fun snapshotOf(path: String): DocumentSnapshot? =
        documents[path]?.let { data ->
            DocumentSnapshot(path = path, id = path.substringAfterLast('/'), data = data, exists = true)
        }

    private fun write(
        path: String,
        data: Map<String, Any?>,
        kind: DocumentWrite.Kind,
    ) {
        writes += DocumentWrite(path, data, kind)
        version.value++
    }

    private inner class Document(
        override val path: String,
    ) : FirestoreDocumentReference {
        override suspend fun get(): Result<DocumentSnapshot?> {
            failingPaths[path]?.let { return Result.failure(it) }
            return Result.success(snapshotOf(path))
        }

        override fun snapshots(): Flow<Result<DocumentSnapshot?>> = version.map { failingPaths[path]?.let { Result.failure(it) } ?: Result.success(snapshotOf(path)) }

        override suspend fun delete(): Result<Unit> {
            failingPaths[path]?.let { return Result.failure(it) }
            documents.remove(path)
            write(path, emptyMap(), DocumentWrite.Kind.DELETE)
            return Result.success(Unit)
        }

        override suspend fun set(
            data: Map<String, Any?>,
            merge: Boolean,
        ): Result<Unit> {
            failingPaths[path]?.let { return Result.failure(it) }
            documents[path] = if (merge) documents[path].orEmpty() + data else data
            write(path, data, if (merge) DocumentWrite.Kind.MERGE else DocumentWrite.Kind.SET)
            return Result.success(Unit)
        }

        override suspend fun update(data: Map<String, Any?>): Result<Unit> {
            failingPaths[path]?.let { return Result.failure(it) }
            val existing = documents[path] ?: return Result.failure(IllegalStateException("NOT_FOUND: $path"))
            documents[path] = existing + data
            write(path, data, DocumentWrite.Kind.UPDATE)
            return Result.success(Unit)
        }

        override fun collection(name: String): FirestoreCollectionReference = Collection("$path/$name")
    }

    private inner class Collection(
        override val path: String,
    ) : FirestoreCollectionReference {
        override fun query(): FirestoreQueryBuilder = Query(path, collectionGroup = false)

        override suspend fun list(): Result<List<DocumentSnapshot>> = query().get()

        override fun snapshots(query: FirestoreQueryBuilder?): Flow<Result<List<DocumentSnapshot>>> = (query ?: query()).snapshots()

        override suspend fun add(data: Map<String, Any?>): Result<String> {
            failingPaths[path]?.let { return Result.failure(it) }
            val id = "generated-${nextGeneratedId++}"
            documents["$path/$id"] = data
            write("$path/$id", data, DocumentWrite.Kind.ADD)
            return Result.success(id)
        }
    }

    private data class Filter(
        val field: String,
        val operator: String,
        val value: Any?,
    )

    private inner class Query(
        private val path: String,
        private val collectionGroup: Boolean,
        private val filters: List<Filter> = emptyList(),
        private val orders: List<Pair<String, String>> = emptyList(),
        private val limitCount: Int? = null,
        private val afterValues: List<Any?>? = null,
    ) : FirestoreQueryBuilder {
        private fun copy(
            filters: List<Filter> = this.filters,
            orders: List<Pair<String, String>> = this.orders,
            limitCount: Int? = this.limitCount,
            afterValues: List<Any?>? = this.afterValues,
        ) = Query(path, collectionGroup, filters, orders, limitCount, afterValues)

        override fun where(
            field: String,
            operator: String,
            value: Any?,
        ): FirestoreQueryBuilder = copy(filters = filters + Filter(field, operator, value))

        override fun orderBy(
            field: String,
            direction: String,
        ): FirestoreQueryBuilder = copy(orders = orders + (field to direction))

        override fun orderByDocumentId(direction: String): FirestoreQueryBuilder = copy(orders = orders + (DOCUMENT_ID to direction))

        override fun limit(count: Int): FirestoreQueryBuilder = copy(limitCount = count)

        override fun startAfter(vararg values: Any?): FirestoreQueryBuilder = copy(afterValues = values.toList())

        override fun startAt(vararg values: Any?): FirestoreQueryBuilder = unsupported("startAt")

        override fun endAt(vararg values: Any?): FirestoreQueryBuilder = unsupported("endAt")

        override fun endBefore(vararg values: Any?): FirestoreQueryBuilder = unsupported("endBefore")

        private fun unsupported(name: String): Nothing = throw UnsupportedOperationException("$name is not used by the games sources")

        override suspend fun get(): Result<List<DocumentSnapshot>> {
            failingPaths[path]?.let { return Result.failure(it) }
            return Result.success(evaluate())
        }

        override fun snapshots(): Flow<Result<List<DocumentSnapshot>>> = version.map { failingPaths[path]?.let { Result.failure(it) } ?: Result.success(evaluate()) }

        private fun belongsHere(documentPath: String): Boolean {
            val parent = documentPath.substringBeforeLast('/')
            return if (collectionGroup) parent.substringAfterLast('/') == path else parent == path
        }

        private fun evaluate(): List<DocumentSnapshot> {
            val matching =
                documents.keys
                    .filter(::belongsHere)
                    .mapNotNull(::snapshotOf)
                    .filter { snapshot -> filters.all { it.accepts(snapshot.data[it.field]) } }
            val ordered = orders.foldRight(matching) { (field, direction), list -> list.sortedWith(fieldComparator(field, direction)) }
            val afterCursor = afterValues?.let { cursor -> ordered.filter { isAfter(it, cursor) } } ?: ordered
            return limitCount?.let { afterCursor.take(it) } ?: afterCursor
        }

        private fun isAfter(
            snapshot: DocumentSnapshot,
            cursor: List<Any?>,
        ): Boolean {
            orders.zip(cursor).forEach { (order, cursorValue) ->
                val comparison = compareFieldValues(snapshot.valueOf(order.first), cursorValue)
                val directed = if (order.second == "desc") -comparison else comparison
                if (directed != 0) return directed > 0
            }
            return false
        }

        private fun fieldComparator(
            field: String,
            direction: String,
        ): Comparator<DocumentSnapshot> {
            val ascending = Comparator<DocumentSnapshot> { a, b -> compareFieldValues(a.valueOf(field), b.valueOf(field)) }
            return if (direction == "desc") ascending.reversed() else ascending
        }

        private fun Filter.accepts(actual: Any?): Boolean =
            when (operator) {
                "==" -> compareFieldValues(actual, value) == 0 && actual != null
                ">=" -> actual != null && compareFieldValues(actual, value) >= 0
                "<=" -> actual != null && compareFieldValues(actual, value) <= 0
                ">" -> actual != null && compareFieldValues(actual, value) > 0
                "<" -> actual != null && compareFieldValues(actual, value) < 0
                "array-contains" -> (actual as? List<*>)?.contains(value) == true
                "in" -> (value as? List<*>)?.any { compareFieldValues(actual, it) == 0 } == true
                else -> throw UnsupportedOperationException("operator $operator is not supported by the fake")
            }
    }
}

private const val DOCUMENT_ID = "__name__"

private fun DocumentSnapshot.valueOf(field: String): Any? = if (field == DOCUMENT_ID) id else data[field]

private fun compareFieldValues(
    a: Any?,
    b: Any?,
): Int =
    when {
        a == null && b == null -> 0
        a == null -> -1
        b == null -> 1
        a is Number && b is Number -> a.toDouble().compareTo(b.toDouble())
        a is String && b is String -> a.compareTo(b)
        a is Boolean && b is Boolean -> a.compareTo(b)
        else -> a.toString().compareTo(b.toString())
    }
