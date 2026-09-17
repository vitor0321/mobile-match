package com.walcker.identity.domain.phone

import com.walcker.identity.features.domain.phone.CountryDialCode
import com.walcker.identity.features.domain.phone.digitsOnly
import com.walcker.identity.features.domain.phone.flagEmoji
import com.walcker.identity.features.domain.phone.formatBrazilianNational
import com.walcker.identity.features.domain.phone.maxNationalDigits
import com.walcker.identity.features.domain.phone.nationalDigits
import com.walcker.identity.features.domain.phone.toE164
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNumberFormatTest {
    private val brazil = CountryDialCode(isoCode = "BR", dialCode = "55")
    private val portugal = CountryDialCode(isoCode = "PT", dialCode = "351")
    private val unitedStates = CountryDialCode(isoCode = "US", dialCode = "1")
    private val ivoryCoast = CountryDialCode(isoCode = "CI", dialCode = "225")

    @Test
    fun `digitsOnly drops every non digit`() {
        assertEquals("11912345678", digitsOnly("(11) 91234-5678"))
        assertEquals("", digitsOnly("abc"))
    }

    @Test
    fun `digitsOnly normalizes non ascii decimal digits`() {
        assertEquals("0123", digitsOnly("\u0660\u0661\u0662\u0663"))
        assertEquals("123", digitsOnly("\uFF11\uFF12\uFF13"))
    }

    @Test
    fun `brazilian mobile and landline become e164`() {
        assertEquals("+5511912345678", toE164(brazil, "(11) 91234-5678"))
        assertEquals("+551112345678", toE164(brazil, "1112345678"))
    }

    @Test
    fun `brazilian number outside ten or eleven digits is rejected`() {
        assertNull(toE164(brazil, "912345678"))
        assertNull(toE164(brazil, "119123456789"))
    }

    @Test
    fun `other countries accept six to fourteen national digits`() {
        assertEquals("+351912345678", toE164(portugal, "912 345 678"))
        assertNull(toE164(portugal, "12345"))
        assertNull(toE164(unitedStates, "123456789012345"))
    }

    @Test
    fun `accepted boundaries pass`() {
        assertEquals("+351123456", toE164(portugal, "123456"))
        assertEquals("+112345678901234", toE164(unitedStates, "12345678901234"))
    }

    @Test
    fun `total length above fifteen digits is rejected`() {
        assertNull(toE164(portugal, "12345678901234"))
    }

    @Test
    fun `leading zero is dropped only for brazil`() {
        assertEquals("+5511912345678", toE164(brazil, "0 11 91234-5678"))
        assertEquals("+2250712345678", toE164(ivoryCoast, "07 12 34 56 78"))
    }

    @Test
    fun `national digits strip brazilian zeros before any cap`() {
        assertEquals("11912345678", nationalDigits(brazil, "011912345678"))
        assertEquals("0712345678", nationalDigits(ivoryCoast, "0712345678"))
    }

    @Test
    fun `national digit cap follows the dial code length`() {
        assertEquals(11, maxNationalDigits(brazil))
        assertEquals(12, maxNationalDigits(portugal))
        assertEquals(14, maxNationalDigits(unitedStates))
    }

    @Test
    fun `a pasted number with the selected dial code keeps only the national part`() {
        assertEquals("11912345678", nationalDigits(brazil, "+55 11 91234-5678"))
        assertEquals("912345678", nationalDigits(portugal, " +351 912 345 678"))
    }

    @Test
    fun `dial code is kept when there is no plus or it belongs to another country`() {
        assertEquals("5511912345678", nationalDigits(brazil, "5511912345678"))
        assertEquals("351912345678", nationalDigits(brazil, "+351912345678"))
    }

    @Test
    fun `brazilian mask grows as digits are typed`() {
        assertEquals("", formatBrazilianNational(""))
        assertEquals("(1", formatBrazilianNational("1"))
        assertEquals("(11", formatBrazilianNational("11"))
        assertEquals("(11) 9", formatBrazilianNational("119"))
        assertEquals("(11) 9123-4", formatBrazilianNational("1191234"))
        assertEquals("(11) 1234-5678", formatBrazilianNational("1112345678"))
        assertEquals("(11) 91234-5678", formatBrazilianNational("11912345678"))
    }

    @Test
    fun `brazilian mask ignores extra digits`() {
        assertEquals("(11) 91234-5678", formatBrazilianNational("119123456789"))
    }

    @Test
    fun `flag emoji is built from regional indicator symbols`() {
        assertEquals("🇧🇷", flagEmoji("BR"))
        assertEquals("🇵🇹", flagEmoji("pt"))
    }

    @Test
    fun `invalid iso code has no flag`() {
        assertEquals("", flagEmoji("BRA"))
        assertEquals("", flagEmoji("1A"))
    }
}
