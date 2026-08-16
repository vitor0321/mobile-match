package com.walcker.games.features.domain.model

/**
 * Um participante de uma partida. Pode ser confirmado (paga e entrou na partida)
 * ou na fila de espera (waitlist), aguardando liberação de vaga.
 *
 * Fonte: matches/{matchId}/participants/{participantId} subcollection no Firestore.
 * Cada documento denormaliza dados do usuário para evitar leitura extra.
 */
internal data class Participant(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val joinedAt: Long, // unix epoch seconds
    val isConfirmed: Boolean,
    val positionInWaitlist: Int? = null, // null se confirmado
    val hasPaid: Boolean = false,
)

/**
 * Resumo da lista de participantes + contadores agregados.
 */
internal data class ParticipantsSummary(
    val confirmed: List<Participant>,
    val waitlist: List<Participant>,
    val confirmedCount: Int,
    val totalSlots: Int,
) {
    val waitlistCount: Int get() = waitlist.size
    val hasOpenSlots: Boolean get() = confirmedCount < totalSlots
    val openSlots: Int get() = (totalSlots - confirmedCount).coerceAtLeast(0)
}
