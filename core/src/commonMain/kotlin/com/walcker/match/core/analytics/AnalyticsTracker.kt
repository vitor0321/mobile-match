package com.walcker.match.core.analytics

/**
 * Registro de eventos de produto.
 *
 * Um método só, recebendo [AnalyticsEvent], em vez de um método por evento:
 * evento novo passa a ser uma classe nova, e nenhuma implementação de
 * plataforma precisa ser tocada. A versão anterior desta interface tinha três
 * métodos herdados do Lexis (`trackVerseRead`, `trackStrongViewed`,
 * `trackChapterNavigated`) e nenhuma chamada no app inteiro.
 */
public interface AnalyticsTracker {
    public fun track(event: AnalyticsEvent)
}

/**
 * O funil do produto: **ver → entrar → confirmar → jogar**.
 *
 * Duas escolhas que valem explicação:
 *
 * 1. **Nenhum evento carrega `matchId` ou `userId`.** Parâmetro de analytics
 *    serve para segmentar, e id é alta cardinalidade — não agrega, estoura o
 *    limite do Firebase e transforma o painel num log. Quem precisa cruzar por
 *    partida usa o Firestore, que é onde o dado mora.
 *
 * 2. **Os valores são todos `String`.** As perguntas do funil são contagens de
 *    evento segmentadas por dimensão, e a contagem quem faz é o Firebase. Nota
 *    de avaliação como texto ("4") é exatamente o que se quer para segmentar;
 *    número contínuo aqui não teria uso.
 *
 * Nomes seguem a convenção do Firebase: snake_case, até 40 caracteres.
 */
public sealed class AnalyticsEvent(
    public val name: String,
    public val params: Map<String, String> = emptyMap(),
) {
    /** Topo do funil: a pessoa viu uma lista de partidas. */
    public class MatchListViewed(source: MatchListSource) : AnalyticsEvent(
        name = "match_list_viewed",
        params = mapOf("source" to source.value),
    )

    /**
     * Abriu o detalhe de uma partida. `has_open_slots` separa quem olhou uma
     * partida em que dava para entrar de quem olhou uma já lotada — sem isso a
     * queda entre ver e entrar fica impossível de ler.
     */
    public class MatchViewed(sport: String, hasOpenSlots: Boolean) : AnalyticsEvent(
        name = "match_viewed",
        params = mapOf("sport" to sport, "has_open_slots" to hasOpenSlots.toString()),
    )

    /** Tocou em entrar. Emparelha com [MatchJoinResult] para medir a falha. */
    public class MatchJoinAttempted(sport: String) : AnalyticsEvent(
        name = "match_join_attempted",
        params = mapOf("sport" to sport),
    )

    /**
     * Como terminou a tentativa. Um evento com dimensão, e não três eventos
     * separados, porque a pergunta é sempre a mesma — quantos entraram, quantos
     * caíram na fila, quantos falharam — e uma dimensão responde as três.
     */
    public class MatchJoinResult(sport: String, outcome: JoinOutcome) : AnalyticsEvent(
        name = "match_join_result",
        params = mapOf("sport" to sport, "outcome" to outcome.value),
    )

    /** Saiu de uma partida em que já estava. */
    public class MatchLeft(sport: String) : AnalyticsEvent(
        name = "match_left",
        params = mapOf("sport" to sport),
    )

    /** Criou partida — a outra ponta do marketplace. */
    public class MatchCreated(sport: String) : AnalyticsEvent(
        name = "match_created",
        params = mapOf("sport" to sport),
    )

    /**
     * Fim do funil. É **proxy** de "jogou", não prova: avaliar exige que a
     * partida tenha acabado e que quem avalia tenha estado nela, mas quem jogou
     * e não avaliou não aparece aqui. Ler como piso, nunca como total.
     */
    public class PlayerRated(stars: Int) : AnalyticsEvent(
        name = "player_rated",
        params = mapOf("stars" to stars.toString()),
    )
}

/** De onde a pessoa estava olhando partidas. */
public enum class MatchListSource(public val value: String) {
    HOME("home"),
    SEARCH("search"),
    MAP("map"),
}

/** Desfecho de uma tentativa de entrar. */
public enum class JoinOutcome(public val value: String) {
    CONFIRMED("confirmed"),
    WAITLIST("waitlist"),
    FAILED("failed"),
}
