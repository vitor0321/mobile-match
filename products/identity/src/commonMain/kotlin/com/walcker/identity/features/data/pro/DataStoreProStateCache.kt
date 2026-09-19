package com.walcker.identity.features.data.pro

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

internal class DataStoreProStateCache(
    private val dataStore: DataStore<Preferences>,
) : ProStateCache {
    override suspend fun read(uid: String): Boolean = dataStore.data.first()[isProKey(uid)] ?: false

    override suspend fun save(
        uid: String,
        isPro: Boolean,
    ) {
        dataStore.edit { preferences ->
            preferences[isProKey(uid)] = isPro
        }
    }

    override suspend fun readRegistrationDate(uid: String): String? = dataStore.data.first()[registrationDateKey(uid)]

    override suspend fun saveRegistrationDate(
        uid: String,
        dateStr: String,
    ) {
        dataStore.edit { preferences ->
            preferences[registrationDateKey(uid)] = dateStr
        }
    }

    override suspend fun clear(uid: String) {
        dataStore.edit { preferences ->
            preferences.remove(isProKey(uid))
            preferences.remove(registrationDateKey(uid))
        }
    }
}

private fun isProKey(uid: String) = booleanPreferencesKey("is_pro_$uid")

private fun registrationDateKey(uid: String) = stringPreferencesKey("registration_date_$uid")
