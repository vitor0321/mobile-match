package com.walcker.match.core.payments

/**
 * 100% local BR Code Pix generator (EMV-QRCPS-MPM). No gateway, no third-party
 * API calls — the QR will be rendered on-device in Phase 5.
 *
 * Ported 1:1 from `lib/pix.ts` in the Lovable MVP. The generated payload is
 * identical and can be copy-pasted into any banking app.
 */
public fun buildPixPayload(
    key: String,
    amountCents: Int,
    merchant: String,
    city: String = "SAO PAULO",
    txid: String = "***",
    description: String? = null,
): String {
    val merchantAccount =
        tlv("00", "br.gov.bcb.pix".uppercase()) +
            tlv("01", key.trim().uppercase()) +
            (description?.let { tlv("02", sanitize(it, 40)) } ?: "")

    val payload =
        tlv("00", "01") +
            tlv("26", merchantAccount) +
            tlv("52", "0000") +
            tlv("53", "986") +
            (if (amountCents > 0) tlv("54", formatAmount(amountCents)) else "") +
            tlv("58", "BR") +
            tlv("59", sanitize(merchant.ifBlank { "RECEBEDOR" }, 25).ifBlank { "RECEBEDOR" }) +
            tlv("60", sanitize(city, 15).ifBlank { "SAO PAULO" }) +
            tlv("62", tlv("05", sanitize(txid, 25).ifBlank { "***" }))

    val withCrcId = "${payload}6304"
    return "${withCrcId}${crc16(withCrcId)}"
}

/**
 * Formats amount in cents as "XX.XX" string for the Pix payload.
 * e.g. 1500 → "15.00", 500 → "5.00"
 */
private fun formatAmount(cents: Int): String {
    val reais = cents / 100
    val centavos = cents % 100
    return "$reais.${centavos.toString().padStart(2, '0')}"
}

/**
 * EMV TLV encoding: ID (2 chars) + length (2 chars, zero-padded) + value.
 */
private fun tlv(id: String, value: String): String {
    val len = value.length.toString().padStart(2, '0')
    return "$id$len$value"
}

/**
 * CRC-16/CCITT (polynomial 0x1021, initial value 0xFFFF).
 * Used by the BR Code to compute the checksum at the end of the payload.
 */
internal fun crc16(payload: String): String {
    var crc = 0xFFFF
    for (ch in payload) {
        crc = crc xor (ch.code shl 8)
        repeat(8) {
            crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) and 0xFFFF
            else (crc shl 1) and 0xFFFF
        }
    }
    return crc.toString(16).uppercase().padStart(4, '0')
}

/**
 * Normalizes text for the Pix payload: strips diacritics, removes special
 * characters, uppercases, and truncates to [max] characters.
 */
internal fun sanitize(text: String, max: Int): String {
    return text
        .normalizeToNFD()
        .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        .replace(Regex("[^A-Za-z0-9 ]"), "")
        .trim()
        .take(max)
        .uppercase()
}

internal expect fun String.normalizeToNFD(): String
