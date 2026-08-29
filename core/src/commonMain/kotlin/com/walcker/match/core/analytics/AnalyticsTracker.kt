package com.walcker.match.core.analytics

public interface AnalyticsTracker {
    public fun track(event: AnalyticsEvent)
}

public sealed class AnalyticsEvent(
    public val name: String,
    public val params: Map<String, String> = emptyMap(),
) {
    public class MatchListViewed(source: MatchListSource) : AnalyticsEvent(
        name = "match_list_viewed",
        params = mapOf("source" to source.value),
    )

    public class MatchViewed(sport: String, hasOpenSlots: Boolean) : AnalyticsEvent(
        name = "match_viewed",
        params = mapOf("sport" to sport, "has_open_slots" to hasOpenSlots.toString()),
    )

    public class MatchJoinAttempted(sport: String) : AnalyticsEvent(
        name = "match_join_attempted",
        params = mapOf("sport" to sport),
    )

    public class MatchJoinResult(sport: String, outcome: JoinOutcome) : AnalyticsEvent(
        name = "match_join_result",
        params = mapOf("sport" to sport, "outcome" to outcome.value),
    )

    public class MatchLeft(sport: String) : AnalyticsEvent(
        name = "match_left",
        params = mapOf("sport" to sport),
    )

    public class MatchCreated(sport: String) : AnalyticsEvent(
        name = "match_created",
        params = mapOf("sport" to sport),
    )

    public class PlayerRated(stars: Int) : AnalyticsEvent(
        name = "player_rated",
        params = mapOf("stars" to stars.toString()),
    )
}

public enum class MatchListSource(public val value: String) {
    HOME("home"),
    SEARCH("search"),
    MAP("map"),
}

public enum class JoinOutcome(public val value: String) {
    CONFIRMED("confirmed"),
    WAITLIST("waitlist"),
    FAILED("failed"),
}
