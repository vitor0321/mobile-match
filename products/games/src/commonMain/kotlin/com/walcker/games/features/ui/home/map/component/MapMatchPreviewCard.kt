package com.walcker.games.features.ui.home.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.walcker.games.features.ui.home.map.model.MatchPreview
import com.walcker.games.features.ui.shared.matchDetail.component.LocationAppDialog
import com.walcker.games.strings.GameListStrings
import com.walcker.games.strings.MapStrings
import com.walcker.match.cedar.components.MatchCard
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.geo.formatDistance

private val CloseButtonSize = 48.dp

@Composable
internal fun MapMatchPreviewCard(
    preview: MatchPreview,
    strings: MapStrings,
    gameListStrings: GameListStrings,
    onDismiss: () -> Unit,
    onDetailsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = preview.game
    var showLocationChooser by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Box(modifier = modifier) {
        MatchCard(
            venueName = game.venueName,
            startsAtSeconds = game.startsAtSeconds,
            metaLabel =
                buildString {
                    append(game.sport.label)
                    preview.distanceKm?.let {
                        append(" · ")
                        append(formatDistance(it))
                    }
                },
            priceLabel = game.pricePerPlayer?.let { gameListStrings.perPlayer(it) },
            slotsLabel = gameListStrings.slotsBadge(game.openSlots),
            openSlots = game.openSlots,
            joinButtonLabel = strings.previewDetailsAction,
            onJoinClick = { onDetailsClick(game.id) },
            modifier = Modifier.padding(top = CloseButtonSize + CedarTokens.spacing.xs),
        )

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = CedarTokens.elevation.overlay,
            ) {
                IconButton(onClick = { showLocationChooser = true }, modifier = Modifier.size(CloseButtonSize)) {
                    Icon(
                        imageVector = Icons.Filled.Directions,
                        contentDescription = strings.directionsContentDescription,
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = CedarTokens.elevation.overlay,
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(CloseButtonSize)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.closeContentDescription,
                    )
                }
            }
        }
    }

    if (showLocationChooser) {
        LocationAppDialog(
            title = strings.openLocationTitle,
            googleMapsLabel = strings.openInGoogleMaps,
            wazeLabel = strings.openInWaze,
            cancelLabel = strings.openLocationCancel,
            onGoogleMaps = {
                uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=${game.lat},${game.lng}")
                showLocationChooser = false
            },
            onWaze = {
                uriHandler.openUri("https://waze.com/ul?ll=${game.lat},${game.lng}&navigate=yes")
                showLocationChooser = false
            },
            onDismiss = { showLocationChooser = false },
        )
    }
}
