package com.walcker.match.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

internal class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {

    /**
     * Repassa nome e parâmetros como vieram. Toda a decisão de o que medir está
     * em [AnalyticsEvent]; aqui não existe regra, e é de propósito — quando a
     * tradução para o SDK tem opinião própria, o evento no painel deixa de ser
     * o evento que o código diz que é.
     */
    override fun track(event: AnalyticsEvent) {
        analytics.logEvent(event.name) {
            event.params.forEach { (key, value) -> param(key, value) }
        }
    }
}
