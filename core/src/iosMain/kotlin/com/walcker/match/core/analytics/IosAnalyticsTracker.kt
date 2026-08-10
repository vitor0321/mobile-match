package com.walcker.match.core.analytics

internal class IosAnalyticsTracker : AnalyticsTracker {
    override fun trackVerseRead(bookId: Int, chapter: Int) {}
    override fun trackStrongViewed(strongNumber: String, language: String) {}
    override fun trackChapterNavigated(direction: String) {}
}
