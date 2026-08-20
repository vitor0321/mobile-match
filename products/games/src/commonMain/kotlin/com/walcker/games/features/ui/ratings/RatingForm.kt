package com.walcker.games.features.ui.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.model.RatingDimension
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.strings.RatingStrings

private const val MAX_COMMENT_LENGTH = 500

/**
 * Formulário para submeter avaliação de um jogador.
 *
 * Estrelas (1-5) e as quatro dimensões são obrigatórias — `parseRatingDimensions`
 * nas Functions recusa payload sem qualquer uma delas. O comentário é opcional.
 *
 * O botão fica travado até as quatro estarem respondidas, com o aviso à vista
 * desde o começo: deixar enviar e devolver `INVALID_ARGUMENT` genérico jogaria
 * no usuário um erro que a tela já sabia prever.
 */
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = strings.formTitle(playerName),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        StarRatingPicker(
            rating = rating,
            onRatingChange = { rating = it },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it.take(MAX_COMMENT_LENGTH) },
            label = { Text(strings.commentLabel) },
            placeholder = { Text(strings.commentPlaceholder) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            enabled = !isLoading,
        )

        Text(
            text = "${comment.length}/$MAX_COMMENT_LENGTH",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )

        HorizontalDivider()

        Text(
            text = strings.dimensionsTitle,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = strings.dimensionsHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Column e não LazyColumn: são quatro linhas fixas, e este formulário
        // já vive dentro de um bottom sheet rolável — lazy dentro de rolável de
        // altura não limitada quebra em runtime.
        RatingDimension.entries.forEach { dimension ->
            DimensionRow(
                label = dimension.label(strings),
                stars = dimensions[dimension],
                enabled = !isLoading,
                onStarsChange = { stars -> dimensions = dimensions.with(dimension, stars) },
            )
        }

        Button(
            onClick = { onSubmit(rating, comment, dimensions) },
            enabled = !isLoading && dimensions.isComplete,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) strings.submitting else strings.submitAction)
        }
    }
}

/**
 * `when` exaustivo em vez de um mapa nas strings: assim, adicionar uma quinta
 * dimensão não compila até alguém escrever o texto dela nos dois idiomas.
 */
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
    enabled: Boolean,
    onStarsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)

        StarRatingPicker(
            // Zero desenha cinco estrelas vazias — é como uma dimensão ainda
            // não respondida se apresenta. Não dá para voltar a esse estado
            // depois de responder, e não precisa: o envio exige as quatro.
            rating = stars ?: 0,
            onRatingChange = onStarsChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun StarRatingPicker(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val starRating = index + 1
            TextButton(
                onClick = { onRatingChange(starRating) },
                enabled = enabled,
                modifier = Modifier.padding(4.dp),
            ) {
                Text(if (starRating <= rating) "⭐" else "☆")
            }
        }
    }
}
