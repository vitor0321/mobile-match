package com.walcker.identity.strings

import androidx.compose.runtime.Composable
import com.walcker.match.core.strings.LocalMatchLanguageTag
import org.koin.compose.koinInject

@Composable
internal fun WithIdentityStrings(content: @Composable () -> Unit) {
    val stringsHolder = koinInject<IdentityStringsHolder>()
    val lyricist = rememberIdentityStrings(languageTag = LocalMatchLanguageTag.current)

    ProvideIdentityStrings(
        lyricist = lyricist,
        stringsHolder = stringsHolder,
    ) {
        content()
    }
}
