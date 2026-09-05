package com.walcker.match.navigator

import cafe.adriel.voyager.core.screen.Screen

interface GamesDestination {
    fun gameList(): Screen

    fun search(): Screen

    fun create(): Screen

    fun editMatch(matchId: String): Screen

    fun myMatches(): Screen

    fun playerProfile(): Screen

    fun matchDetail(matchId: String): Screen
}
