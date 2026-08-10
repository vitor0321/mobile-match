package com.walcker.match.core.analytics

public interface AnalyticsTracker {
    fun trackVerseRead(bookId: Int, chapter: Int)
    fun trackStrongViewed(strongNumber: String, language: String)
    fun trackChapterNavigated(direction: String)
}
