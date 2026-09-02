package com.walcker.games.features.ui.shared.notifications

import com.walcker.games.fake.FakeNotificationRepository
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.games.features.domain.shared.usecase.DeleteNotificationUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.GetNotificationHistoryUseCaseImpl
import com.walcker.games.features.domain.shared.usecase.MarkNotificationAsReadUseCaseImpl
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.PtBrGamesStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryStepModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val stringsHolder = GamesStringsHolder().apply { setStrings(PtBrGamesStrings) }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(
        repository: FakeNotificationRepository,
        sessionHolder: FakeSessionHolder = FakeSessionHolder(),
    ) = NotificationHistoryStepModel(
        getNotificationHistory = GetNotificationHistoryUseCaseImpl(repository),
        markNotificationAsRead = MarkNotificationAsReadUseCaseImpl(repository),
        deleteNotification = DeleteNotificationUseCaseImpl(repository),
        sessionHolder = sessionHolder,
        stringsHolder = stringsHolder,
    )

    private fun item(id: String) =
        NotificationHistoryItem(
            id = id,
            title = "Partida confirmada",
            body = "Sua vaga foi confirmada.",
            receivedAt = 1_700_000_000_000L,
        )

    @Test
    fun `an anonymous user loads nothing`() =
        runTest(testDispatcher) {
            val model = buildModel(FakeNotificationRepository(), FakeSessionHolder(session = null))

            advanceUntilIdle()

            assertTrue(
                model.state.value.notifications
                    .isEmpty(),
            )
            assertTrue(!model.state.value.isLoading)
        }

    @Test
    fun `loads the history for the current user`() =
        runTest(testDispatcher) {
            val repository = FakeNotificationRepository(historyResult = Result.success(listOf(item("n1"))))
            val model = buildModel(repository)

            advanceUntilIdle()

            assertEquals(
                listOf("n1"),
                model.state.value.notifications
                    .map { it.id },
            )
        }

    @Test
    fun `a load failure surfaces an error message`() =
        runTest(testDispatcher) {
            val repository = FakeNotificationRepository(historyResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(repository)

            advanceUntilIdle()

            assertEquals("offline", model.state.value.errorMessage)
        }

    @Test
    fun `marking as read updates only the matching notification`() =
        runTest(testDispatcher) {
            val repository =
                FakeNotificationRepository(historyResult = Result.success(listOf(item("n1"), item("n2"))))
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(NotificationHistoryEvent.MarkAsRead("n1"))
            advanceUntilIdle()

            val notifications =
                model.state.value.notifications
                    .associateBy { it.id }
            assertTrue(notifications.getValue("n1").isRead)
            assertTrue(!notifications.getValue("n2").isRead)
        }

    @Test
    fun `deleting a notification removes it from the list`() =
        runTest(testDispatcher) {
            val repository =
                FakeNotificationRepository(historyResult = Result.success(listOf(item("n1"), item("n2"))))
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(NotificationHistoryEvent.Delete("n1"))
            advanceUntilIdle()

            assertEquals(
                listOf("n2"),
                model.state.value.notifications
                    .map { it.id },
            )
        }

    @Test
    fun `dismissing the error only clears the message`() =
        runTest(testDispatcher) {
            val repository = FakeNotificationRepository(historyResult = Result.failure(IllegalStateException("offline")))
            val model = buildModel(repository)
            advanceUntilIdle()

            model.onEvent(NotificationHistoryEvent.DismissError)

            assertNull(model.state.value.errorMessage)
        }
}
