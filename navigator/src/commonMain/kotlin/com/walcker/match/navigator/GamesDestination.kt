package com.walcker.match.navigator

import cafe.adriel.voyager.core.screen.Screen

/**
 * Contrato de navegação exposto pelo produto de jogos.
 * A implementação vive em products/games e é registrada no Koin.
 */
interface GamesDestination {
    fun gameList(): Screen
    fun search(): Screen
}
