package com.walcker.games.features.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.walcker.games.features.domain.model.Sport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reactive, DataStore-backed preferences for the games product.
 *
 * Holds user-tunable filters and the last-known location used for
 * radius queries. Exposed as Flows so screens can recompose when the
 * user changes a setting.
 */
internal class GamesPreferences(
    private val dataStore: DataStore<Preferences>,
) {

    /** Selected sport filter, or `null` for "all sports". */
    val selectedSport: Flow<Sport?> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_SPORT]
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Sport.valueOf(it) }.getOrNull() }
    }

    /** Search radius in kilometers. Defaults to [DEFAULT_RADIUS_KM] when unset. */
    val radiusKm: Flow<Double> = dataStore.data.map { prefs ->
        prefs[KEY_RADIUS_KM] ?: DEFAULT_RADIUS_KM
    }

    /** Last known location (lat, lng), or `null` if not yet captured. */
    val lastLocation: Flow<LastLocation?> = dataStore.data.map { prefs ->
        val lat = prefs[KEY_LAST_LAT]
        val lng = prefs[KEY_LAST_LNG]
        if (lat != null && lng != null) LastLocation(lat, lng) else null
    }

    /** Epoch millis of the last successful Firestore sync. */
    val lastSyncAt: Flow<Long?> = dataStore.data.map { prefs -> prefs[KEY_LAST_SYNC_AT] }

    suspend fun setSelectedSport(sport: Sport?) {
        dataStore.edit { prefs ->
            if (sport == null) prefs.remove(KEY_SELECTED_SPORT)
            else prefs[KEY_SELECTED_SPORT] = sport.name
        }
    }

    suspend fun setRadiusKm(radiusKm: Double) {
        dataStore.edit { prefs -> prefs[KEY_RADIUS_KM] = radiusKm }
    }

    suspend fun setLastLocation(latitude: Double, longitude: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_LAT] = latitude
            prefs[KEY_LAST_LNG] = longitude
        }
    }

    suspend fun setLastSyncAt(epochMillis: Long) {
        dataStore.edit { prefs -> prefs[KEY_LAST_SYNC_AT] = epochMillis }
    }

    data class LastLocation(val latitude: Double, val longitude: Double)

    private companion object {
        const val DEFAULT_RADIUS_KM: Double = 15.0

        val KEY_SELECTED_SPORT = stringPreferencesKey("selected_sport")
        val KEY_RADIUS_KM = doublePreferencesKey("radius_km")
        val KEY_LAST_LAT = doublePreferencesKey("last_lat")
        val KEY_LAST_LNG = doublePreferencesKey("last_lng")
        val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }
}
