package com.walcker.identity.ui.verification

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.walcker.identity.features.ui.verification.phone.BrazilianPhoneVisualTransformation
import kotlin.test.Test
import kotlin.test.assertEquals

class BrazilianPhoneVisualTransformationTest {
    private val fullNumber = "11912345678"

    private fun transform(digits: String): TransformedText = BrazilianPhoneVisualTransformation.filter(AnnotatedString(digits))

    @Test
    fun formatsTheTextProgressivelyWhileTyping() {
        assertEquals("", transform("").text.text)
        assertEquals("(1", transform("1").text.text)
        assertEquals("(11) 9", transform("119").text.text)
        assertEquals("(11) 91234-5678", transform(fullNumber).text.text)
    }

    @Test
    fun mapsEachDigitOffsetToItsPositionInTheMask() {
        val mapping = transform(fullNumber).offsetMapping

        assertEquals(1, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(1))
        assertEquals(5, mapping.originalToTransformed(2))
        assertEquals(11, mapping.originalToTransformed(7))
        assertEquals(15, mapping.originalToTransformed(11))
    }

    @Test
    fun placesTheCursorAtTheEndAfterTheLastDigit() {
        assertEquals(2, transform("1").offsetMapping.originalToTransformed(1))
        assertEquals(0, transform("").offsetMapping.originalToTransformed(0))
    }

    @Test
    fun mapsMaskPositionsBackToDigitOffsets() {
        val mapping = transform(fullNumber).offsetMapping

        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(0, mapping.transformedToOriginal(1))
        assertEquals(1, mapping.transformedToOriginal(2))
        assertEquals(2, mapping.transformedToOriginal(5))
        assertEquals(7, mapping.transformedToOriginal(11))
        assertEquals(11, mapping.transformedToOriginal(15))
    }

    @Test
    fun roundTripsEveryDigitOffset() {
        val mapping = transform(fullNumber).offsetMapping

        for (offset in 0..fullNumber.length) {
            assertEquals(offset, mapping.transformedToOriginal(mapping.originalToTransformed(offset)))
        }
    }
}
