@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.ui.shared.notifications.NotificationHistoryContent
import com.walcker.games.features.ui.shared.notifications.NotificationHistoryState
import com.walcker.games.strings.PtBrGamesStrings
import org.junit.Rule
import org.junit.Test

class NotificationHistoryStepTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val loadedState =
        NotificationHistoryState(
            notifications =
                listOf(
                    fakeNotification(id = "1"),
                    fakeNotification(id = "2", title = "Partida confirmada", body = "Você entrou na Arena Vila Nova.", isRead = true),
                ),
        )

    private fun snapshot(
        state: NotificationHistoryState,
        darkTheme: Boolean,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) {
                NotificationHistoryContent(
                    state = state,
                    strings = PtBrGamesStrings.notificationHistory,
                    onEvent = {},
                    onClose = {},
                    onNotificationTap = {},
                )
            }
        }
    }

    @Test
    fun content_lightMode() = snapshot(loadedState, darkTheme = false)

    @Test
    fun content_darkMode() = snapshot(loadedState, darkTheme = true)

    @Test
    fun empty_lightMode() = snapshot(NotificationHistoryState(), darkTheme = false)

    @Test
    fun error_lightMode() = snapshot(NotificationHistoryState(errorMessage = "Não foi possível carregar as notificações."), darkTheme = false)
}
