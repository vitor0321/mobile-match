package com.walcker.games.features.data.home.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.walcker.games.features.domain.shared.model.Sport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

internal class GamesPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    private val _selectedSport = MutableStateFlow<Sport?>(null)
    val selectedSport: StateFlow<Sport?> = _selectedSport.asStateFlow()

    val radiusKm: Flow<Double> =
        dataStore.data.map { prefs ->
            prefs[KEY_RADIUS_KM] ?: DEFAULT_RADIUS_KM
        }

    val lastLocation: Flow<LastLocation?> =
        dataStore.data.map { prefs ->
            val lat = prefs[KEY_LAST_LAT]
            val lng = prefs[KEY_LAST_LNG]
            if (lat != null && lng != null) LastLocation(lat, lng) else null
        }

    val lastSyncAt: Flow<Long?> = dataStore.data.map { prefs -> prefs[KEY_LAST_SYNC_AT] }

    fun setSelectedSport(sport: Sport?) {
        _selectedSport.value = sport
    }

    suspend fun setRadiusKm(radiusKm: Double) {
        dataStore.edit { prefs -> prefs[KEY_RADIUS_KM] = radiusKm }
    }

    suspend fun setLastLocation(
        latitude: Double,
        longitude: Double,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_LAT] = latitude
            prefs[KEY_LAST_LNG] = longitude
        }
    }

    suspend fun setLastSyncAt(epochMillis: Long) {
        dataStore.edit { prefs -> prefs[KEY_LAST_SYNC_AT] = epochMillis }
    }

    data class LastLocation(
        val latitude: Double,
        val longitude: Double,
    )

    private companion object {
        const val DEFAULT_RADIUS_KM: Double = 15.0

        val KEY_RADIUS_KM = doublePreferencesKey("radius_km")
        val KEY_LAST_LAT = doublePreferencesKey("last_lat")
        val KEY_LAST_LNG = doublePreferencesKey("last_lng")
        val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }
}
