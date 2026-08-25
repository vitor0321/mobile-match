package com.walcker.games.features.ui.player_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.model.DimensionAverage
import com.walcker.games.features.domain.model.RatingDimension
import com.walcker.games.strings.PlayerDetailsStrings
import com.walcker.games.strings.RatingStrings
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.tokens.CedarTokens

private const val MAX_STARS = 5f

/** A barra do Material tem 4dp por padrão; 6dp lê melhor à distância de um braço. */
private val BarHeight = 6.dp

/**
 * Médias por dimensão do jogador.
 *
 * Só desenha as dimensões que alguém respondeu, e mostra a contagem de cada uma
 * separadamente: como responder é opcional, "4,8 de pontualidade" com uma resposta e
 * com quarenta são coisas muito diferentes, e omitir o número deixaria as duas com a
 * mesma cara.
 *
 * Era um `Card` do Material, que traz sombra e uma cor de container própria — num
 * app cujo relevo vem do canvas azulado atrás de cartões brancos, a sombra briga com
 * o resto. Virou [Surface] plana, como os outros cartões.
 *
 * Quem chama já garantiu que [averages] não está vazio.
 */
@Composable
internal fun DimensionAveragesCard(
    averages: Map<RatingDimension, DimensionAverage>,
    strings: PlayerDetailsStrings,
    ratingStrings: RatingStrings,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CedarTokens.radius.mdShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            CedarSectionHeader(title = strings.dimensionsTitle)

            // Itera pelo enum, não pelo mapa: assim a ordem das linhas é sempre a
            // mesma entre perfis, em vez de depender da ordem de inserção.
            RatingDimension.entries.forEach { dimension ->
                val average = averages[dimension] ?: return@forEach
                DimensionRow(
                    label = dimension.label(ratingStrings),
                    average = average,
                    strings = strings,
                )
            }
        }
    }
}

@Composable
private fun DimensionRow(
    label: String,
    average: DimensionAverage,
    strings: PlayerDetailsStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label: ${strings.ratingAccessibility(average.average)}"
            },
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = strings.ratingValue(average.average),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LinearProgressIndicator(
            // coerceIn porque a barra só aceita 0f..1f: um agregado corrompido no
            // Firestore deve desenhar torto, não derrubar a tela.
            progress = { (average.average / MAX_STARS).coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            // outlineVariant é a divisória, quase invisível; o trilho precisa
            // aparecer para a barra significar alguma coisa.
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight),
        )

        Text(
            text = strings.dimensionCount(average.count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Mesmo `when` exaustivo do formulário: dimensão nova não compila sem texto. */
private fun RatingDimension.label(strings: RatingStrings): String = when (this) {
    RatingDimension.PUNCTUALITY -> strings.dimensionPunctuality
    RatingDimension.RESPECT -> strings.dimensionRespect
    RatingDimension.FAIR_PLAY -> strings.dimensionFairPlay
    RatingDimension.BEHAVIOR -> strings.dimensionBehavior
}
