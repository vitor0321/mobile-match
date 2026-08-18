package com.walcker.games.features.domain.model

/**
 * How restricted an account is. Mirrors `ModerationLevel` in
 * `functions/src/moderation.ts`.
 *
 * Only the server decides this — the client reads it to explain a block, never
 * to enforce one.
 */
internal enum class ModerationLevel(val id: String) {
    NONE("none"),

    /** A heads-up. Nothing is blocked. */
    WARNING("warning"),

    /** Temporarily blocked, until a deadline. */
    SUSPENDED("suspended"),

    /** Permanently blocked. Only an admin sets this. */
    BANNED("banned"),
    ;

    internal companion object {
        internal fun fromId(id: String?): ModerationLevel =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}
