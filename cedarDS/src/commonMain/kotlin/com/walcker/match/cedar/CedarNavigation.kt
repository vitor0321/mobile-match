package com.walcker.match.cedar

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalAppDrawerState: ProvidableCompositionLocal<DrawerState?> =
    staticCompositionLocalOf { null }
