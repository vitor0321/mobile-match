package com.walcker.identity.features.domain.phone

private const val ACCENTED_LETTERS = "àáâãäåçèéêëìíîïñòóôõöøùúûüýÿ"
private const val ASCII_LETTERS = "aaaaaaceeeeiiiinoooooouuuuyy"

private val MULTI_LETTER_EXPANSIONS = mapOf('æ' to "ae", 'ß' to "ss")

internal fun foldAccents(value: String): String {
    val lowercased = value.lowercase()
    return buildString(lowercased.length) {
        lowercased.forEach { char ->
            val accentIndex = ACCENTED_LETTERS.indexOf(char)
            when {
                accentIndex >= 0 -> append(ASCII_LETTERS[accentIndex])
                MULTI_LETTER_EXPANSIONS.containsKey(char) -> append(MULTI_LETTER_EXPANSIONS.getValue(char))
                else -> append(char)
            }
        }
    }
}
