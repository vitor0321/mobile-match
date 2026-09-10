package com.walcker.match.core.payments

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

internal actual fun String.normalizeToNFD(): String {
    @Suppress("UNCHECKED_CAST")
    val ns = this as NSString
    return ns.decomposedStringWithCanonicalMapping
}
