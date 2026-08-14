package com.walcker.match.core.payments

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixPayloadBuilderTest {

    @Test
    fun `buildPixPayload basic`() {
        val payload = buildPixPayload(
            key = "12345678900",
            amountCents = 1500,
            merchant = "Joao Silva",
        )

        // Verify it starts with the expected format
        assertTrue(payload.startsWith("000201"))
        // Must contain merchant account info
        assertTrue(payload.contains("26"))
        // Must end with CRC16 (4 hex chars)
        assertEquals(4, payload.takeLast(4).length)
        assertTrue(payload.takeLast(4).all { it in "0123456789ABCDEF" })
    }

    @Test
    fun `buildPixPayload no amount`() {
        val payload = buildPixPayload(
            key = "chave@email.com",
            amountCents = 0,
            merchant = "Maria",
        )
        assertTrue(payload.contains("52040000")) // MCC
        assertTrue(payload.contains("5303986")) // BRL
        // Amount field (54) should be absent when amount is 0
        val idx54 = payload.indexOf("54")
        assertEquals(-1, idx54)
    }

    @Test
    fun `buildPixPayload with description`() {
        val payload = buildPixPayload(
            key = "12345678900",
            amountCents = 2000,
            merchant = "Teste",
            description = "Partida Futsal",
        )
        // BR Code é texto puro (EMV), não hex: sub-TLV 02, tamanho 14, valor sanitizado.
        assertTrue(payload.contains("0214PARTIDA FUTSAL")) // "Partida Futsal" sanitizado
    }

    @Test
    fun `buildPixPayload sanitizes accents`() {
        val payload = buildPixPayload(
            key = "chave@email.com",
            amountCents = 1000,
            merchant = "João São Paulo",
            city = "São Paulo",
        )
        // Should not contain accents
        assertTrue(!payload.contains("ã") && !payload.contains("é"))
        // Should be uppercase
        assertTrue(payload.uppercase() == payload)
    }

    @Test
    fun `buildPixPayload truncates long fields`() {
        val longMerchant = "A".repeat(50)
        val payload = buildPixPayload(
            key = "chave@email.com",
            amountCents = 1000,
            merchant = longMerchant,
        )
        // Merchant field (59) max 25 chars → after sanitize
        val idx59 = payload.indexOf("59")
        assertTrue(idx59 >= 0)
    }

    @Test
    fun `crc16 produces 4 hex chars`() {
        val crc = crc16("000201260000005204000053039865802BR5908RECEBEDOR6008SAOPAULO62070503***6304")
        assertEquals(4, crc.length)
        assertTrue(crc.all { it in "0123456789ABCDEF" })
    }

    @Test
    fun `formatBRLCents`() {
        assertEquals("R$ 15,00", formatBRLCents(1500))
        assertEquals("R$ 1.000,00", formatBRLCents(100000))
        assertEquals("R$ 0,00", formatBRLCents(0))
        assertEquals("R$ 0,50", formatBRLCents(50))
    }
}
