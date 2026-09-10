package com.walcker.games.features.domain.shared.util

import kotlin.random.Random

private const val MIN_TEAMS = 2
private const val MAX_TEAMS = 6

internal fun shuffleIntoTeams(
    playerIds: List<String>,
    teamCount: Int,
    random: Random = Random,
): Map<String, Int> {
    require(teamCount in MIN_TEAMS..MAX_TEAMS) {
        "teamCount must be between $MIN_TEAMS and $MAX_TEAMS, was $teamCount"
    }
    return playerIds
        .shuffled(random)
        .mapIndexed { index, userId -> userId to index % teamCount }
        .toMap()
}
