package com.walcker.games.features.ui.player_search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.Sport
import com.walcker.games.strings.PlayerSearchStrings
import com.walcker.match.cedar.components.CedarFilterSection
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarScreenTitle
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens

private const val MAX_RATING = 5f

/** Aceita vírgula: num teclado pt-BR, o separador decimal é ela. */
private fun String.toRatingOrNull(): Float? =
    replace(',', '.').toFloatOrNull()?.coerceIn(0f, MAX_RATING)

/**
 * Filtros avançados da busca de jogadores, numa bottom sheet.
 *
 * Só nota e esporte: os filtros por número de partidas saíram no Sprint 3 porque
 * nada escreve aqueles contadores, então eles excluíam todo mundo em silêncio.
 *
 * O que mudou nesta repaginação:
 * - **Dez `FilterChip` de largura cheia, um por linha.** Aqui a seleção múltipla
 *   está certa — esportes favoritos são um conjunto — mas dez linhas de pílula
 *   gigante empurravam os botões para fora da sheet. Virou [FlowRow].
 * - **"Aplicar" dividia a linha com "Limpar filtros".** A ação que fecha a sheet
 *   agora ocupa a linha; limpar é texto abaixo.
 * - **Não dava para digitar uma nota decimal.** Ver [RatingBoundField].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlayerFiltersPanel(
    filters: PlayerSearchFilters,
    strings: PlayerSearchStrings,
    onFiltersChanged: (filters: PlayerSearchFilters) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CedarTokens.spacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        CedarScreenTitle(title = strings.filtersTitle)

        CedarFilterSection(label = strings.ratingSection) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                RatingBoundField(
                    value = filters.minRating,
                    label = strings.ratingMin,
                    onValueChange = { onFiltersChanged(filters.copy(minRating = it)) },
                    modifier = Modifier.weight(1f),
                )
                RatingBoundField(
                    value = filters.maxRating,
                    label = strings.ratingMax,
                    onValueChange = { onFiltersChanged(filters.copy(maxRating = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        CedarFilterSection(label = strings.sportsSection) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                Sport.entries.forEach { sport ->
                    val selected = sport in filters.favoriteSports
                    SportChip(
                        label = sport.label,
                        selected = selected,
                        onClick = {
                            val updated = if (selected) {
                                filters.favoriteSports - sport
                            } else {
                                filters.favoriteSports + sport
                            }
                            onFiltersChanged(filters.copy(favoriteSports = updated))
                        },
                    )
                }
            }
        }

        CedarPrimaryButton(
            text = strings.applyFilters,
            onClick = onDismiss,
            modifier = Modifier.padding(top = CedarTokens.spacing.xs),
        )
        CedarTextButton(
            text = strings.clearFilters,
            onClick = onResetFilters,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(CedarTokens.spacing.xl))
    }
}

/**
 * Limite de nota, de 0 a 5.
 *
 * O texto digitado é estado local e só o `Float` sobe. Era o contrário: o campo
 * desenhava `value?.toString()`, então todo texto intermediário que não parseava —
 * `"4,"`, `"4."`, `""` — virava nulo e apagava o que a pessoa estava digitando antes
 * do segundo dígito. Não dava para digitar uma nota decimal.
 *
 * O [LaunchedEffect] existe para o caminho contrário, o "Limpar filtros": quando o
 * valor muda de fora e deixa de corresponder ao texto, o texto acompanha.
 *
 * Um texto impossível limpa o limite em vez de travar o campo — o filtro é uma dica,
 * não um formulário a validar.
 */
@Composable
private fun RatingBoundField(
    value: Float?,
    label: String,
    onValueChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value?.toString().orEmpty()) }

    LaunchedEffect(value) {
        if (text.toRatingOrNull() != value) {
            text = value?.toString().orEmpty()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { typed ->
            text = typed
            onValueChange(typed.toRatingOrNull())
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        shape = CedarTokens.radius.smShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
