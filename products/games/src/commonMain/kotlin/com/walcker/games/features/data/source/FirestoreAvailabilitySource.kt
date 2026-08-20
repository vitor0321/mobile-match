package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Availability
import com.walcker.match.firestore.DocumentSnapshot
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação Firestore de [AvailabilitySource].
 *
 * Os nomes dos campos são contrato com `parseCandidate` em
 * `functions/src/notifications.ts`: é aquele parser que decide quem recebe
 * aviso de partida. Errar um nome aqui não quebra compilação nenhuma — só
 * deixa a pessoa invisível para as notificações, silenciosamente.
 */
internal class FirestoreAvailabilitySource(
    private val firestore: FirestoreClient,
) : AvailabilitySource {

    override fun observe(userId: String): Flow<Result<Availability>> =
        firestore.document(privatePath(userId))
            .snapshots()
            .map { result -> result.map { snapshot -> snapshot.toAvailability() } }

    override suspend fun setAvailable(
        userId: String,
        availability: Availability,
    ): Result<Unit> = firestore.document(privatePath(userId)).update(
        // update é merge. `set` apagaria telefone, chave Pix e coordenadas, que
        // moram neste mesmo documento.
        mapOf(
            FIELD_IS_AVAILABLE to availability.isAvailable,
            FIELD_AVAILABLE_UNTIL to availability.availableUntilMs,
        ),
    )

    /**
     * Documento ausente é indisponível, não erro: quem acabou de se cadastrar
     * pode chegar na tela antes do `onUserCreate` terminar de gravar.
     */
    private fun DocumentSnapshot?.toAvailability(): Availability {
        if (this == null) return Availability.Unavailable

        return Availability(
            isAvailable = getBoolean(FIELD_IS_AVAILABLE) ?: false,
            // getTimestamp normaliza tanto número quanto Timestamp do SDK — o
            // campo pode ter sido gravado das duas formas.
            availableUntilMs = getTimestamp(FIELD_AVAILABLE_UNTIL),
        )
    }

    private companion object {
        fun privatePath(userId: String) = "profiles/$userId/private/data"

        const val FIELD_IS_AVAILABLE = "isAvailable"
        const val FIELD_AVAILABLE_UNTIL = "availableUntil"
    }
}
