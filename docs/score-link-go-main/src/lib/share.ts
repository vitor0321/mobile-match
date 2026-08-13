export function onlyDigits(v: string) {
  return v.replace(/\D+/g, "");
}

/** Normaliza telefone BR para o formato aceito pelo WhatsApp (55 + DDD + número). */
export function waPhone(raw?: string | null) {
  if (!raw) return null;
  let d = onlyDigits(raw);
  if (!d) return null;
  if (d.startsWith("0")) d = d.replace(/^0+/, "");
  if (!d.startsWith("55")) d = `55${d}`;
  return d.length >= 12 ? d : null;
}

export function waLink(text: string, phone?: string | null) {
  const p = waPhone(phone);
  const msg = encodeURIComponent(text);
  return p ? `https://wa.me/${p}?text=${msg}` : `https://wa.me/?text=${msg}`;
}

export function openWhatsApp(text: string, phone?: string | null) {
  window.open(waLink(text, phone), "_blank", "noopener,noreferrer");
}

export function matchShareText(m: {
  venue: string;
  when: string;
  left: number;
  price: string;
  id: string;
}) {
  const url = `${typeof window !== "undefined" ? window.location.origin : ""}/partida/${m.id}`;
  return [
    `⚽ VAGA ABERTA — ${m.venue}`,
    `🕒 ${m.when}`,
    `🎽 ${m.left > 0 ? `${m.left} vaga(s) restante(s)` : "lista de espera"}`,
    `💸 ${m.price}`,
    ``,
    `Confirma sua vaga: ${url}`,
  ].join("\n");
}
