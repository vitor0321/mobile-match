package com.walcker.games.features.ui.shared.matchDetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.walcker.games.features.ui.shared.manageMatch.ManageMatchStep
import com.walcker.match.cedar.components.CedarFloatingDialog
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.navigator.MatchDetailCoordinator
import org.koin.compose.koinInject

@Composable
public fun MatchDetailBottomSheet() {
    val coordinator = koinInject<MatchDetailCoordinator>()
    val navigatorHolder = koinInject<NavigatorHolder>()
    val currentMatchId by coordinator.selectedMatchId.collectAsState()
    val matchId = currentMatchId
    println("DEEPLINK_DEBUG MatchDetailBottomSheet recomposed, coordinator=$coordinator matchId=$matchId")

    if (matchId != null) {
        CedarFloatingDialog(onDismiss = { coordinator.close() }, scrollable = false) {
            MatchDetailScreenContent(
                matchId = matchId,
                onDismiss = { coordinator.close() },
                onNavigateToConfirmation = { id, venueName, startsAtSeconds, durationMin, sport ->
                    coordinator.close()
                    navigatorHolder.navigator?.push(
                        MatchConfirmedStep(
                            matchId = id,
                            venueName = venueName,
                            startsAtSeconds = startsAtSeconds,
                            durationMin = durationMin,
                            sport = sport,
                        ),
                    )
                },
                onManageMatch = { id ->
                    coordinator.close()
                    navigatorHolder.navigator?.push(ManageMatchStep(id))
                },
            )
        }
    }
}
