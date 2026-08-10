package com.walcker.identity.fake

import com.walcker.identity.features.data.pro.ProStateCache

internal class FakeProStateCache(
    initialIsPro: Boolean = false,
) : ProStateCache {
    private val proStates = mutableMapOf<String, Boolean>().apply { put(DEFAULT_UID, initialIsPro) }
    private val registrationDates = mutableMapOf<String, String>()
    val savedValues = mutableListOf<Pair<String, Boolean>>()

    override suspend fun read(uid: String): Boolean = proStates[uid] ?: false

    override suspend fun save(uid: String, isPro: Boolean) {
        proStates[uid] = isPro
        savedValues += uid to isPro
    }

    override suspend fun readRegistrationDate(uid: String): String? = registrationDates[uid]

    override suspend fun saveRegistrationDate(uid: String, dateStr: String) {
        registrationDates[uid] = dateStr
    }

    override suspend fun clear(uid: String) {
        proStates.remove(uid)
        registrationDates.remove(uid)
    }

    private companion object {
        const val DEFAULT_UID = "uid-1"
    }
}
