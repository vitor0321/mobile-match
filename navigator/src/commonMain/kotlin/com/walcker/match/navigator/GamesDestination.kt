package com.walcker.match.navigator

import cafe.adriel.voyager.core.screen.Screen

/**
 * Contrato de navegação exposto pelo produto de jogos.
 * A implementação vive em products/games e é registrada no Koin.
 */
interface GamesDestination {
    fun gameList(): Screen
    fun map(): Screen
    fun search(): Screen
    fun create(): Screen
    fun myMatches(): Screen
    fun playerProfile(): Screen
    fun matchDetail(matchId: String): Screen
}
