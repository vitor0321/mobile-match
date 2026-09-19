package com.walcker.match.app.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchDeepLinkTest {
    @Test
    fun `a shared web link carries the match id in the id parameter`() {
        assertEquals("abc123", matchIdFromDeepLink("https://vitor0321.github.io/match/?id=abc123"))
    }

    @Test
    fun `a web link without the id parameter opens no match`() {
        assertNull(matchIdFromDeepLink("https://vitor0321.github.io/match/"))
        assertNull(matchIdFromDeepLink("https://vitor0321.github.io/match"))
        assertNull(matchIdFromDeepLink("https://vitor0321.github.io/"))
    }

    @Test
    fun `an app link carries the match id in the path`() {
        assertEquals("abc123", matchIdFromDeepLink("joinplay://match/abc123"))
    }

    @Test
    fun `an app link without a match id opens no match`() {
        assertNull(matchIdFromDeepLink("joinplay://match"))
        assertNull(matchIdFromDeepLink("joinplay://match/"))
    }

    @Test
    fun `the id parameter wins over the path`() {
        assertEquals("xyz789", matchIdFromDeepLink("joinplay://match/abc123?id=xyz789"))
    }

    @Test
    fun `the id parameter is found next to other parameters`() {
        assertEquals("abc123", matchIdFromDeepLink("https://vitor0321.github.io/match/?utm_source=whatsapp&id=abc123"))
    }

    @Test
    fun `an empty id parameter opens no match`() {
        assertNull(matchIdFromDeepLink("https://vitor0321.github.io/match/?id="))
    }

    @Test
    fun `a missing or malformed link opens no match`() {
        assertNull(matchIdFromDeepLink(null))
        assertNull(matchIdFromDeepLink(""))
        assertNull(matchIdFromDeepLink("   "))
        assertNull(matchIdFromDeepLink("vitor0321.github.io/match/?id=abc123"))
    }
}
