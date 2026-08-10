package com.walcker.match.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

internal class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {

    override fun trackVerseRead(bookId: Int, chapter: Int) {
        analytics.logEvent("verse_read") {
            param("book_id", bookId.toLong())
            param("chapter", chapter.toLong())
        }
    }

    override fun trackStrongViewed(strongNumber: String, language: String) {
        analytics.logEvent("strong_viewed") {
            param("strong_number", strongNumber)
            param("language", language)
        }
    }

    override fun trackChapterNavigated(direction: String) {
        analytics.logEvent("chapter_navigated") {
            param("direction", direction)
        }
    }
}
