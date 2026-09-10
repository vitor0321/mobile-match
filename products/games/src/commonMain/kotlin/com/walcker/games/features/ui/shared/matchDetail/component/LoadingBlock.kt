package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun LoadingBlock(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(CedarTokens.spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        CedarLoading(contentDescription = contentDescription)
    }
}
