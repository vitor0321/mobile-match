export const DEFAULT_CENTER = { lat: -23.5505, lng: -46.6333 }; // fallback: SP

export function distanceKm(
  a: { lat: number; lng: number },
  b: { lat: number; lng: number },
) {
  const R = 6371;
  const dLat = ((b.lat - a.lat) * Math.PI) / 180;
  const dLng = ((b.lng - a.lng) * Math.PI) / 180;
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((a.lat * Math.PI) / 180) *
      Math.cos((b.lat * Math.PI) / 180) *
      Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s));
}

export function formatDistance(km: number) {
  return km < 1 ? `${Math.round(km * 1000)} m` : `${km.toFixed(1)} km`;
}

export function formatWhen(iso: string) {
  const d = new Date(iso);
  const today = new Date();
  const tomorrow = new Date(today.getTime() + 86400000);
  const same = (a: Date, b: Date) => a.toDateString() === b.toDateString();
  const time = d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  if (same(d, today)) return `Hoje · ${time}`;
  if (same(d, tomorrow)) return `Amanhã · ${time}`;
  return `${d.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" })} · ${time}`;
}
