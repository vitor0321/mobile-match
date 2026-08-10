package com.walcker.identity.features.ui.paywall.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
internal fun PaywallLegalFooter(
    termsLabel: String,
    privacyLabel: String,
    colors: ColorScheme,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val color = colors.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = termsLabel,
            color = color,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable(onClick = onOpenTerms)
                .padding(8.dp),
        )
        Text(
            text = "·",
            color = color,
        )
        Text(
            text = privacyLabel,
            color = color,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable(onClick = onOpenPrivacy)
                .padding(8.dp),
        )
    }
}
