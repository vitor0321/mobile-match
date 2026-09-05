package com.walcker.games.features.ui.shared.matchDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.shared.common.icon
import com.walcker.games.features.ui.shared.matchDetail.component.IconInfoRow
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarCodeBlock
import com.walcker.match.cedar.components.CedarSuccessScreen
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatDayLabel
import com.walcker.match.core.datetime.formatTimeRange
import com.walcker.match.navigator.MatchDetailCoordinator
import org.koin.compose.koinInject

internal data class MatchConfirmedStep(
    val matchId: String,
    val venueName: String,
    val startsAtSeconds: Long,
    val durationMin: Int,
    val sport: Sport,
    val matchCode: String? = null,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val matchDetailCoordinator = koinInject<MatchDetailCoordinator>()
        val strings = rememberGamesStrings().strings.matchConfirmed
        val detail = rememberGamesStrings().strings.matchDetail

        Scaffold(containerColor = CedarTokens.colors.canvas) { padding ->
            CedarSuccessScreen(
                title = strings.title,
                subtitle = strings.subtitle,
                primaryActionLabel = strings.viewDetails,
                onPrimaryAction = {
                    navigator.pop()
                    matchDetailCoordinator.open(matchId)
                },
                secondaryActionLabel = strings.backToMatches,
                onSecondaryAction = { navigator.pop() },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                summary = {
                    Text(
                        text = venueName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
                        IconInfoRow(
                            icon = Icons.Filled.CalendarMonth,
                            text = formatDayLabel(startsAtSeconds = startsAtSeconds),
                        )
                        IconInfoRow(
                            icon = Icons.Filled.Schedule,
                            text = "${formatTimeRange(startsAtSeconds, durationMin)} (${detail.durationValue(durationMin)})",
                        )
                        IconInfoRow(
                            icon = sport.icon(),
                            text = sport.label,
                        )
                    }
                    if (matchCode != null) {
                        CedarCodeBlock(
                            label = strings.codeLabel,
                            code = matchCode,
                            modifier = Modifier.padding(top = CedarTokens.spacing.sm),
                        )
                    }
                },
            )
        }
    }
}
