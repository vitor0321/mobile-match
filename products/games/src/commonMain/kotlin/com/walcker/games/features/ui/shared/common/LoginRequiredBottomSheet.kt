package com.walcker.games.features.ui.shared.common

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.walcker.games.strings.LoginRequiredStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.coroutines.launch

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
        val scope = rememberCoroutineScope()

        fun hideThenRun(action: () -> Unit) {
            scope
                .launch { sheetState.hide() }
                .invokeOnCompletion { action() }
        }

        ModalBottomSheet(
            onDismissRequest = { hideThenRun(onDismiss) },
            sheetState = sheetState,
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    modifier
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
                    onClick = { hideThenRun(onConfirm) },
                    modifier = Modifier.fillMaxWidth(),
                )
                CedarTextButton(
                    text = strings.cancel,
                    onClick = { hideThenRun(onDismiss) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
