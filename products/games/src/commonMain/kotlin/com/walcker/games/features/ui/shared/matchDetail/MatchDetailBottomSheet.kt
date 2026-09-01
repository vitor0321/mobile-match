package com.walcker.games.features.ui.shared.matchDetail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.walcker.games.features.ui.create.CreateMatchStep
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.navigator.MatchDetailCoordinator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MatchDetailBottomSheet() {
    val coordinator = koinInject<MatchDetailCoordinator>()
    val navigatorHolder = koinInject<NavigatorHolder>()
    val currentMatchId by coordinator.selectedMatchId.collectAsState()
    val matchId = currentMatchId

    if (matchId != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { coordinator.close() },
            sheetState = sheetState,
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            MatchDetailScreenContent(
                matchId = matchId,
                onDismiss = { coordinator.close() },
                onNavigateToConfirmation = { id, venueName, startsAtSeconds, sportLabel ->
                    coordinator.close()
                    navigatorHolder.navigator?.push(
                        MatchConfirmedStep(
                            matchId = id,
                            venueName = venueName,
                            startsAtSeconds = startsAtSeconds,
                            sportLabel = sportLabel,
                        ),
                    )
                },
                onEditMatch = { id ->
                    coordinator.close()
                    navigatorHolder.navigator?.push(CreateMatchStep(id))
                },
            )
        }
    }
}
