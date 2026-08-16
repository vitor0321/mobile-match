package com.walcker.games.features.ui.notifications

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun getCurrentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
