package com.walcker.identity.features.ui.paywall.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

/** Alvo mínimo de toque. Os links tinham ~36dp de altura. */
private val MinTouchTarget = 48.dp

/**
 * Termos de uso e política de privacidade, no rodapé do paywall.
 *
 * Dois links que a loja exige e que ninguém revisa. O que estava errado:
 * - **Alvo de toque de ~36dp.** Era o tamanho do texto mais 8dp de padding.
 * - **`clickable` sem papel**, então um leitor de tela lia dois parágrafos, não
 *   dois links.
 * - Cor `onSurfaceVariant`: sublinhado, mas cinza. Link é `primary`.
 */
@Composable
internal fun PaywallLegalFooter(
    termsLabel: String,
    privacyLabel: String,
    colors: ColorScheme,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = CedarTokens.spacing.xs,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegalLink(label = termsLabel, color = colors.primary, onClick = onOpenTerms)
        Text(text = "·", color = colors.onSurfaceVariant)
        LegalLink(label = privacyLabel, color = colors.primary, onClick = onOpenPrivacy)
    }
}

@Composable
private fun LegalLink(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textDecoration = TextDecoration.Underline,
        textAlign = TextAlign.Center,
        modifier = modifier
            .defaultMinSize(minHeight = MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = CedarTokens.spacing.xs)
            .wrapContentHeight(align = Alignment.CenterVertically),
    )
}
