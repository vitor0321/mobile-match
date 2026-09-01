package com.walcker.games.features.ui.shared.playerDetails.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.components.CedarLoading

@Composable
internal fun LoadingContent(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CedarLoading(contentDescription = contentDescription)
    }
}
