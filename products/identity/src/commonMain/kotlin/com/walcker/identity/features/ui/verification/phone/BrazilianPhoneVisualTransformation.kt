package com.walcker.identity.features.ui.verification.phone

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.walcker.identity.features.domain.phone.formatBrazilianNational

internal fun digitOffsetMapping(
    digitCount: Int,
    formatted: String,
): OffsetMapping {
    val digitPositions =
        formatted.indices
            .filter { index -> formatted[index].isDigit() }
            .take(digitCount)

    return object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = if (offset >= digitPositions.size) formatted.length else digitPositions[offset.coerceAtLeast(0)]

        override fun transformedToOriginal(offset: Int): Int = digitPositions.count { position -> position < offset }.coerceIn(0, digitCount)
    }
}

internal object BrazilianPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatBrazilianNational(text.text)
        return TransformedText(AnnotatedString(formatted), digitOffsetMapping(text.text.length, formatted))
    }
}
