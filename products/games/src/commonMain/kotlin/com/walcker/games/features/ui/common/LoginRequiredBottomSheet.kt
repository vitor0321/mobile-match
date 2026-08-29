package com.walcker.games.features.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.strings.LoginRequiredStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginRequiredBottomSheet(
    isVisible: Boolean,
    strings: LoginRequiredStrings,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(CedarTokens.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            ) {
                Text(
                    text = strings.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = strings.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CedarPrimaryButton(
                    text = strings.confirm,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                )
                CedarTextButton(
                    text = strings.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
