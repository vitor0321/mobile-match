package com.walcker.identity.fake

import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker

/**
 * Guarda o que foi registrado para o teste poder afirmar sobre o funil, em vez
 * de só engolir a chamada.
 */
internal class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }

    fun names(): List<String> = events.map { it.name }
}
