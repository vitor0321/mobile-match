package com.walcker.games.features.ui.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.walcker.games.features.domain.model.RatingDimension
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.strings.RatingStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarStarPicker
import com.walcker.match.cedar.tokens.CedarTokens

private const val MAX_COMMENT_LENGTH = 500

@Composable
internal fun RatingForm(
    playerName: String,
    strings: RatingStrings,
    onSubmit: (rating: Int, comment: String, dimensions: RatingDimensions) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var dimensions by remember { mutableStateOf(RatingDimensions.None) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CedarTokens.spacing.lg,
                vertical = CedarTokens.spacing.md,
            ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        CedarSectionHeader(title = strings.formTitle(playerName))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        ) {
            Text(
                text = strings.overallLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CedarStarPicker(
                rating = rating,
                onRatingChange = { rating = it },
                starContentDescription = strings.starContentDescription,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it.take(MAX_COMMENT_LENGTH) },
            label = { Text(strings.commentLabel) },
            placeholder = { Text(strings.commentPlaceholder) },
            minLines = 3,
            maxLines = 5,
            shape = CedarTokens.radius.smShape,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            enabled = !isLoading,
        )

        Text(
            text = strings.commentCounter(comment.length, MAX_COMMENT_LENGTH),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        CedarSectionHeader(
            title = strings.dimensionsTitle,
            subtitle = strings.dimensionsHint,
        )

        RatingDimension.entries.forEach { dimension ->
            DimensionRow(
                label = dimension.label(strings),
                stars = dimensions[dimension],
                strings = strings,
                enabled = !isLoading,
                onStarsChange = { stars -> dimensions = dimensions.with(dimension, stars) },
            )
        }

        CedarPrimaryButton(
            text = strings.submitAction,
            onClick = { onSubmit(rating, comment, dimensions) },
            enabled = dimensions.isComplete,
            loading = isLoading,
        )
    }
}

private fun RatingDimension.label(strings: RatingStrings): String = when (this) {
    RatingDimension.PUNCTUALITY -> strings.dimensionPunctuality
    RatingDimension.RESPECT -> strings.dimensionRespect
    RatingDimension.FAIR_PLAY -> strings.dimensionFairPlay
    RatingDimension.BEHAVIOR -> strings.dimensionBehavior
}

@Composable
private fun DimensionRow(
    label: String,
    stars: Int?,
    strings: RatingStrings,
    enabled: Boolean,
    onStarsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        CedarStarPicker(
            rating = stars ?: 0,
            onRatingChange = onStarsChange,
            starContentDescription = strings.starContentDescription,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
