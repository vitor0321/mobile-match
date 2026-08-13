// Gerador de Pix "Copia e Cola" (BR Code / EMV-QRCPS-MPM) — 100% local, sem gateway.

function tlv(id: string, value: string) {
  const len = value.length.toString().padStart(2, "0");
  return `${id}${len}${value}`;
}

function crc16(payload: string) {
  let crc = 0xffff;
  for (let i = 0; i < payload.length; i++) {
    crc ^= payload.charCodeAt(i) << 8;
    for (let j = 0; j < 8; j++) {
      crc = crc & 0x8000 ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff;
    }
  }
  return crc.toString(16).toUpperCase().padStart(4, "0");
}

function sanitize(text: string, max: number) {
  return text
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^A-Za-z0-9 ]/g, "")
    .trim()
    .slice(0, max)
    .toUpperCase();
}

export function buildPixPayload({
  key,
  amount,
  merchant,
  city = "SAO PAULO",
  txid = "***",
  description,
}: {
  key: string;
  amount: number;
  merchant: string;
  city?: string;
  txid?: string;
  description?: string;
}) {
  const merchantAccount =
    tlv("00", "br.gov.bcb.pix") +
    tlv("01", key.trim()) +
    (description ? tlv("02", sanitize(description, 40)) : "");

  const payload =
    tlv("00", "01") +
    tlv("26", merchantAccount) +
    tlv("52", "0000") +
    tlv("53", "986") +
    (amount > 0 ? tlv("54", amount.toFixed(2)) : "") +
    tlv("58", "BR") +
    tlv("59", sanitize(merchant || "RECEBEDOR", 25) || "RECEBEDOR") +
    tlv("60", sanitize(city, 15) || "SAO PAULO") +
    tlv("62", tlv("05", sanitize(txid, 25) || "***"));

  const withCrcId = `${payload}6304`;
  return `${withCrcId}${crc16(withCrcId)}`;
}

export function qrImageUrl(payload: string, size = 260) {
  return `https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&margin=0&data=${encodeURIComponent(payload)}`;
}

export function formatBRLCents(cents: number) {
  return (cents / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}
