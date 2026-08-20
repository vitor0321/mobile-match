package com.walcker.games.features.domain.model

/**
 * Uma partida que já existe: quadra reservada, organizador definido e um número
 * de vagas em aberto. O produto conecta essas vagas a jogadores disponíveis.
 *
 * Fonte: matches/{matchId} collection no Firestore com denormalização de organizador.
 */
internal data class Game(
    val id: String,
    val sport: Sport,
    val venueName: String,
    val neighborhood: String,
    val city: String,
    val address: String,
    // Location data for geohash queries
    val lat: Double,
    val lng: Double,
    val geohash: String,
    // Time in seconds since epoch (Firestore Timestamp)
    val startsAtSeconds: Long,
    val durationMin: Int,
    val confirmedPlayers: Int,
    val totalPlayers: Int,
    val pricePerPlayer: String?, // nullable: free matches have null
    val organizerName: String,
    val organizerId: String,
    val organizerRating: Double,
    val status: MatchStatus = MatchStatus.OPEN,
    /**
     * User IDs that have joined the match. Includes the organizer.
     * Empty list means only the organizer is in.
     */
    val participants: List<String> = emptyList(),
) {
    val openSlots: Int
        get() = (totalPlayers - confirmedPlayers).coerceAtLeast(0)

    val hasOpenSlots: Boolean
        get() = openSlots > 0

    /**
     * Fim da partida, em segundos desde a época. Duração negativa conta como
     * zero, espelhando o `Math.max(durationMin, 0)` do servidor.
     */
    val endsAtSeconds: Long
        get() = startsAtSeconds + durationMin.coerceAtLeast(0).toLong() * 60

    /**
     * A partida já acabou, pelo relógio.
     *
     * [status] nunca vira [MatchStatus.FINISHED]: nada no produto escreve esse
     * valor. Quem decide é o horário, e o critério tem de ser exatamente o do
     * `requireMatchIsOver` das Functions — que roda antes de aceitar
     * `submitPlayerRating`. Se divergir, a tela oferece uma ação que o servidor
     * recusa.
     */
    fun isOver(nowSeconds: Long): Boolean = endsAtSeconds <= nowSeconds
}

/**
 * As três condições que `submitPlayerRating` checa no servidor, na mesma ordem:
 * a partida acabou, não foi cancelada, e quem está avaliando jogou.
 *
 * Fica aqui, e não dentro do StepModel, para a regra poder ser testada sem
 * montar a tela inteira — e para a próxima tela que precisar dela não
 * reimplementar por conta própria.
 */
internal fun Game.canBeRatedBy(userId: String?, nowSeconds: Long): Boolean =
    isOver(nowSeconds) &&
        status != MatchStatus.CANCELLED &&
        userId != null &&
        userId in participants

/**
 * Role do usuário logado em relação a uma partida.
 */
internal enum class MatchRole {
    ORGANIZER,
    PARTICIPANT,
}

/**
 * Match status in Firestore.
 */
internal enum class MatchStatus {
    OPEN,
    FULL,
    CANCELLED,
    FINISHED,
}
