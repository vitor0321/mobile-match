package com.walcker.match.app.strings

import androidx.compose.runtime.Composable
import com.walcker.match.core.strings.Locales
import com.walcker.match.core.strings.rememberMatchLanguageTag

/**
 * Copy for the navigation shell: bottom bar captions and top bar actions.
 *
 * The shell had these hardcoded in English ("🏠 Home", "🔍 Search") inside a
 * pt-BR-first app, with the emoji standing in for the icon — so a screen reader
 * announced the emoji's name and nothing described the button.
 *
 * `:app` has no Lyricist aggregator, and adding one for a single file would cost
 * a dependency and a module-wide setup to translate nine strings. The data class
 * plus `...Pt` / `...En` pair is the same shape every other `*Strings.kt` in the
 * repo uses; only the resolution is simpler, and it goes through the very same
 * [rememberMatchLanguageTag] that backs the Lyricist modules.
 */
internal data class AppShellStrings(
    val appTitle: String,
    val homeTab: String,
    val searchTab: String,
    val createTab: String,
    val activityTab: String,
    val profileTab: String,
    val notificationsAction: String,
    val showMapAction: String,
    val showListAction: String,
)

internal val AppShellStringsPt = AppShellStrings(
    appTitle = "Match",
    homeTab = "Início",
    searchTab = "Buscar",
    createTab = "Criar",
    activityTab = "Atividade",
    profileTab = "Perfil",
    notificationsAction = "Notificações",
    showMapAction = "Ver no mapa",
    showListAction = "Ver em lista",
)

internal val AppShellStringsEn = AppShellStrings(
    appTitle = "Match",
    homeTab = "Home",
    searchTab = "Search",
    createTab = "Create",
    activityTab = "Activity",
    profileTab = "Profile",
    notificationsAction = "Notifications",
    showMapAction = "Show map",
    showListAction = "Show list",
)

@Composable
internal fun rememberAppShellStrings(): AppShellStrings =
    when (rememberMatchLanguageTag()) {
        Locales.EN -> AppShellStringsEn
        else -> AppShellStringsPt
    }
