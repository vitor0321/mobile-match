package com.walcker.games.strings

import cafe.adriel.lyricist.LyricistStrings
import com.walcker.match.core.strings.Locales

@LyricistStrings(languageTag = Locales.PT, default = true)
internal val PtBrGamesStrings = GamesStrings(
    gameList = gameListStringsPt,
    search = searchStringsPt,
)
