package com.walcker.identity.strings

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

internal val LocalIdentityStrings = staticCompositionLocalOf { PtBrIdentityStrings }

internal class IdentityStringsHolder {
    private var currentStrings: IdentityStrings? = null

    val strings: IdentityStrings
        get() = currentStrings ?: error("Strings not initialized. Make sure to call setStrings() first.")

    fun setStrings(strings: IdentityStrings) {
        currentStrings = strings
    }

    fun clearStrings() {
        currentStrings = null
    }

    fun hasStrings(): Boolean = currentStrings != null
}

internal fun IdentityStringsHolder.resolveStringsOrDefault(): IdentityStrings {
    return if (hasStrings()) strings else PtBrIdentityStrings
}

private val identityTranslations = mapOf(
    Locales.EN to EnIdentityStrings,
    Locales.PT to PtBrIdentityStrings,
)

@Composable
internal fun ProvideIdentityStrings(
    lyricist: Lyricist<IdentityStrings>,
    stringsHolder: IdentityStringsHolder,
    content: @Composable () -> Unit,
) {
    key(lyricist.languageTag) {
        CompositionLocalProvider(LocalIdentityStrings provides lyricist.strings) {
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
internal fun rememberIdentityStrings(languageTag: String): Lyricist<IdentityStrings> {
    val normalizedLanguageTag = normalizeMatchLanguageTag(languageTag)
    val lyricist = androidx.compose.runtime.remember {
        createLyricist(
            defaultLanguageTag = MatchDefaultLanguageTag,
            translations = identityTranslations,
        )
    }

    LaunchedEffect(normalizedLanguageTag) {
        if (lyricist.languageTag != normalizedLanguageTag) {
            lyricist.languageTag = normalizedLanguageTag
        }
    }

    return lyricist
}

