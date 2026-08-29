package com.walcker.match.core.navigation

import cafe.adriel.voyager.navigator.Navigator

class NavigatorHolder {
    private val attachment = Attachment<Navigator>()

    val navigator: Navigator?
        get() = attachment.value

    fun attach(navigator: Navigator) {
        attachment.attach(navigator)
    }

    fun detach(navigator: Navigator) {
        attachment.detach(navigator)
    }
}

internal class Attachment<T : Any> {
    var value: T? = null
        private set

    fun attach(value: T) {
        this.value = value
    }

    fun detach(value: T) {
        if (this.value === value) {
            this.value = null
        }
    }
}