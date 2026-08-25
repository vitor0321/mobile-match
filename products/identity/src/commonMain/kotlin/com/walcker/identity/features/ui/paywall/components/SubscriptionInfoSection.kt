package com.walcker.identity.features.ui.paywall.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.tokens.CedarTokens

/**
 * A letra miúda da cobrança: quando renova, como cancelar.
 *
 * O fundo era `surfaceVariant.copy(alpha = 0.35f)`. Cor translúcida sobre um fundo
 * que o componente não conhece dá um contraste que ninguém consegue calcular — e
 * este é justamente o texto que a loja exige que seja legível. Agora é o mesmo
 * cartão branco sobre canvas que o resto do app usa.
 */
@Composable
internal fun SubscriptionInfoSection(
    title: String,
    explanation: String,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CedarTokens.radius.mdShape,
        color = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
