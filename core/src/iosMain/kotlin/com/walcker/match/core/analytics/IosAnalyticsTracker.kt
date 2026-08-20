package com.walcker.match.core.analytics

/**
 * No-op: o alvo iOS ainda não tem o SDK do Firebase Analytics integrado.
 *
 * Consequência que precisa estar clara para quem for ler o painel: **o funil
 * hoje é só de Android**. Comparar iOS com Android em qualquer métrica deste
 * arquivo dá zero para iOS, e isso não quer dizer que ninguém usou.
 */
internal class IosAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
