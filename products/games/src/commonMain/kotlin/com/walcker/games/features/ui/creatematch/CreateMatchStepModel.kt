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
import com.walcker.match.core.geo.DefaultCenter
import com.walcker.match.core.geo.encodeGeoHash
import com.walcker.match.core.location.LocationProvider
import com.walcker.match.core.location.ReverseGeocoder
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L

internal class CreateMatchStepModel(
    private val createMatch: CreateMatchUseCase,
    private val stringsHolder: GamesStringsHolder,
    private val sessionHolder: SessionHolder,
    private val tabCoordinator: TabCoordinator,
    private val analytics: AnalyticsTracker,
    private val locationProvider: LocationProvider,
    private val reverseGeocoder: ReverseGeocoder,
) : ScreenModel {

    private val strings get() = stringsHolder.resolveStringsOrDefault().createMatch

    private val _state = MutableStateFlow(CreateMatchState())
    val state: StateFlow<CreateMatchState> = _state.asStateFlow()

    private val _effects = Channel<CreateMatchEffect>(Channel.BUFFERED)
    val effects: Flow<CreateMatchEffect> = _effects.receiveAsFlow()

    init {
        resolveInitialLocation()
    }

    private fun resolveInitialLocation() {
        screenModelScope.launch {
            _state.update { it.copy(isResolvingLocation = true) }

            val current = withTimeoutOrNull(5.seconds) {
                if (locationProvider.requestPermission()) {
                    locationProvider.currentLocation().getOrNull()
                } else {
                    null
                }
            }

            selectLocation(
                lat = current?.lat ?: DefaultCenter.lat,
                lng = current?.lng ?: DefaultCenter.lng,
            )
        }
    }

    fun onEvent(event: CreateMatchEvents) {
        when (event) {
            is CreateMatchEvents.VenueNameChanged -> {
                _state.update { it.copy(venueName = event.name, venueNameError = null) }
            }
            is CreateMatchEvents.SportSelected -> {
                _state.update { it.copy(selectedSport = event.sport, sportError = null) }
            }
            is CreateMatchEvents.LocationSelected -> selectLocation(event.lat, event.lng)
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

    private fun selectLocation(lat: Double, lng: Double) {
        _state.update { it.copy(lat = lat, lng = lng, isResolvingLocation = true) }
        screenModelScope.launch {
            val geocoded = reverseGeocoder.reverseGeocode(lat, lng)
            _state.update {
                it.copy(
                    neighborhood = geocoded?.neighborhood.orEmpty(),
                    city = geocoded?.city.orEmpty(),
                    address = geocoded?.address.orEmpty(),
                    isResolvingLocation = false,
                )
            }
        }
    }

    private fun submitForm() {
        val currentState = _state.value
        if (!currentState.isFormValid) return

        val selectedSport = currentState.selectedSport ?: return
        val selectedDate = currentState.selectedDate ?: return
        val (hour, minute) = currentState.selectedTime ?: return
        val lat = currentState.lat ?: return
        val lng = currentState.lng ?: return

        screenModelScope.launch {
            val session = sessionHolder.currentUser.first()
            if (session == null) {
                _effects.send(CreateMatchEffect.RequireLogin)
                return@launch
            }

            _state.update { it.copy(isSubmitting = true) }

            try {
                val startSeconds = (selectedDate / MILLIS_PER_SECOND) +
                    (hour * SECONDS_PER_HOUR) +
                    (minute * SECONDS_PER_MINUTE)

                val request = CreateMatchRequest(
                    sport = selectedSport,
                    venueName = currentState.venueName,
                    neighborhood = currentState.neighborhood,
                    city = currentState.city,
                    address = currentState.address,
                    lat = lat,
                    lng = lng,
                    geohash = encodeGeoHash(Coordinates(lat, lng)),
                    startsAtSeconds = startSeconds,
                    durationMin = currentState.durationMin,
                    totalPlayers = currentState.totalPlayers,
                    pricePerPlayer = currentState.pricePerPlayer.takeIf { it.isNotBlank() },
                )

                createMatch(request)
                    .onSuccess { matchId ->
                        analytics.track(AnalyticsEvent.MatchCreated(request.sport.name))
                        _effects.send(CreateMatchEffect.NavigateToMyMatches(matchId))
                        tabCoordinator.requestTab(MainTab.MyMatches)
                    }
                    .onFailure {
                        _effects.send(CreateMatchEffect.ShowMessage(strings.genericError))
                    }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
