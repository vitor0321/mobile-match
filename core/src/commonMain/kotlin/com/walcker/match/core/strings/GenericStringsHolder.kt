package com.walcker.match.core.strings

internal open class GenericStringsHolder<T : Any> {
    private var currentStrings: T? = null

    val strings: T
        get() = currentStrings ?: error("Strings not initialized. Make sure to call setStrings() first.")

    fun setStrings(strings: T) {
        currentStrings = strings
    }

    fun clearStrings() {
        currentStrings = null
    }

    fun hasStrings(): Boolean = currentStrings != null
}