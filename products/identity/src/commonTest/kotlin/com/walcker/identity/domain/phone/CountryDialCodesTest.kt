package com.walcker.identity.domain.phone

import com.walcker.identity.features.domain.phone.countryByIsoCode
import com.walcker.identity.features.domain.phone.countryDialCodes
import com.walcker.identity.features.domain.phone.countryForE164
import com.walcker.identity.features.domain.phone.defaultCountry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountryDialCodesTest {
    @Test
    fun `known countries carry their dial codes`() {
        assertEquals("55", countryByIsoCode("BR")?.dialCode)
        assertEquals("351", countryByIsoCode("PT")?.dialCode)
        assertEquals("1", countryByIsoCode("US")?.dialCode)
        assertEquals("54", countryByIsoCode("AR")?.dialCode)
    }

    @Test
    fun `lookup ignores case and rejects unknown codes`() {
        assertEquals("55", countryByIsoCode("br")?.dialCode)
        assertNull(countryByIsoCode("ZZ"))
        assertNull(countryByIsoCode(null))
    }

    @Test
    fun `iso codes are unique two letter uppercase codes`() {
        val isoCodes = countryDialCodes.map { it.isoCode }

        assertEquals(isoCodes.size, isoCodes.toSet().size)
        assertTrue(isoCodes.all { code -> code.length == 2 && code.all { it in 'A'..'Z' } })
    }

    @Test
    fun `dial codes have one to three digits`() {
        assertTrue(countryDialCodes.all { country -> country.dialCode.length in 1..3 && country.dialCode.all { it.isDigit() } })
    }

    @Test
    fun `country is found from an e164 number by its dial code`() {
        assertEquals("BR", countryForE164("+5511912345678", regionIsoCode = null)?.isoCode)
        assertEquals("PT", countryForE164("+351912345678", regionIsoCode = null)?.isoCode)
        assertEquals("CI", countryForE164("+2250712345678", regionIsoCode = null)?.isoCode)
    }

    @Test
    fun `a shared dial code prefers the device region`() {
        assertEquals("CA", countryForE164("+14165550123", regionIsoCode = "CA")?.isoCode)
        assertEquals("1", countryForE164("+14165550123", regionIsoCode = "BR")?.dialCode)
    }

    @Test
    fun `a number that is not e164 has no country`() {
        assertNull(countryForE164("11912345678", regionIsoCode = null))
        assertNull(countryForE164(null, regionIsoCode = null))
    }

    @Test
    fun `default country follows the device region and falls back to brazil`() {
        assertEquals("PT", defaultCountry("PT").isoCode)
        assertEquals("BR", defaultCountry(null).isoCode)
        assertEquals("BR", defaultCountry("ZZ").isoCode)
    }
}
