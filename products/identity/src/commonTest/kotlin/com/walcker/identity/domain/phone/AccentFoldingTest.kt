package com.walcker.identity.domain.phone

import com.walcker.identity.features.domain.phone.foldAccents
import kotlin.test.Test
import kotlin.test.assertEquals

class AccentFoldingTest {
    @Test
    fun `accents are folded to their ascii letters`() {
        assertEquals("austria", foldAccents("Áustria"))
        assertEquals("japao", foldAccents("Japão"))
        assertEquals("cote d'ivoire", foldAccents("Côte d'Ivoire"))
        assertEquals("africa do sul", foldAccents("África do Sul"))
        assertEquals("monaco", foldAccents("Mônaco"))
        assertEquals("nigeria", foldAccents("Nigéria"))
    }

    @Test
    fun `ligatures and sharp s expand to ascii pairs`() {
        assertEquals("aegean", foldAccents("Ægean"))
        assertEquals("strasse", foldAccents("Straße"))
    }

    @Test
    fun `a name without accents only loses its case`() {
        assertEquals("brasil", foldAccents("brasil"))
        assertEquals("brasil", foldAccents("Brasil"))
    }

    @Test
    fun `sorting by the folded name ignores the accent`() {
        val names = listOf("Zimbábue", "Bélgica", "Áustria", "Índia", "Nigéria")

        assertEquals(
            listOf("Áustria", "Bélgica", "Índia", "Nigéria", "Zimbábue"),
            names.sortedBy { name -> foldAccents(name) },
        )
    }
}
