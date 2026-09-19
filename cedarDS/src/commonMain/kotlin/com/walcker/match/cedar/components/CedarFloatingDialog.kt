package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.walcker.match.cedar.tokens.CedarTokens

private val MaxWidth = 400.dp
private val MaxHeight = 640.dp

@Composable
public fun CedarFloatingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                modifier
                    .padding(horizontal = CedarTokens.spacing.lg)
                    .widthIn(max = MaxWidth)
                    .heightIn(max = MaxHeight)
                    .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it },
            shape = CedarTokens.radius.lgShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = CedarTokens.elevation.overlay,
        ) {
            content()
        }
    }
}
