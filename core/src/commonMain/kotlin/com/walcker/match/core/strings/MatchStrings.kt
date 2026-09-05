package com.walcker.match.core.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.intl.Locale

public const val MATCH_DEFAULT_LANGUAGE_TAG = Locales.PT

private val supportedLanguageTags =
    setOf(
        Locales.EN,
        Locales.PT,
    )

public val LocalMatchLanguageTag = compositionLocalOf { MATCH_DEFAULT_LANGUAGE_TAG }

public fun normalizeMatchLanguageTag(languageTag: String): String {
    val baseLanguageTag =
        languageTag
            .substringBefore('-')
            .substringBefore('_')
            .lowercase()

    return if (baseLanguageTag in supportedLanguageTags) {
        baseLanguageTag
    } else {
        MATCH_DEFAULT_LANGUAGE_TAG
    }
}

@Composable
public fun rememberMatchLanguageTag(
    currentLanguageTag: String = Locale.current.toLanguageTag(),
): String = normalizeMatchLanguageTag(currentLanguageTag)

@Composable
public fun ProvideMatchLanguage(
    languageTag: String = rememberMatchLanguageTag(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMatchLanguageTag provides normalizeMatchLanguageTag(languageTag),
        content = content,
    )
}
