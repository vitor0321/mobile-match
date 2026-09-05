package com.walcker.identity.features.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.dismissKeyboardOnTap
import com.walcker.match.cedar.tokens.CedarTokens

private val MinTouchTarget = 48.dp

@Composable
internal fun AuthScaffold(
    title: String,
    subtitle: String,
    backContentDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CedarTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                backContentDescription = backContentDescription,
            )
        },
        containerColor = CedarTokens.colors.canvas,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding)
                    .padding(padding)
                    .imePadding()
                    .dismissKeyboardOnTap()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = CedarTokens.spacing.lg,
                        vertical = CedarTokens.spacing.md,
                    ),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            content = content,
        )
    }
}

@Composable
internal fun AuthFormMessage(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
internal fun SocialDivider(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
internal fun AuthLegalLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.Underline,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MinTouchTarget)
                .clickable(role = Role.Button, onClick = onClick)
                .wrapContentHeight(align = Alignment.CenterVertically),
    )
}
