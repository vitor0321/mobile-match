package com.walcker.games.features.domain.model

/**
 * O toggle "estou disponível" (regra B5), como está em
 * `profiles/{uid}/private/data`.
 *
 * É o que decide quem recebe aviso de partida nova: `selectRecipients` nas
 * Functions passou a filtrar por isto. Enquanto o toggle não existia o filtro
 * ficava desligado — `isAvailable` nasce `false` em `onUserCreate` e nada o
 * ligava, então aplicá-lo teria zerado todas as notificações.
 *
 * @param availableUntilMs fim da janela, em epoch millis. `null` é "até eu
 *   desligar", que é o que o toggle grava hoje. O campo já existe no schema e o
 *   servidor já respeita o vencimento, então uma tela futura de "disponível até
 *   domingo" não precisa mexer em mais nada.
 */
internal data class Availability(
    val isAvailable: Boolean = false,
    val availableUntilMs: Long? = null,
) {
    /**
     * Disponível neste instante: o toggle ligado e a janela ainda aberta.
     *
     * Mesma regra do `isAvailableAt` em `functions/src/notifications.ts`,
     * inclusive na borda — no instante exato do vencimento já está fora.
     */
    fun isActiveAt(nowMs: Long): Boolean =
        isAvailable && (availableUntilMs == null || availableUntilMs > nowMs)

    companion object {
        /** Estado de quem nunca tocou no toggle. */
        val Unavailable = Availability()
    }
}
