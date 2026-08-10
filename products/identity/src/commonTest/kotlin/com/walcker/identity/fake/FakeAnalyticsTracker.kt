package com.walcker.identity.fake

import com.walcker.match.core.analytics.AnalyticsTracker

internal class FakeAnalyticsTracker : AnalyticsTracker {
    override fun trackVerseRead(bookId: Int, chapter: Int) = Unit

    override fun trackStrongViewed(strongNumber: String, language: String) = Unit

    override fun trackChapterNavigated(direction: String) = Unit
}
