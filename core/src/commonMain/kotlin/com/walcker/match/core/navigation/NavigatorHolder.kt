package com.walcker.match.core.navigation


class NavigatorHolder {
    private val attachment = Attachment<Navigator>()

    val navigator: Navigator?
        get() = attachment.value

    /** Registers the navigator associated with the active root composition. */
    fun attach(navigator: Navigator) {
        attachment.attach(navigator)
    }

    /**
     * Removes [navigator] only when it is still the active attachment.
     *
     * A composition disposed after a replacement must not clear the navigator that replaced it.
     */
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
