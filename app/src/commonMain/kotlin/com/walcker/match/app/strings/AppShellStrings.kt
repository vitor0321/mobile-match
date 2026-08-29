package com.walcker.match.app.strings

import androidx.compose.runtime.Composable
import com.walcker.match.core.strings.Locales
import com.walcker.match.core.strings.rememberMatchLanguageTag

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
    appTitle = "Join Play",
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
    appTitle = "Join Play",
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
