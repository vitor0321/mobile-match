package com.walcker.identity.features.ui.paywall.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.tokens.CedarTokens

/**
 * Nenhum plano veio da loja — ou a chamada falhou.
 *
 * A mensagem é uma live region: ela substitui a lista de planos depois de um
 * carregamento, e quem não enxerga precisa saber que o conteúdo trocou.
 *
 * O espaçamento era `Spacer(Modifier.padding(top = 12.dp))` — um `Spacer` sem
 * tamanho, cujo padding é a única coisa que o faz ocupar espaço.
 */
@Composable
internal fun EmptyPaywallState(
    message: String,
    retryLabel: String,
    colors: ColorScheme,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        CedarPrimaryButton(
            text = retryLabel,
            onClick = onRetry,
        )
    }
}
