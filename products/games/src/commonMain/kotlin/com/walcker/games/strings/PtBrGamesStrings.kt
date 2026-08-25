package com.walcker.games.strings

import cafe.adriel.lyricist.LyricistStrings
import com.walcker.match.core.strings.Locales

@LyricistStrings(languageTag = Locales.PT, default = true)
internal val PtBrGamesStrings = GamesStrings(
    gameList = gameListStringsPt,
    search = searchStringsPt,
    myMatches = myMatchesStringsPt,
    createMatch = createMatchStringsPt,
    playerProfile = playerProfileStringsPt,
    notificationHistory = PtBrNotificationHistoryStrings(),
    playerSearch = playerSearchStringsPt,
    playerDetails = playerDetailsStringsPt,
    playerRatings = playerRatingsStringsPt,
    ratings = ratingStringsPt,
    reports = reportStringsPt,
    matchConfirmed = matchConfirmedStringsPt,
    matchDetail = matchDetailStringsPt,
    map = mapStringsPt,
)
