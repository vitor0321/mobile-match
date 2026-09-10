package com.walcker.identity.features.data.pro

internal interface ProStateCache {
    suspend fun read(uid: String): Boolean

    suspend fun save(
        uid: String,
        isPro: Boolean,
    )

    suspend fun readRegistrationDate(uid: String): String?

    suspend fun saveRegistrationDate(
        uid: String,
        dateStr: String,
    )

    suspend fun clear(uid: String)
}
