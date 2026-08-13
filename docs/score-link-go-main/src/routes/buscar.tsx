import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { Search, MapPin, Clock, Users } from "lucide-react";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/hooks/useAuth";
import { BottomNav } from "@/components/BottomNav";
import { SPORTS, sportEmoji, type MatchRow } from "@/lib/db";
import { DEFAULT_CENTER, distanceKm, formatDistance, formatWhen } from "@/lib/geo";
import { formatBRLCents } from "@/lib/pix";
import { Input } from "@/components/ui/input";
import { Slider } from "@/components/ui/slider";

export const Route = createFileRoute("/buscar")({
  head: () => ({
    meta: [
      { title: "Buscar partidas com vaga — Vaga" },
      {
        name: "description",
        content: "Filtre partidas de futebol e futsal por distância, esporte e horário e ache uma vaga agora.",
      },
      { property: "og:title", content: "Buscar partidas com vaga — Vaga" },
      { property: "og:description", content: "Filtre por distância, esporte e horário e ache sua vaga." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: SearchPage,
});

function SearchPage() {
  const { profile } = useAuth();
  const [matches, setMatches] = useState<MatchRow[]>([]);
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [q, setQ] = useState("");
  const [sport, setSport] = useState<string | null>(null);
  const [radius, setRadius] = useState(15);
  const [onlyOpen, setOnlyOpen] = useState(true);

  const me = profile?.lat && profile?.lng ? { lat: profile.lat, lng: profile.lng } : DEFAULT_CENTER;

  useEffect(() => {
    void (async () => {
      const { data } = await supabase
        .from("matches")
        .select("*")
        .in("status", ["open", "full"])
        .gte("starts_at", new Date().toISOString())
        .order("starts_at");
      setMatches((data as MatchRow[]) ?? []);
      const { data: parts } = await supabase
        .from("match_participants")
        .select("match_id")
        .eq("status", "confirmed");
      const map: Record<string, number> = {};
      (parts ?? []).forEach((p: { match_id: string }) => {
        map[p.match_id] = (map[p.match_id] ?? 0) + 1;
      });
      setCounts(map);
    })();
  }, []);

  const results = useMemo(() => {
    const term = q.trim().toLowerCase();
    return matches
      .map((m) => ({ ...m, km: distanceKm(me, { lat: m.lat, lng: m.lng }), filled: counts[m.id] ?? 0 }))
      .filter((m) => (!sport || m.sport === sport) && m.km <= radius)
      .filter((m) => (onlyOpen ? m.total_slots - m.filled > 0 : true))
      .filter(
        (m) =>
          !term ||
          m.venue.toLowerCase().includes(term) ||
          (m.neighborhood ?? "").toLowerCase().includes(term),
      )
      .sort((a, b) => a.km - b.km);
  }, [matches, counts, q, sport, radius, onlyOpen, me.lat, me.lng]);

  return (
    <div className="min-h-screen bg-background pb-24">
      <header className="px-5 pt-6">
        <h1 className="text-hard text-2xl font-black uppercase">Buscar vaga</h1>
        <div className="relative mt-4">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Quadra ou bairro"
            className="pl-9"
          />
        </div>
      </header>

      <section className="mx-auto max-w-md px-5 pt-4">
        <div className="flex gap-2 overflow-x-auto pb-1">
          <Chip active={sport === null} onClick={() => setSport(null)}>
            Todos
          </Chip>
          {SPORTS.map((s) => (
            <Chip key={s.id} active={sport === s.id} onClick={() => setSport(s.id)}>
              {s.emoji} {s.label}
            </Chip>
          ))}
          <Chip active={onlyOpen} onClick={() => setOnlyOpen((v) => !v)}>
            Só com vaga
          </Chip>
        </div>

        <div className="card-3d mt-4 rounded-2xl p-4">
          <div className="flex items-center justify-between text-sm font-semibold">
            <span>Distância</span>
            <span className="text-primary">{radius} km</span>
          </div>
          <Slider
            className="mt-3"
            value={[radius]}
            min={1}
            max={50}
            step={1}
            onValueChange={(v) => setRadius(v[0] ?? 15)}
          />
        </div>

        <div className="mt-4 space-y-3">
          {results.length === 0 && (
            <p className="rounded-3xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              Nada encontrado. Aumente a distância ou tire os filtros.
            </p>
          )}
          {results.map((m) => {
            const left = Math.max(m.total_slots - m.filled, 0);
            return (
              <Link
                key={m.id}
                to="/partida/$id"
                params={{ id: m.id }}
                className="card-3d card-3d-press block rounded-3xl p-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
                      {sportEmoji(m.sport)} {m.level}
                    </p>
                    <h3 className="mt-1 truncate text-lg font-bold">{m.venue}</h3>
                    <p className="mt-0.5 flex items-center gap-1 text-xs text-muted-foreground">
                      <MapPin className="size-3.5" /> {m.neighborhood ?? "—"} · {formatDistance(m.km)}
                    </p>
                  </div>
                  <span
                    className={`shrink-0 rounded-full px-3 py-1 text-xs font-bold ${
                      left === 0 ? "bg-muted text-muted-foreground" : "bg-success/15 text-success"
                    }`}
                  >
                    {left === 0 ? "Espera" : `${left} vaga${left > 1 ? "s" : ""}`}
                  </span>
                </div>
                <div className="mt-3 flex items-center justify-between border-t border-border/60 pt-3 text-sm">
                  <span className="flex items-center gap-1.5 font-semibold">
                    <Clock className="size-4 text-primary" /> {formatWhen(m.starts_at)}
                  </span>
                  <span className="flex items-center gap-3">
                    <span className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Users className="size-3.5" /> {m.filled}/{m.total_slots}
                    </span>
                    <span className="font-bold text-primary">{formatBRLCents(m.price_cents)}</span>
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      </section>

      <BottomNav />
    </div>
  );
}

function Chip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`shrink-0 rounded-xl border px-3.5 py-1.5 text-xs font-black uppercase tracking-wide transition-all bevel-top ${
        active
          ? "border-primary/60 action-gradient shadow-glow"
          : "border-border/70 surface-steel text-muted-foreground"
      }`}
    >
      {children}
    </button>
  );
}
