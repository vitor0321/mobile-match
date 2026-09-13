package com.walcker.games.features.domain.shared.util

import kotlin.random.Random

internal const val MIN_TEAMS = 2
internal const val MAX_TEAMS = 6
internal const val MIN_PLAYERS_PER_TEAM = 2
internal const val MAX_PLAYERS_PER_TEAM = 15
internal const val BEGINNER_SKILL_RATING = 1f

internal fun shuffleIntoTeams(
    playerIds: List<String>,
    teamCount: Int,
    playersPerTeam: Int,
    skillRatings: Map<String, Float> = emptyMap(),
    random: Random = Random,
): Map<String, Int> {
    require(teamCount in MIN_TEAMS..MAX_TEAMS) {
        "teamCount must be between $MIN_TEAMS and $MAX_TEAMS, was $teamCount"
    }
    require(playersPerTeam in MIN_PLAYERS_PER_TEAM..MAX_PLAYERS_PER_TEAM) {
        "playersPerTeam must be between $MIN_PLAYERS_PER_TEAM and $MAX_PLAYERS_PER_TEAM, was $playersPerTeam"
    }
    val capacity = teamCount * playersPerTeam
    val candidates =
        playerIds
            .shuffled(random)
            .sortedByDescending { userId -> skillRatings[userId] ?: BEGINNER_SKILL_RATING }
            .take(capacity)

    val assignments = mutableMapOf<String, Int>()
    var round = 0
    var index = 0
    while (index < candidates.size) {
        val teamOrder = if (round % 2 == 0) 0 until teamCount else (teamCount - 1) downTo 0
        for (teamIndex in teamOrder) {
            if (index >= candidates.size) break
            assignments[candidates[index]] = teamIndex
            index++
        }
        round++
    }
    return assignments
}
