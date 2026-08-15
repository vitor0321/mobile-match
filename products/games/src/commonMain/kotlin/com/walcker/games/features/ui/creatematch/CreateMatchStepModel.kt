package com.walcker.games.features.ui.creatematch

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.usecase.CreateMatchUseCase
import com.walcker.games.strings.GamesStringsHolder
import com.walcker.games.strings.resolveStringsOrDefault
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal class CreateMatchStepModel(
    private val createMatch: CreateMatchUseCase,
    private val stringsHolder: GamesStringsHolder,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().gameList

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

                val request = CreateMatchRequest(
                    sport = currentState.selectedSport!!,
                    venueName = currentState.venueName,
                    neighborhood = currentState.neighborhood,
                    city = currentState.city,
                    address = currentState.address,
                    lat = -23.5505, // TODO: Get from SessionHolder or LocationProvider
                    lng = -46.6333, // TODO: Get from SessionHolder or LocationProvider
                    geohash = "abc123", // TODO: Compute geohash from lat/lng
                    startsAtSeconds = startSeconds,
                    durationMin = currentState.durationMin,
                    totalPlayers = currentState.totalPlayers,
                    pricePerPlayer = currentState.pricePerPlayer.takeIf { it.isNotBlank() },
                )

                createMatch(request)
                    .onSuccess { matchId ->
                        _effects.send(CreateMatchEffect.ShowMessage(strings.joinSuccess))
                        _effects.send(CreateMatchEffect.NavigateToMyMatches(matchId))
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
