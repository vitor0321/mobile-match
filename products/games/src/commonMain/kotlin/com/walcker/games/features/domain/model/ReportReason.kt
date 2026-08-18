package com.walcker.games.features.domain.model

/**
 * Why a player is being reported.
 *
 * [id] is the wire contract with `functions/src/moderation.ts` — a report
 * already stored carries it, so ids may be added but never renamed.
 */
internal enum class ReportReason(val id: String) {
    NO_SHOW("no_show"),
    LATE("late"),
    NO_PAYMENT("no_payment"),
    AGGRESSIVE_BEHAVIOR("aggressive_behavior"),
    VERBAL_ABUSE("verbal_abuse"),
    DISCRIMINATION("discrimination"),
    HARASSMENT("harassment"),
    DANGEROUS_PLAY("dangerous_play"),
    FAKE_PROFILE("fake_profile"),

    /**
     * Without a generic option people pick the closest wrong reason just to be
     * able to report, and the reason statistics become noise.
     */
    OTHER("other"),
    ;

    internal companion object {
        internal const val MAX_DETAILS_LENGTH: Int = 1_000

        internal fun fromId(id: String): ReportReason? = entries.firstOrNull { it.id == id }
    }
}
