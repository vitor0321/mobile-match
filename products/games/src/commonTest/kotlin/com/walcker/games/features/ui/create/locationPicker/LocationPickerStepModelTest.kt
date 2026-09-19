package com.walcker.games.features.ui.create.locationPicker

import com.walcker.games.fake.FakeAddressGeocoder
import com.walcker.games.fake.FakeReverseGeocoder
import com.walcker.match.core.location.GeocodedAddress
import com.walcker.match.core.location.GeocodedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPickerStepModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(
        reverseGeocoder: FakeReverseGeocoder = FakeReverseGeocoder(),
        addressGeocoder: FakeAddressGeocoder = FakeAddressGeocoder(),
        initialLat: Double = -23.55,
        initialLng: Double = -46.63,
    ) = LocationPickerStepModel(
        initialLat = initialLat,
        initialLng = initialLng,
        reverseGeocoder = reverseGeocoder,
        addressGeocoder = addressGeocoder,
    )

    @Test
    fun `resolves the address for the initial location on creation`() =
        runTest(testDispatcher) {
            val reverseGeocoder =
                FakeReverseGeocoder(
                    result = GeocodedAddress(address = "Rua Um, 100", neighborhood = "Centro", city = "São Paulo"),
                )
            val model = buildModel(reverseGeocoder = reverseGeocoder)

            advanceUntilIdle()

            val state = model.state.value
            assertEquals("Rua Um, 100", state.address)
            assertEquals("Centro", state.neighborhood)
            assertEquals("São Paulo", state.city)
            assertFalse(state.isResolvingLocation)
            assertEquals(listOf(-23.55 to -46.63), reverseGeocoder.calls)
        }

    @Test
    fun `a failed reverse geocode clears the address fields`() =
        runTest(testDispatcher) {
            val model = buildModel(reverseGeocoder = FakeReverseGeocoder(result = null))

            advanceUntilIdle()

            val state = model.state.value
            assertEquals("", state.address)
            assertEquals("", state.neighborhood)
            assertEquals("", state.city)
            assertFalse(state.isResolvingLocation)
        }

    @Test
    fun `changing the location re-resolves the address`() =
        runTest(testDispatcher) {
            val reverseGeocoder = FakeReverseGeocoder()
            val model = buildModel(reverseGeocoder = reverseGeocoder)
            advanceUntilIdle()

            reverseGeocoder.result = GeocodedAddress(address = "Avenida Dois, 200", neighborhood = "Jardins", city = "São Paulo")
            model.onLocationChanged(-23.56, -46.64)
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(-23.56, state.lat)
            assertEquals(-46.64, state.lng)
            assertEquals("Avenida Dois, 200", state.address)
            assertEquals(2, reverseGeocoder.calls.size)
        }

    @Test
    fun `an empty address query submit does nothing`() =
        runTest(testDispatcher) {
            val addressGeocoder = FakeAddressGeocoder()
            val model = buildModel(addressGeocoder = addressGeocoder)
            advanceUntilIdle()

            model.onAddressSearchSubmit()
            advanceUntilIdle()

            assertTrue(addressGeocoder.queries.isEmpty())
        }

    @Test
    fun `submitting an address query updates the location and requests focus`() =
        runTest(testDispatcher) {
            val addressGeocoder =
                FakeAddressGeocoder(
                    result = GeocodedLocation(lat = -23.50, lng = -46.60, address = "Praça Central", neighborhood = "Sé", city = "São Paulo"),
                )
            val model = buildModel(addressGeocoder = addressGeocoder)
            advanceUntilIdle()

            model.onAddressQueryChanged("praça central")
            model.onAddressSearchSubmit()
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(-23.50, state.lat)
            assertEquals(-46.60, state.lng)
            assertEquals("Praça Central", state.address)
            assertFalse(state.isSearching)
            assertFalse(state.searchError)
            assertEquals(PickedLocation(-23.50, -46.60), state.focusRequest)
            assertEquals(listOf("praça central"), addressGeocoder.queries)
        }

    @Test
    fun `a failed address search surfaces the error flag`() =
        runTest(testDispatcher) {
            val model = buildModel(addressGeocoder = FakeAddressGeocoder(result = null))
            advanceUntilIdle()

            model.onAddressQueryChanged("endereço inexistente")
            model.onAddressSearchSubmit()
            advanceUntilIdle()

            val state = model.state.value
            assertTrue(state.searchError)
            assertFalse(state.isSearching)
            assertNull(state.focusRequest)
        }

    @Test
    fun `changing the query clears a previous search error`() =
        runTest(testDispatcher) {
            val model = buildModel(addressGeocoder = FakeAddressGeocoder(result = null))
            advanceUntilIdle()
            model.onAddressQueryChanged("endereço inexistente")
            model.onAddressSearchSubmit()
            advanceUntilIdle()
            assertTrue(model.state.value.searchError)

            model.onAddressQueryChanged("novo endereço")

            assertFalse(model.state.value.searchError)
            assertEquals("novo endereço", model.state.value.addressQuery)
        }
}
