package com.walcker.games.features.ui.creatematch

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.usecase.CreateMatchUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.analytics.AnalyticsEvent
import com.walcker.match.core.analytics.AnalyticsTracker
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.geo.encodeGeoHash
import com.walcker.match.navigator.MainTab
import com.walcker.match.navigator.TabCoordinator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L

internal class CreateMatchStepModel(
    private val createMatch: CreateMatchUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val sessionHolder: SessionHolder,
    private val tabCoordinator: TabCoordinator,
    private val analytics: AnalyticsTracker,
) : ScreenModel {

    // Era `.gameList`: a tela de criação falava com as mensagens da listagem, e o
    // arquivo de textos próprio dela ficava sem uso.
    private val strings get() = stringsHolder.resolveStringsOrDefault().createMatch

    // TODO Phase 3: replace with real LocationProvider. For now hardcoded to SP center.
    private val defaultLat = -23.5505
    private val defaultLng = -46.6333

    private val defaultGeohash: String
        get() = encodeGeoHash(Coordinates(defaultLat, defaultLng))

    private val _state = MutableStateFlow(CreateMatchState())
    val state: StateFlow<CreateMatchState> = _state.asStateFlow()

    private val _effects = Channel<CreateMatchEffect>(Channel.BUFFERED)
    val effects: Flow<CreateMatchEffect> = _effects.receiveAsFlow()

    fun onEvent(event: CreateMatchEvents) {
        when (event) {
            is CreateMatchEvents.VenueNameChanged -> {
                _state.update { it.copy(venueName = event.name, venueNameError = null) }
            }
            is CreateMatchEvents.SportSelected -> {
                _state.update { it.copy(selectedSport = event.sport, sportError = null) }
            }
            is CreateMatchEvents.NeighborhoodChanged -> {
                _state.update {
                    it.copy(neighborhood = event.neighborhood, neighborhoodError = null)
                }
            }
            is CreateMatchEvents.CityChanged -> {
                _state.update { it.copy(city = event.city, cityError = null) }
            }
            is CreateMatchEvents.AddressChanged -> {
                _state.update { it.copy(address = event.address, addressError = null) }
            }
            is CreateMatchEvents.DateSelected -> {
                _state.update { it.copy(selectedDate = event.dateMillis, dateError = null) }
            }
            is CreateMatchEvents.TimeSelected -> {
                _state.update { it.copy(selectedTime = event.hour to event.minute, timeError = null) }
            }
            is CreateMatchEvents.DurationSelected -> {
                _state.update { it.copy(durationMin = event.durationMin) }
            }
            is CreateMatchEvents.PlayersChanged -> {
                _state.update { it.copy(totalPlayers = event.totalPlayers, playersError = null) }
            }
            is CreateMatchEvents.PriceChanged -> {
                _state.update { it.copy(pricePerPlayer = event.price, priceError = null) }
            }
            is CreateMatchEvents.Submit -> submitForm()
        }
    }

    private fun submitForm() {
        val currentState = _state.value
        if (!currentState.isFormValid) return

        val selectedSport = currentState.selectedSport ?: return
        val selectedDate = currentState.selectedDate ?: return
        val (hour, minute) = currentState.selectedTime ?: return

        screenModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }

            try {
                val startSeconds = (selectedDate / MILLIS_PER_SECOND) +
                    (hour * SECONDS_PER_HOUR) +
                    (minute * SECONDS_PER_MINUTE)

                // Ensure the user is logged in. Without a session we cannot scope
                // the new match to the right user document.
                val session = sessionHolder.currentUser.first()
                if (session == null) {
                    _effects.send(CreateMatchEffect.ShowMessage(strings.notLoggedIn))
                    return@launch
                }

                val request = CreateMatchRequest(
                    sport = selectedSport,
                    venueName = currentState.venueName,
                    neighborhood = currentState.neighborhood,
                    city = currentState.city,
                    address = currentState.address,
                    lat = defaultLat, // TODO Phase 3: integrate LocationProvider
                    lng = defaultLng, // TODO Phase 3: integrate LocationProvider
                    geohash = defaultGeohash,
                    startsAtSeconds = startSeconds,
                    durationMin = currentState.durationMin,
                    totalPlayers = currentState.totalPlayers,
                    pricePerPlayer = currentState.pricePerPlayer.takeIf { it.isNotBlank() },
                )

                createMatch(request)
                    .onSuccess { matchId ->
                        // A outra ponta do marketplace: sem oferta, o funil de
                        // entrada não tem do que viver.
                        analytics.track(AnalyticsEvent.MatchCreated(request.sport.name))
                        // Um efeito só. Antes iam dois — um ShowMessage e um
                        // NavigateToMyMatches que também mostrava snackbar — e o
                        // usuário via duas mensagens seguidas, a segunda com o id
                        // do documento do Firestore.
                        _effects.send(CreateMatchEffect.NavigateToMyMatches(matchId))
                        // Ask the navigation shell to switch to the My Matches tab.
                        tabCoordinator.requestTab(MainTab.MyMatches)
                    }
                    .onFailure {
                        // `error.message` aqui é a mensagem da exceção — em inglês e
                        // técnica. O transporte de callable do Firebase não preserva
                        // o código do HttpsError, então não há taxonomia a montar:
                        // uma mensagem traduzida é mais honesta que um stack trace.
                        _effects.send(CreateMatchEffect.ShowMessage(strings.genericError))
                    }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
