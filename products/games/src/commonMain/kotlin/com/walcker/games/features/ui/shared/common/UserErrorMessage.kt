package com.walcker.games.features.ui.shared.common

import com.walcker.games.features.domain.shared.error.GamesError
import com.walcker.games.strings.ErrorStrings

internal fun Throwable.userMessage(
    fallback: String,
    errors: ErrorStrings,
): String = if (this is GamesError.Network) errors.noConnection else fallback
