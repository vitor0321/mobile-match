package com.walcker.match.firestore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocumentSnapshotTest {
    private fun snapshot(vararg entries: Pair<String, Any?>) =
        DocumentSnapshot(
            path = "profiles/player-1",
            id = "player-1",
            data = mapOf(*entries),
            exists = true,
        )

    @Test
    fun `getDouble reads a value the native SDK deserialized as Long`() {
        // Firestore stores a whole-number field (ex: a single-rating average)
        // as integerValue on the wire, so Android/iOS hand it back as Long,
        // not Double, even though the field is conceptually a double.
        val snap = snapshot("rating" to 5L)

        assertEquals(5.0, snap.getDouble("rating"))
    }

    @Test
    fun `getDouble reads a value the native SDK deserialized as Double`() {
        val snap = snapshot("rating" to 4.5)

        assertEquals(4.5, snap.getDouble("rating"))
    }

    @Test
    fun `getDouble returns null for a missing or non-numeric field`() {
        val snap = snapshot("rating" to "not a number")

        assertNull(snap.getDouble("rating"))
        assertNull(snap.getDouble("missing"))
    }

    @Test
    fun `getLong reads a value the native SDK deserialized as Double`() {
        val snap = snapshot("ratingCount" to 3.0)

        assertEquals(3L, snap.getLong("ratingCount"))
    }

    @Test
    fun `getLong reads a value the native SDK deserialized as Long`() {
        val snap = snapshot("ratingCount" to 3L)

        assertEquals(3L, snap.getLong("ratingCount"))
    }
}
