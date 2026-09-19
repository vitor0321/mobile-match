package com.walcker.games.strings

import androidx.compose.runtime.Composable
import com.walcker.games.features.domain.shared.model.Sport

internal data class SportStrings(
    val name: (Sport) -> String,
)

internal val sportStringsPt =
    SportStrings(
        name = { sport ->
            when (sport) {
                Sport.FUTSAL -> "Futsal"
                Sport.FUTEBOL -> "Futebol"
                Sport.SOCIETY -> "Society"
                Sport.VOLEI -> "Vôlei"
                Sport.BASQUETE -> "Basquete"
                Sport.BEACH_TENNIS -> "Beach Tennis"
                Sport.TENIS -> "Tênis"
                Sport.PADEL -> "Padel"
                Sport.FUTEVOLEI -> "Futevôlei"
                Sport.PICKLEBALL -> "Pickleball"
                Sport.NATACAO -> "Natação"
            }
        },
    )

internal val sportStringsEn =
    SportStrings(
        name = { sport ->
            when (sport) {
                Sport.FUTSAL -> "Futsal"
                Sport.FUTEBOL -> "Soccer"
                Sport.SOCIETY -> "Society (7-a-side)"
                Sport.VOLEI -> "Volleyball"
                Sport.BASQUETE -> "Basketball"
                Sport.BEACH_TENNIS -> "Beach Tennis"
                Sport.TENIS -> "Tennis"
                Sport.PADEL -> "Padel"
                Sport.FUTEVOLEI -> "Footvolley"
                Sport.PICKLEBALL -> "Pickleball"
                Sport.NATACAO -> "Swimming"
            }
        },
    )

@Composable
internal fun sportName(sport: Sport): String = LocalGamesStrings.current.sports.name(sport)
