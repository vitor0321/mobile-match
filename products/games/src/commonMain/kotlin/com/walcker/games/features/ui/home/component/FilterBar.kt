package com.walcker.games.features.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.ui.home.GameListState
import com.walcker.games.strings.GameListStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun FilterBar(
    strings: GameListStrings,
    selectedSport: Sport?,
    mySports: Set<Sport>,
    radiusKm: Double,
    isRadiusUnlimited: Boolean,
    onSelectSport: (Sport?) -> Unit,
    onRadiusChange: (Double) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = CedarTokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        SportChipsRow(
            strings = strings,
            selectedSport = selectedSport,
            mySports = mySports,
            onSelectSport = onSelectSport,
        )
        if (isRadiusUnlimited) {
            Text(
                text = strings.radiusUnlimitedLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
            )
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CedarTokens.spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.radiusLabel(radiusKm.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = CedarTokens.spacing.sm),
                )
                Slider(
                    value = radiusKm.toFloat(),
                    onValueChange = { onRadiusChange(it.toDouble()) },
                    valueRange = GameListState.MIN_RADIUS_KM.toFloat()..GameListState.MAX_RADIUS_KM.toFloat(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
