package com.walcker.match.app

import com.walcker.match.app.strings.AppShellStringsPt
import com.walcker.match.cedar.components.MatchBottomBarTab
import com.walcker.match.navigator.MainTab
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchScaffoldTest {
    @Test
    fun `each bottom bar tab maps to its equivalent main tab`() {
        assertEquals(MainTab.Home, MatchBottomBarTab.Home.toMainTab())
        assertEquals(MainTab.Search, MatchBottomBarTab.Search.toMainTab())
        assertEquals(MainTab.Create, MatchBottomBarTab.Create.toMainTab())
        assertEquals(MainTab.MyMatches, MatchBottomBarTab.Activity.toMainTab())
        assertEquals(MainTab.PlayerProfile, MatchBottomBarTab.Profile.toMainTab())
    }

    @Test
    fun `every MainTab round-trips through its index back to the same tab`() {
        for (mainTab in MainTab.entries) {
            val bottomBarTab = MatchBottomBarTab.entries[mainTab.index]
            assertEquals(mainTab, bottomBarTab.toMainTab(), "MainTab.$mainTab and MatchBottomBarTab.$bottomBarTab drifted out of sync")
        }
    }

    @Test
    fun `both tab enums have the same number of entries`() {
        assertEquals(MainTab.entries.size, MatchBottomBarTab.entries.size)
    }

    @Test
    fun `labelFor resolves the matching string for every tab`() {
        val strings = AppShellStringsPt
        assertEquals(strings.homeTab, strings.labelFor(MatchBottomBarTab.Home))
        assertEquals(strings.searchTab, strings.labelFor(MatchBottomBarTab.Search))
        assertEquals(strings.createTab, strings.labelFor(MatchBottomBarTab.Create))
        assertEquals(strings.activityTab, strings.labelFor(MatchBottomBarTab.Activity))
        assertEquals(strings.profileTab, strings.labelFor(MatchBottomBarTab.Profile))
    }
}
