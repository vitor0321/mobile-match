package com.walcker.games.features.ui.playerprofile

import com.walcker.games.features.domain.model.Rating

internal data class PlayerProfileState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val matchesOrganized: Int = 0,
    val matchesParticipated: Int = 0,
    val ratings: List<Rating> = emptyList(),
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val errorMessage: String? = null,
    /**
     * O toggle da regra B5. Espelha `profiles/{uid}/private/data.isAvailable`,
     * que é o campo que o `selectRecipients` das Functions consulta para
     * decidir quem recebe aviso de partida nova.
     */
    val isAvailable: Boolean = false,
    /**
     * O switch está esperando a gravação. Separado de [isLoading], que cobre a
     * tela inteira — travar o perfil todo por causa de um toque no switch seria
     * desproporcional.
     */
    val isUpdatingAvailability: Boolean = false,
    /**
     * Falha só da gravação do toggle. Separado de [errorMessage] pelo mesmo
     * motivo: uma disponibilidade que não gravou não pode esconder o perfil.
     */
    val availabilityErrorMessage: String? = null,
)

internal sealed interface PlayerProfileEvent {
    data object Refresh : PlayerProfileEvent
    data object DismissError : PlayerProfileEvent
    data object LogoutRequested : PlayerProfileEvent
    /** Liga ou desliga "estou disponível". */
    data class AvailabilityChanged(val isAvailable: Boolean) : PlayerProfileEvent
    data object DismissAvailabilityError : PlayerProfileEvent
}
