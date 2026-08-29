package com.walcker.match.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

internal class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        analytics.logEvent(event.name) {
            event.params.forEach { (key, value) -> param(key, value) }
        }
    }
}
