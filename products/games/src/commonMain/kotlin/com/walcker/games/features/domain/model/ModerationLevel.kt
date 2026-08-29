package com.walcker.games.features.domain.model

internal enum class ModerationLevel(val id: String) {
    NONE("none"),

    WARNING("warning"),

    SUSPENDED("suspended"),

    BANNED("banned"),
    ;

    internal companion object {
        internal fun fromId(id: String?): ModerationLevel =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}
