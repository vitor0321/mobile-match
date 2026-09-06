package com.walcker.games.features.ui.shared.matchDetail

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberShareLauncher(): (subject: String, text: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { subject: String, text: String ->
            val sendIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            val chooser =
                Intent.createChooser(sendIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(chooser)
        }
    }
}
