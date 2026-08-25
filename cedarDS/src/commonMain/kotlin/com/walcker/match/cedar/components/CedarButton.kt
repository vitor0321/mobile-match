package com.walcker.match.cedar.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

/** Altura mínima, não fixa: o rótulo cresce com a escala de fonte do sistema. */
private val ButtonHeight = 56.dp
private val CompactButtonHeight = 44.dp
private val SpinnerSize = 20.dp
private val IconSize = 20.dp

/**
 * The main action of a screen: "Aplicar filtros", "Criar partida", "Entrar".
 *
 * Blue, because in this design system blue is what you press and green is what
 * tells you a match still has room. Full width by default — the redesign puts
 * primary actions edge to edge, one per screen.
 *
 * @param loading swaps the label for a spinner and blocks the click. The button
 *   keeps its size, so the layout does not jump — which is the whole point of
 *   having this here instead of at each call site.
 */
@Composable
public fun CedarPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fillWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CedarTokens.radius.mdShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = ButtonHeight),
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

/**
 * The action that is about a slot rather than about navigation:
 * "Garantir minha vaga · R$ 25".
 *
 * Green on purpose, and the only green button in the system. Text is ink, not
 * white — white on this green is 2:1 and unreadable; ink is 9:1.
 */
@Composable
public fun CedarAvailabilityButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CedarTokens.radius.mdShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CedarTokens.colors.available,
            contentColor = CedarTokens.colors.onAvailable,
        ),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ButtonHeight),
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

/**
 * The alternative that is not the main action: "Ver detalhes da partida",
 * "Cancelar", "Sair da partida".
 */
@Composable
public fun CedarSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fillWidth: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CedarTokens.radius.mdShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = ButtonHeight),
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

/**
 * A low-weight action that still needs a 48dp target: "Limpar filtros",
 * "Ver todas", "Recentrar".
 *
 * [defaultMinSize] rather than a fixed height so the target survives a user
 * running the system font at 200%.
 */
@Composable
public fun CedarTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CedarTokens.radius.smShape,
        modifier = modifier.defaultMinSize(minHeight = CompactButtonHeight),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    leadingIcon: ImageVector?,
) {
    // Crossfade keeps the button's measured size while the spinner swaps in, so a
    // slow join does not make the screen jump under the user's thumb.
    Crossfade(targetState = loading) { isLoading ->
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                    modifier = Modifier.size(SpinnerSize),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize),
                        )
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        // Duas linhas, não uma: "Enviar e-mail de recuperação" a
                        // 200% de escala de fonte virava reticências numa linha só.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = if (leadingIcon != null) {
                            Modifier.padding(start = CedarTokens.spacing.xs)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}
