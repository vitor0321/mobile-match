package com.walcker.match.navigator

import cafe.adriel.voyager.core.screen.Screen

public interface IdentityDestination {
    fun login(): Screen
    fun signUp(): Screen
    fun paywall(): Screen
    fun profile(): Screen
}