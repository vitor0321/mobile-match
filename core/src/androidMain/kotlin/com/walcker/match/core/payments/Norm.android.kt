package com.walcker.match.core.payments

import java.text.Normalizer

internal actual fun String.normalizeToNFD(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
