package com.walcker.games.features.ui.create.locationPicker

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.walcker.match.core.location.AddressGeocoder
import com.walcker.match.core.location.ReverseGeocoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LocationPickerState(
    val lat: Double,
    val lng: Double,
    val address: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val isResolvingLocation: Boolean = true,
    val addressQuery: String = "",
    val isSearching: Boolean = false,
    val searchError: Boolean = false,
    val focusRequest: PickedLocation? = null,
)

internal class LocationPickerStepModel(
    initialLat: Double,
    initialLng: Double,
    private val reverseGeocoder: ReverseGeocoder,
    private val addressGeocoder: AddressGeocoder,
) : ScreenModel {
    private val _state = MutableStateFlow(LocationPickerState(lat = initialLat, lng = initialLng))
    val state: StateFlow<LocationPickerState> = _state.asStateFlow()

    init {
        resolveAddress(initialLat, initialLng)
    }

    fun onLocationChanged(
        lat: Double,
        lng: Double,
    ) {
        _state.update { it.copy(lat = lat, lng = lng, isResolvingLocation = true) }
        resolveAddress(lat, lng)
    }

    fun onAddressQueryChanged(query: String) {
        _state.update { it.copy(addressQuery = query, searchError = false) }
    }

    fun onAddressSearchSubmit() {
        val query = _state.value.addressQuery
        if (query.isBlank()) return

        screenModelScope.launch {
            _state.update { it.copy(isSearching = true, searchError = false) }
            val result = addressGeocoder.geocodeAddress(query)
            if (result == null) {
                _state.update { it.copy(isSearching = false, searchError = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    lat = result.lat,
                    lng = result.lng,
                    address = result.address,
                    neighborhood = result.neighborhood,
                    city = result.city,
                    isResolvingLocation = false,
                    isSearching = false,
                    focusRequest = PickedLocation(result.lat, result.lng),
                )
            }
        }
    }

    private fun resolveAddress(
        lat: Double,
        lng: Double,
    ) {
        screenModelScope.launch {
            val geocoded = reverseGeocoder.reverseGeocode(lat, lng)
            _state.update {
                it.copy(
                    address = geocoded?.address.orEmpty(),
                    neighborhood = geocoded?.neighborhood.orEmpty(),
                    city = geocoded?.city.orEmpty(),
                    isResolvingLocation = false,
                )
            }
        }
    }
}
