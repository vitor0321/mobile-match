package com.walcker.games.features.data.shared.repository

import com.walcker.games.fake.FakeNotificationSource
import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationRepositoryImplTest {
    private val item =
        NotificationHistoryItem(
            id = "notif-1",
            title = "Partida confirmada",
            body = "Sua vaga foi confirmada.",
            receivedAt = 1_700_000_000_000L,
        )

    @Test
    fun `returns the history the source provides`() =
        runTest {
            val source = FakeNotificationSource(historyResult = { listOf(item) })
            val repository = NotificationRepositoryImpl(source)

            val history = repository.getNotificationHistory("user-1", limit = 20).getOrThrow()

            assertEquals(listOf(item), history)
        }

    @Test
    fun `a source failure becomes a Result failure instead of throwing`() =
        runTest {
            val source = FakeNotificationSource(historyResult = { error("offline") })
            val repository = NotificationRepositoryImpl(source)

            val result = repository.getNotificationHistory("user-1", limit = 20)

            assertTrue(result.isFailure)
        }

    @Test
    fun `markNotificationAsRead delegates to the source`() =
        runTest {
            val source = FakeNotificationSource()
            val repository = NotificationRepositoryImpl(source)

            repository.markNotificationAsRead("user-1", "notif-1")

            assertEquals(1, source.markAsReadCallCount)
        }

    @Test
    fun `deleteNotification delegates to the source`() =
        runTest {
            val source = FakeNotificationSource()
            val repository = NotificationRepositoryImpl(source)

            repository.deleteNotification("user-1", "notif-1")

            assertEquals(1, source.deleteCallCount)
        }
}
