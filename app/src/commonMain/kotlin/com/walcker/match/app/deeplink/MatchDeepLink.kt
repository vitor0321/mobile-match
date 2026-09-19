package com.walcker.match.app.deeplink

private const val SCHEME_SEPARATOR = "://"

public fun matchIdFromDeepLink(url: String?): String? {
    val link = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val schemeEnd = link.indexOf(SCHEME_SEPARATOR)
    if (schemeEnd <= 0) return null

    val scheme = link.take(schemeEnd).lowercase()
    val afterScheme = link.drop(schemeEnd + SCHEME_SEPARATOR.length).substringBefore('#')
    val path = afterScheme.substringBefore('?')
    val query = afterScheme.substringAfter('?', "")

    idFromQuery(query)?.let { return it }
    if (scheme == "http" || scheme == "https") return null

    return path
        .split('/')
        .drop(1)
        .lastOrNull { it.isNotBlank() }
}

private fun idFromQuery(query: String): String? =
    query
        .split('&')
        .firstOrNull { it.startsWith("id=") }
        ?.removePrefix("id=")
        ?.takeIf { it.isNotBlank() }
