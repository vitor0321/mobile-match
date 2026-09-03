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
import androidx.compose.material3.Icon
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

private val ButtonHeight = 56.dp
private val CompactButtonHeight = 44.dp
private val SpinnerSize = 20.dp
private val IconSize = 20.dp

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
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier =
            modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(minHeight = ButtonHeight),
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

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
        colors =
            ButtonDefaults.buttonColors(
                containerColor = CedarTokens.colors.available,
                contentColor = CedarTokens.colors.onAvailable,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ButtonHeight),
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

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
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier =
            modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(minHeight = ButtonHeight),
    ) {
        ButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

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
    Crossfade(targetState = loading) { isLoading ->
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CedarLoading(
                    contentDescription = text,
                    size = SpinnerSize,
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier =
                            if (leadingIcon != null) {
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
