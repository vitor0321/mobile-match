package com.walcker.games.features.domain.model

/**
 * Resultado de chamar o Cloud Function `leaveMatch`.
 *
 * Se o usuário era confirmado, o Cloud Function pode ter promovido o primeiro
 * da fila (regra B3) — o app usa [promotedUserId] para mostrar o banner local
 * ou sincronizar estado se for o próprio usuário.
 */
internal data class LeaveMatchOutcome(
    val matchId: String,
    /** UID do usuário promovido da waitlist, se houve promoção. Null caso contrário. */
    val promotedUserId: String? = null,
)
