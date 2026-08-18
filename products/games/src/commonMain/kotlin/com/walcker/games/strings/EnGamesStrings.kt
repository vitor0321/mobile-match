package com.walcker.games.strings

import cafe.adriel.lyricist.LyricistStrings
import com.walcker.match.core.strings.Locales

@LyricistStrings(languageTag = Locales.EN)
internal val EnGamesStrings = GamesStrings(
    gameList = gameListStringsEn,
    search = searchStringsEn,
    myMatches = myMatchesStringsEn,
    createMatch = createMatchStringsEn,
    playerProfile = playerProfileStringsEn,
    notificationHistory = EnNotificationHistoryStrings(),
    playerSearch = playerSearchStringsEn,
)
