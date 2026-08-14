package com.walcker.match.core.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal actual fun getCurrentTime(): Instant = Clock.System.now()
