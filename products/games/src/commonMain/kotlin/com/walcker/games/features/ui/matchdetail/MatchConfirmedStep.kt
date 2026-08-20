package com.walcker.games.features.ui.matchdetail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarCodeBlock
import com.walcker.match.cedar.components.CedarSuccessScreen
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatWhen

/**
 * Confirmation after joining a match — screen 04 of the redesign.
 *
 * New screen: today joining shows a snackbar and leaves the user on the detail
 * screen, so the moment the product exists for ("I have a game tonight") passes
 * without anything marking it.
 *
 * Deliberately dumb — it takes the facts it renders instead of reading a
 * StepModel. The data is already in hand at the moment the join succeeds, and a
 * confirmation screen that re-fetches can contradict the thing it is confirming.
 *
 * @param matchCode short code the user reads out at the venue. The domain has no
 *   such field yet — `Game` carries only `id` — so this stays null until product
 *   decides what the code is. The block is hidden when null rather than showing a
 *   raw Firestore id.
 */
internal data class MatchConfirmedStep(
    val matchId: String,
    val venueName: String,
    val startsAtSeconds: Long,
    val sportLabel: String,
    val matchCode: String? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val strings = rememberGamesStrings().strings.matchConfirmed

        Scaffold(containerColor = CedarTokens.colors.canvas) { padding ->
            CedarSuccessScreen(
                title = strings.title,
                subtitle = strings.subtitle,
                primaryActionLabel = strings.viewDetails,
                onPrimaryAction = { navigator.replace(MatchDetailStep(matchId)) },
                secondaryActionLabel = strings.backToMatches,
                onSecondaryAction = { navigator.pop() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                summary = {
                    Text(
                        text = venueName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formatWhen(startsAtSeconds = startsAtSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = sportLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
