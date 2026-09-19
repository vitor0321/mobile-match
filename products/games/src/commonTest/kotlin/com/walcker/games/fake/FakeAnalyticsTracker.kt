package com.walcker.games.fake

import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker

internal class FakeAnalyticsTracker : AnalyticsTracker {
    val trackedEvents: MutableList<AnalyticsEvent> = mutableListOf()

    override fun track(event: AnalyticsEvent) {
        trackedEvents += event
    }
}
