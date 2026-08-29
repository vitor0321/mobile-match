package com.walcker.games.features.domain.model

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

    OTHER("other"),
    ;

    internal companion object {
        internal const val MAX_DETAILS_LENGTH: Int = 1_000

        internal fun fromId(id: String): ReportReason? = entries.firstOrNull { it.id == id }
    }
}
