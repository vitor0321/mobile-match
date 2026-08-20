package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.Availability
import kotlinx.coroutines.flow.Flow

/**
 * Acesso ao toggle de disponibilidade em `profiles/{uid}/private/data`.
 *
 * Documento privado do dono — as regras liberam leitura e escrita só para ele
 * (`match /private/{documentId} { allow read, write: if isOwner(userId); }`).
 * Por isso não existe versão "de outra pessoa" desta API.
 */
internal interface AvailabilitySource {
    /**
     * Observa a disponibilidade do próprio usuário.
     *
     * Documento ausente vale como indisponível, não como erro: o `onUserCreate`
     * cria o privado, mas a tela não pode quebrar se ele ainda não chegou.
     */
    fun observe(userId: String): Flow<Result<Availability>>

    /**
     * Grava o toggle. Escrita parcial (merge): telefone, Pix e coordenadas
     * moram no mesmo documento e não podem ser apagados por um toque no switch.
     */
    suspend fun setAvailable(userId: String, availability: Availability): Result<Unit>
}
