package com.walcker.match.core.strings

import cafe.adriel.lyricist.Lyricist

public fun <T> createLyricist(
    defaultLanguageTag: String,
    translations: Map<String, T>,
): Lyricist<T> =
    Lyricist(
        defaultLanguageTag = defaultLanguageTag,
        translations = translations,
    ).apply {
        languageTag = defaultLanguageTag
    }
