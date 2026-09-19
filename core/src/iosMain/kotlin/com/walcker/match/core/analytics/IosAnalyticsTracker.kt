package com.walcker.match.core.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal class IosAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        FIRAnalytics.logEventWithName(event.name, event.params as Map<Any?, *>)
    }
}
