package com.walcker.games.features.ui.shared.matchDetail

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberShareLauncher(): (subject: String, text: String) -> Unit
