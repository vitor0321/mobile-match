package com.walcker.games.features.ui.creatematch

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.usecase.CreateMatchUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
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

internal class CreateMatchStepModel(
    private val createMatch: CreateMatchUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val sessionHolder: com.walcker.identity.api.SessionHolder,
    private val tabCoordinator: TabCoordinator,
    private val analytics: AnalyticsTracker,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().gameList

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
                _state.update { it.copy(neighborhood = event.neighborhood, neighborhoodError = null) }
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

        screenModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }

            try {
                val startSeconds = (currentState.selectedDate!! / 1000) +
                        (currentState.selectedTime!!.first * 3600L) +
                        (currentState.selectedTime.second * 60L)

                // Ensure the user is logged in. Without a session we cannot scope
                // the new match to the right user document.
                val session = sessionHolder.currentUser.first()
                if (session == null) {
                    _effects.send(CreateMatchEffect.ShowMessage(strings.joinError))
                    return@launch
                }

                val request = CreateMatchRequest(
                    sport = currentState.selectedSport!!,
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
                        _effects.send(CreateMatchEffect.ShowMessage(strings.joinSuccess))
                        _effects.send(CreateMatchEffect.NavigateToMyMatches(matchId))
                        // Ask the navigation shell to switch to the My Matches tab.
                        tabCoordinator.requestTab(MainTab.MyMatches)
                    }
                    .onFailure { error ->
                        _effects.send(CreateMatchEffect.ShowMessage(error.message ?: strings.joinError))
                    }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
