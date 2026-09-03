@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.ui.home.component.GameList
import com.walcker.games.features.ui.myMatches.component.MyMatchCard
import com.walcker.games.features.ui.playerProfile.component.AvailabilityCard
import com.walcker.games.features.ui.shared.matchDetail.component.JoinBar
import com.walcker.games.features.ui.shared.matchDetail.component.ParticipantRow
import com.walcker.games.features.ui.shared.matchDetail.component.StatusBadge
import com.walcker.games.features.ui.shared.notifications.component.NotificationItemRow
import com.walcker.games.features.ui.shared.playerDetails.component.RatingCard
import com.walcker.games.features.ui.shared.playerSearch.component.PlayerSearchResultCard
import com.walcker.games.strings.PtBrGamesStrings
import com.walcker.match.cedar.components.CedarAvailabilityButton
import com.walcker.match.cedar.components.CedarPrimaryButton
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test

class GamesComponentsTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private fun snapshot(
        darkTheme: Boolean = false,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        paparazzi.snapshot {
            GamesSnapshotTheme(darkTheme = darkTheme) { content() }
        }
    }

    @Test
    fun myMatchCard_organizer_lightMode() =
        snapshot {
            MyMatchCard(
                myMatch = fakeMyMatch(role = MatchRole.ORGANIZER),
                organizerBadge = "Organizador",
                participantBadge = "Participante",
                cancelActionLabel = "Cancelar",
                leaveActionLabel = "Sair",
                statusCancelledLabel = "Cancelada",
                statusFinishedLabel = "Encerrada",
                playersLabel = "6/10 jogadores",
                ratingsCountLabel = { n -> "$n avaliações" },
                isPast = false,
                onActionClick = {},
            )
        }

    @Test
    fun myMatchCard_participant_darkMode() =
        snapshot(darkTheme = true) {
            MyMatchCard(
                myMatch = fakeMyMatch(role = MatchRole.PARTICIPANT),
                organizerBadge = "Organizador",
                participantBadge = "Participante",
                cancelActionLabel = "Cancelar",
                leaveActionLabel = "Sair",
                statusCancelledLabel = "Cancelada",
                statusFinishedLabel = "Encerrada",
                playersLabel = "6/10 jogadores",
                ratingsCountLabel = { n -> "$n avaliações" },
                isPast = false,
                onActionClick = {},
            )
        }

    @Test
    fun myMatchCard_past_lightMode() =
        snapshot {
            MyMatchCard(
                myMatch = fakeMyMatch(role = MatchRole.PARTICIPANT, game = fakeGame(status = MatchStatus.FINISHED)),
                organizerBadge = "Organizador",
                participantBadge = "Participante",
                cancelActionLabel = "Cancelar",
                leaveActionLabel = "Sair",
                statusCancelledLabel = "Cancelada",
                statusFinishedLabel = "Encerrada",
                playersLabel = "10/10 jogadores",
                ratingsCountLabel = { n -> "$n avaliações" },
                isPast = true,
                onActionClick = {},
            )
        }

    @Test
    fun gameList_lightMode() =
        snapshot {
            GameList(
                strings = PtBrGamesStrings.gameList,
                games = persistentListOf(fakeGame(id = "match-1"), fakeGame(id = "match-2", venueName = "Arena Vila Nova")),
                onClick = {},
            )
        }

    @Test
    fun statusBadge_open_lightMode() =
        snapshot {
            StatusBadge(status = MatchStatus.OPEN, isMatchOver = false, detail = PtBrGamesStrings.matchDetail)
        }

    @Test
    fun statusBadge_cancelled_lightMode() =
        snapshot {
            StatusBadge(status = MatchStatus.CANCELLED, isMatchOver = false, detail = PtBrGamesStrings.matchDetail)
        }

    @Test
    fun joinBar_enabled_lightMode() =
        snapshot {
            JoinBar(
                label = "Entrar na partida",
                priceLabel = "R$ 15,00",
                enabled = true,
                isLoading = false,
                useAvailabilityTone = true,
                onClick = {},
            )
        }

    @Test
    fun joinBar_loading_lightMode() =
        snapshot {
            JoinBar(
                label = "Entrar na partida",
                priceLabel = "R$ 15,00",
                enabled = false,
                isLoading = true,
                useAvailabilityTone = true,
                onClick = {},
            )
        }

    @Test
    fun availabilityCard_available_lightMode() =
        snapshot {
            AvailabilityCard(
                isAvailable = true,
                isUpdating = false,
                strings = PtBrGamesStrings.playerProfile,
                onCheckedChange = {},
            )
        }

    @Test
    fun availabilityCard_unavailable_darkMode() =
        snapshot(darkTheme = true) {
            AvailabilityCard(
                isAvailable = false,
                isUpdating = false,
                strings = PtBrGamesStrings.playerProfile,
                onCheckedChange = {},
            )
        }

    @Test
    fun playerSearchResultCard_lightMode() =
        snapshot {
            PlayerSearchResultCard(
                player = fakePlayerSearchResult(),
                ratingLabel = "4.6",
                ratingAccessibilityLabel = "Nota 4.6 de 5",
                onPlayerSelected = {},
            )
        }

    @Test
    fun ratingCard_lightMode() =
        snapshot {
            RatingCard(
                rating = fakeRating(),
                ratingLabel = "5.0",
                ratingAccessibilityLabel = "Nota 5 de 5",
            )
        }

    @Test
    fun notificationItemRow_unread_lightMode() =
        snapshot {
            NotificationItemRow(
                notification = fakeNotification(isRead = false),
                strings = PtBrGamesStrings.notificationHistory,
                onTap = {},
                onMarkAsRead = {},
                onDelete = {},
            )
        }

    @Test
    fun notificationItemRow_read_darkMode() =
        snapshot(darkTheme = true) {
            NotificationItemRow(
                notification = fakeNotification(isRead = true),
                strings = PtBrGamesStrings.notificationHistory,
                onTap = {},
                onMarkAsRead = {},
                onDelete = {},
            )
        }

    @Test
    fun participantRow_confirmed_canRate_lightMode() =
        snapshot {
            ParticipantRow(
                participant = fakeParticipant(isConfirmed = true),
                statusLabel = "Confirmado",
                paidLabel = "Pago",
                rateLabel = "Avaliar",
                canRate = true,
                canReport = true,
                reportStrings = PtBrGamesStrings.reports,
                onReportPlayer = { _, _ -> },
                onRatePlayer = { _, _ -> },
                ratingSummary = null,
                ratingsCountLabel = { n -> "$n avaliações" },
            )
        }

    @Test
    fun participantRow_waitlisted_lightMode() =
        snapshot {
            ParticipantRow(
                participant = fakeParticipant(isConfirmed = false, positionInWaitlist = 2),
                statusLabel = "Fila de espera (#2)",
                paidLabel = "Pago",
                rateLabel = "Avaliar",
                canRate = false,
                canReport = false,
                reportStrings = PtBrGamesStrings.reports,
                onReportPlayer = { _, _ -> },
                onRatePlayer = { _, _ -> },
                ratingSummary = null,
                ratingsCountLabel = { n -> "$n avaliações" },
            )
        }

    @Test
    fun primaryButton_loading_lightMode() =
        snapshot {
            CedarPrimaryButton(text = "Criar e publicar", onClick = {}, loading = true)
        }

    @Test
    fun availabilityButton_loading_lightMode() =
        snapshot {
            CedarAvailabilityButton(text = "Estou disponível", onClick = {}, loading = true)
        }
}
