package com.walcker.match.core.datetime

import kotlinx.datetime.Instant
import kotlin.time.Clock

internal actual fun getCurrentTime(): Instant = Clock.System.now()
