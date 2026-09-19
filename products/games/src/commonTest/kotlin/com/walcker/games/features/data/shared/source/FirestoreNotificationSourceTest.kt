package com.walcker.games.features.data.shared.source

import com.walcker.games.fake.FakeFirestoreClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreNotificationSourceTest {
    private val history = "users/u1/notificationHistory"

    private fun serverNotification(
        receivedAt: Long,
        isRead: Boolean = false,
        matchId: String = "m1",
    ): Map<String, Any?> =
        mapOf(
            "type" to "new_match",
            "title" to "Partida nova perto de você",
            "body" to "FUTSAL · Green Ball · Centro",
            "data" to mapOf("matchId" to matchId),
            "isRead" to isRead,
            "receivedAt" to receivedAt,
        )

    @Test
    fun `a notification written by the server is read back whole`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("$history/n1", serverNotification(receivedAt = 1_700_000_000_000L)) }

            val item = FirestoreNotificationSource(firestore).getNotificationHistory("u1", limit = 10).single()

            assertEquals("n1", item.id)
            assertEquals("Partida nova perto de você", item.title)
            assertEquals("FUTSAL · Green Ball · Centro", item.body)
            assertEquals(1_700_000_000_000L, item.receivedAt)
            assertFalse(item.isRead)
            assertEquals(mapOf("matchId" to "m1"), item.data)
        }

    @Test
    fun `the history shows the newest first, up to the limit`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("$history/old", serverNotification(receivedAt = 1L))
                    seed("$history/new", serverNotification(receivedAt = 3L))
                    seed("$history/mid", serverNotification(receivedAt = 2L))
                }

            val items = FirestoreNotificationSource(firestore).getNotificationHistory("u1", limit = 2)

            assertEquals(listOf("new", "mid"), items.map { it.id })
        }

    @Test
    fun `a notification missing fields still shows up with safe defaults`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("$history/bare", emptyMap()) }

            val item = FirestoreNotificationSource(firestore).getNotificationHistory("u1", limit = 10).single()

            assertEquals("", item.title)
            assertEquals(0L, item.receivedAt)
            assertEquals(emptyMap(), item.data)
        }

    @Test
    fun `an unreachable history is shown as empty`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { failingPaths[history] = IllegalStateException("offline") }

            assertTrue(FirestoreNotificationSource(firestore).getNotificationHistory("u1", limit = 10).isEmpty())
        }

    @Test
    fun `the unread badge follows the unread notifications`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("$history/read", serverNotification(receivedAt = 1L, isRead = true)) }
            val source = FirestoreNotificationSource(firestore)

            assertFalse(source.observeHasUnread("u1").first())

            firestore.seed("$history/unread", serverNotification(receivedAt = 2L))
            assertTrue(source.observeHasUnread("u1").first())
        }

    @Test
    fun `the unread badge stays off when the history cannot be read`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { failingPaths[history] = IllegalStateException("offline") }

            assertFalse(FirestoreNotificationSource(firestore).observeHasUnread("u1").first())
        }

    @Test
    fun `marking as read and deleting touch only that notification`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("$history/a", serverNotification(receivedAt = 1L))
                    seed("$history/b", serverNotification(receivedAt = 2L))
                }
            val source = FirestoreNotificationSource(firestore)

            source.markNotificationAsRead("u1", "a")
            source.deleteNotification("u1", "b")

            assertEquals(true, firestore.data("$history/a")?.get("isRead"))
            assertNull(firestore.data("$history/b"))
        }

    @Test
    fun `marking a notification that no longer exists is an error`() =
        runTest {
            assertFailsWith<IllegalStateException> { FirestoreNotificationSource(FakeFirestoreClient()).markNotificationAsRead("u1", "gone") }
        }
}
