package com.walcker.match.navigator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Evento de promoção do usuário da fila de espera para confirmado em uma partida.
 *
 * O Cloud Function de promoção (B3 na arquitetura) é o source of truth para
 * a transição de status; o app apenas reage ao listener e mostra o banner.
 *
 * Definido aqui no navigator (em vez de em games) para evitar que o módulo
 * navigator dependa de games — mesmo padrão do [DeepLink].
 */
public data class PromotionNotice(
    val matchId: String,
    val matchTitle: String,
    val promotedAtMs: Long,
)

/**
 * Coordinator para eventos de promoção da waitlist.
 *
 * Singleton no app; o MatchDetailStepModel emite quando detecta que o
 * usuário logado acabou de ser promovido (via listener do Firestore),
 * e o shell pode mostrar um banner/snackbar global.
 *
 * Buffered para não bloquear se ninguém estiver coletando no momento.
 */
public class PromotionCoordinator {
    private val channel = Channel<PromotionNotice>(Channel.BUFFERED)
    public val promotions: Flow<PromotionNotice> = channel.receiveAsFlow()

    public fun emit(notice: PromotionNotice) {
        channel.trySend(notice)
    }
}
