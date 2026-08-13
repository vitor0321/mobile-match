import { createServerFn } from "@tanstack/react-start";
import { requireSupabaseAuth } from "@/integrations/supabase/auth-middleware";

export type NearbyPlayer = {
  id: string;
  full_name: string;
  phone: string | null;
  km: number;
};

function haversine(aLat: number, aLng: number, bLat: number, bLng: number) {
  const toRad = (v: number) => (v * Math.PI) / 180;
  const dLat = toRad(bLat - aLat);
  const dLng = toRad(bLng - aLng);
  const s =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(aLat)) * Math.cos(toRad(bLat)) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.min(1, Math.sqrt(s)));
}

/**
 * Lista jogadores disponíveis perto da partida, com telefone, para o organizador
 * disparar o convite no WhatsApp. Só o dono da partida pode chamar.
 */
export const nearbyAvailablePlayers = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((data: { matchId: string }) => data)
  .handler(async ({ data, context }): Promise<NearbyPlayer[]> => {
    const { supabase, userId } = context;

    const { data: match } = await supabase
      .from("matches")
      .select("id,organizer_id,lat,lng")
      .eq("id", data.matchId)
      .maybeSingle();

    if (!match || match.organizer_id !== userId) return [];

    const { supabaseAdmin } = await import("@/integrations/supabase/client.server");
    const { data: rows } = await supabaseAdmin
      .from("profiles")
      .select("id,full_name,phone,lat,lng,radius_km,is_available,is_banned")
      .eq("is_available", true)
      .eq("is_banned", false)
      .limit(300);

    return ((rows ?? []) as Array<{
      id: string;
      full_name: string;
      phone: string | null;
      lat: number | null;
      lng: number | null;
      radius_km: number;
    }>)
      .filter((p) => p.id !== userId && p.lat != null && p.lng != null)
      .map((p) => ({
        id: p.id,
        full_name: p.full_name || "Jogador",
        phone: p.phone,
        km: haversine(p.lat!, p.lng!, match.lat, match.lng),
      }))
      .filter((p) => p.km <= 30)
      .sort((a, b) => a.km - b.km)
      .slice(0, 60);
  });
