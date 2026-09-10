@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.walcker.identity.strings.IdentityStringsHolder
import com.walcker.identity.strings.ProvideIdentityStrings
import com.walcker.identity.strings.rememberIdentityStrings
import com.walcker.match.cedar.CedarTheme
import com.walcker.match.core.strings.Locales

@Composable
internal fun IdentitySnapshotTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CedarTheme(darkTheme = darkTheme) {
        val stringsHolder = remember { IdentityStringsHolder() }
        val lyricist = rememberIdentityStrings(languageTag = Locales.PT)

        ProvideIdentityStrings(
            lyricist = lyricist,
            stringsHolder = stringsHolder,
        ) {
            content()
        }
    }
}
