package com.walcker.games.features.ui.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp

/**
 * Formulário para submeter avaliação de um jogador.
 *
 * Inclui seleção de estrelas (1-5) e campo de comentário opcional.
 */
@Composable
internal fun RatingForm(
    playerName: String,
    onSubmit: (rating: Int, comment: String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Text(
            text = "Avaliar: $playerName",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        // Star rating selector
        StarRatingPicker(
            rating = rating,
            onRatingChange = { rating = it },
            modifier = Modifier.fillMaxWidth(),
        )

        // Comment field
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it.take(500) }, // Max 500 chars
            label = { Text("Comentário (opcional)") },
            placeholder = { Text("Compartilhe sua experiência...") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            enabled = !isLoading,
        )

        // Character count
        Text(
            text = "${comment.length}/500",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )

        // Submit button
        Button(
            onClick = { onSubmit(rating, comment) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) "Enviando..." else "Enviar Avaliação")
        }
    }
}

@Composable
internal fun StarRatingPicker(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val starRating = index + 1
            Button(
                onClick = { onRatingChange(starRating) },
                modifier = Modifier.padding(4.dp),
            ) {
                Text(if (starRating <= rating) "⭐" else "☆", modifier = Modifier.padding(8.dp))
            }
        }
    }
}
