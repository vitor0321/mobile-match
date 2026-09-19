package com.walcker.match.core.analytics

public interface AnalyticsTracker {
    public fun track(event: AnalyticsEvent)
}

public sealed class AnalyticsEvent(
    public val name: String,
    public val params: Map<String, String> = emptyMap(),
) {
    public class MatchListViewed(
        source: MatchListSource,
    ) : AnalyticsEvent(
            name = "match_list_viewed",
            params = mapOf("source" to source.value),
        )

    public class MatchViewed(
        sport: String,
        hasOpenSlots: Boolean,
    ) : AnalyticsEvent(
            name = "match_viewed",
            params = mapOf("sport" to sport, "has_open_slots" to hasOpenSlots.toString()),
        )

    public class MatchJoinAttempted(
        sport: String,
    ) : AnalyticsEvent(
            name = "match_join_attempted",
            params = mapOf("sport" to sport),
        )

    public class MatchJoinResult(
        sport: String,
        outcome: JoinOutcome,
    ) : AnalyticsEvent(
            name = "match_join_result",
            params = mapOf("sport" to sport, "outcome" to outcome.value),
        )

    public class MatchLeft(
        sport: String,
    ) : AnalyticsEvent(
            name = "match_left",
            params = mapOf("sport" to sport),
        )

    public class MatchCreated(
        sport: String,
    ) : AnalyticsEvent(
            name = "match_created",
            params = mapOf("sport" to sport),
        )

    public class PlayerRated(
        stars: Int,
    ) : AnalyticsEvent(
            name = "player_rated",
            params = mapOf("stars" to stars.toString()),
        )

    public class MatchCreateAttempted(
        sport: String,
    ) : AnalyticsEvent(
            name = "match_create_attempted",
            params = mapOf("sport" to sport),
        )

    public class MatchCancelled(
        sport: String,
        isSeries: Boolean,
    ) : AnalyticsEvent(
            name = "match_cancelled",
            params = mapOf("sport" to sport, "is_series" to isSeries.toString()),
        )

    public class MatchRated(
        sport: String,
        stars: Int,
    ) : AnalyticsEvent(
            name = "match_rated",
            params = mapOf("sport" to sport, "stars" to stars.toString()),
        )

    public class PlayerReported(
        reason: String,
    ) : AnalyticsEvent(
            name = "player_reported",
            params = mapOf("reason" to reason),
        )

    public class PlayerProfileViewed(
        source: PlayerProfileSource,
    ) : AnalyticsEvent(
            name = "player_profile_viewed",
            params = mapOf("source" to source.value),
        )

    public class AvailabilityToggled(
        isAvailable: Boolean,
    ) : AnalyticsEvent(
            name = "availability_toggled",
            params = mapOf("is_available" to isAvailable.toString()),
        )

    public class LoginAttempted(
        method: String,
    ) : AnalyticsEvent(
            name = "login_attempted",
            params = mapOf("method" to method),
        )

    public class LoginResult(
        method: String,
        success: Boolean,
    ) : AnalyticsEvent(
            name = "login_result",
            params = mapOf("method" to method, "success" to success.toString()),
        )

    public class SignUpAttempted : AnalyticsEvent(name = "signup_attempted")

    public class SignUpResult(
        success: Boolean,
    ) : AnalyticsEvent(
            name = "signup_result",
            params = mapOf("success" to success.toString()),
        )

    public class PasswordResetRequested : AnalyticsEvent(name = "password_reset_requested")

    public class PurchaseAttempted(
        offeringId: String,
    ) : AnalyticsEvent(
            name = "purchase_attempted",
            params = mapOf("offering_id" to offeringId),
        )

    public class PurchaseResult(
        offeringId: String,
        success: Boolean,
    ) : AnalyticsEvent(
            name = "purchase_result",
            params = mapOf("offering_id" to offeringId, "success" to success.toString()),
        )

    public class PurchaseRestored(
        success: Boolean,
    ) : AnalyticsEvent(
            name = "purchase_restored",
            params = mapOf("success" to success.toString()),
        )

    public class AccountUpgradeClicked : AnalyticsEvent(name = "account_upgrade_clicked")

    public class SignedOut : AnalyticsEvent(name = "signed_out")

    public class AccountDeleted : AnalyticsEvent(name = "account_deleted")
}

public enum class MatchListSource(
    public val value: String,
) {
    HOME("home"),
    SEARCH("search"),
    MAP("map"),
}

public enum class JoinOutcome(
    public val value: String,
) {
    CONFIRMED("confirmed"),
    WAITLIST("waitlist"),
    FAILED("failed"),
}

public enum class PlayerProfileSource(
    public val value: String,
) {
    SEARCH("search"),
    MATCH_DETAIL("match_detail"),
    NEXT_MATCH("next_match"),
    SELF("self"),
}
