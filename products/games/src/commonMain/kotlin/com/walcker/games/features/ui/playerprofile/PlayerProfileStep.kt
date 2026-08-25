package com.walcker.games.features.ui.playerprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarStat
import com.walcker.match.cedar.components.CedarStatRow
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.components.RatingStars
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatShortDate
import com.walcker.match.core.format.formatDecimal
import kotlinx.collections.immutable.persistentListOf

/**
 * The signed-in player's own profile.
 *
 * Rebuilt for the redesign: avatar and name at the top, three stat cards, then the
 * ratings history — the shape of screen 05 in the Figma.
 *
 * It also fixes a build break nobody could see: `formatRatingDate` called
 * `System.currentTimeMillis()`, which is `java.lang.System`. This file is in
 * `commonMain`, so the iOS target could not compile it — and no CI job builds iOS,
 * so nothing said so. Dates now go through `formatShortDate` from `core`, which is
 * the same helper the rating cards elsewhere already use.
 */
internal class PlayerProfileStep : Screen {

    @Composable
    override fun Content() {
        val strings = rememberGamesStrings().strings.playerProfile
        val model = koinScreenModel<PlayerProfileStepModel>()
        val state by model.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.errorMessage) {
            state.errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(PlayerProfileEvent.DismissError)
            }
        }

        LaunchedEffect(state.availabilityErrorMessage) {
            state.availabilityErrorMessage?.let {
                snackbarHostState.showSnackbar(it)
                model.onEvent(PlayerProfileEvent.DismissAvailabilityError)
            }
        }

        Scaffold(
            containerColor = CedarTokens.colors.canvas,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
                contentPadding = PaddingValues(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
            ) {
                item {
                    CedarScreenTitle(title = strings.title)
                }

                item {
                    ProfileHeader(
                        name = state.userName,
                        email = state.userEmail,
                    )
                }

                item {
                    AvailabilityCard(
                        isAvailable = state.isAvailable,
                        isUpdating = state.isUpdatingAvailability,
                        strings = strings,
                        onCheckedChange = { checked ->
                            model.onEvent(PlayerProfileEvent.AvailabilityChanged(checked))
                        },
                    )
                }

                item {
                    CedarStatRow(
                        stats = persistentListOf(
                            CedarStat(
                                value = state.matchesOrganized.toString(),
                                label = strings.statsOrganized,
                            ),
                            CedarStat(
                                value = state.matchesParticipated.toString(),
                                label = strings.statsParticipated,
                            ),
                            CedarStat(
                                value = if (state.totalRatings > 0) {
                                    formatDecimal(value = state.averageRating, decimals = 1)
                                } else {
                                    strings.noRatingYet
                                },
                                label = strings.statsRating,
                                highlighted = state.totalRatings > 0,
                            ),
                        ),
                    )
                }

                if (state.totalRatings > 0) {
                    item {
                        CedarSectionHeader(
                            title = strings.ratingsReceived,
                            subtitle = strings.ratingsCount(state.totalRatings),
                        )
                    }
                    items(items = state.ratings, key = { it.id }) { rating ->
                        RatingItemCard(rating = rating, strings = strings)
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = CedarTokens.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                    ) {
                        Text(
                            text = strings.settings,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CedarSecondaryButton(
                            text = strings.logout,
                            onClick = { model.onEvent(PlayerProfileEvent.LogoutRequested) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String?,
    email: String?,
    modifier: Modifier = Modifier,
) {
    if (name == null) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(displayName = name, size = PlayerAvatarSize.Large)
        Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (email != null) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * O switch é otimista — acompanha o dedo e o StepModel reverte se a gravação
 * falhar — mas fica travado enquanto a escrita está em voo, para um toque repetido
 * não virar uma fila de escritas concorrentes no mesmo documento.
 */
@Composable
private fun AvailabilityCard(
    isAvailable: Boolean,
    isUpdating: Boolean,
    strings: PlayerProfileStrings,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.lgShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.availabilityTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // Descreve a consequência, não o estado: "desligado" não diz a
                    // ninguém que vai parar de receber aviso de partida.
                    text = if (isAvailable) {
                        strings.availabilityOnDescription
                    } else {
                        strings.availabilityOffDescription
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(
                checked = isAvailable,
                onCheckedChange = onCheckedChange,
                enabled = !isUpdating,
            )
        }
    }
}

@Composable
private fun RatingItemCard(
    rating: Rating,
    strings: PlayerProfileStrings,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.mdShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(CedarTokens.spacing.md)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Was "⭐ ".repeat(n) + "☆".repeat(5 - n) — a star count a screen
                // reader announced as a string of emoji names.
                RatingStars(
                    rating = rating.rating.toFloat(),
                    contentDescription = strings.ratingContentDescription(rating.rating.toFloat()),
                )
                Text(
                    text = formatShortDate(epochMillis = rating.createdAtMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rating.comment.isNotEmpty()) {
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
