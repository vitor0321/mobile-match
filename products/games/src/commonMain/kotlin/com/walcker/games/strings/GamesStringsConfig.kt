package com.walcker.games.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.lyricist.Lyricist
import com.walcker.match.core.strings.MatchDefaultLanguageTag
import com.walcker.match.core.strings.Locales
import com.walcker.match.core.strings.createLyricist
import com.walcker.match.core.strings.normalizeMatchLanguageTag

internal val LocalGamesStrings = staticCompositionLocalOf { PtBrGamesStrings }

internal class GamesStringsHolder {
    private var currentStrings: GamesStrings? = null

    val strings: GamesStrings
        get() = currentStrings ?: error("Strings not initialized. Make sure to call setStrings() first.")

    fun setStrings(strings: GamesStrings) {
        currentStrings = strings
    }

    fun clearStrings() {
        currentStrings = null
    }

    fun hasStrings(): Boolean = currentStrings != null
}

internal fun GamesStringsHolder.resolveStringsOrDefault(): GamesStrings {
    return if (hasStrings()) strings else PtBrGamesStrings
}

private val gamesTranslations = mapOf(
    Locales.EN to EnGamesStrings,
    Locales.PT to PtBrGamesStrings,
)

@Composable
internal fun ProvideGamesStrings(
    lyricist: Lyricist<GamesStrings>,
    stringsHolder: GamesStringsHolder,
    content: @Composable () -> Unit,
) {
    key(lyricist.languageTag) {
        CompositionLocalProvider(LocalGamesStrings provides lyricist.strings) {
            DisposableEffect(stringsHolder, lyricist.languageTag) {
                stringsHolder.setStrings(lyricist.strings)

                onDispose {
                    stringsHolder.clearStrings()
                }
            }

            content()
        }
    }
}

@Composable
internal fun rememberGamesStrings(languageTag: String = MatchDefaultLanguageTag): Lyricist<GamesStrings> {
    val normalizedLanguageTag = normalizeMatchLanguageTag(languageTag)
    val lyricist = androidx.compose.runtime.remember {
        createLyricist(
            defaultLanguageTag = MatchDefaultLanguageTag,
            translations = gamesTranslations,
        )
    }

    LaunchedEffect(normalizedLanguageTag) {
        if (lyricist.languageTag != normalizedLanguageTag) {
            lyricist.languageTag = normalizedLanguageTag
        }
    }

    return lyricist
}
