package com.walcker.identity.api

public data class UserSession(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val creationTimestamp: Long? = null,
)
