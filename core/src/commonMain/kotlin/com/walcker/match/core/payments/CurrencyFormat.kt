package com.walcker.match.core.payments

// Código de moeda (ISO 4217, ex. "BRL", "USD") do dispositivo de quem está criando a
// partida — a localização real do aparelho, não o idioma escolhido no app
// (com.walcker.match.core.strings.Locales). É isso que fica gravado na partida e
// decide o símbolo mostrado pra todo mundo depois, inclusive em outro país.
public expect fun currentDeviceCurrencyCode(): String

// Formata centavos usando a moeda informada (não a do dispositivo de quem está
// vendo) — a convenção de casas decimais e símbolo vem da própria moeda via API
// nativa da plataforma, não de um mapa manual código→símbolo.
public expect fun formatCurrencyCents(
    cents: Int,
    currencyCode: String,
): String

public fun formatBRLCents(cents: Int): String {
    val reais = cents / 100
    val centavos = cents % 100
    val reaisStr =
        reais.toLong().let { value ->
            val builder = StringBuilder()
            var remaining = value
            while (remaining >= 1000) {
                builder.insert(0, ".${(remaining % 1000).toString().padStart(3, '0')}")
                remaining /= 1000
            }
            builder.insert(0, remaining.toString())
            builder.toString()
        }
    return "R$ $reaisStr,${centavos.toString().padStart(2, '0')}"
}
