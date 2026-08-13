import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { MapPin, Navigation, Zap, Users, Clock, LogIn, Radar, Share2 } from "lucide-react";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/hooks/useAuth";
import { BottomNav } from "@/components/BottomNav";
import { MapView, type MapPin as Pin } from "@/components/MapView";
import { NotificationBell, askNotificationPermission } from "@/components/NotificationBell";
import { SPORTS, sportEmoji, type MatchRow } from "@/lib/db";
import { DEFAULT_CENTER, distanceKm, formatDistance, formatWhen } from "@/lib/geo";
import { formatBRLCents } from "@/lib/pix";
import { matchShareText, openWhatsApp } from "@/lib/share";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Vaga — encontre jogos com vaga aberta perto de você" },
      {
        name: "description",
        content:
          "Veja no mapa as partidas de futebol e futsal com vagas abertas na sua região, confirme presença e pague no Pix.",
      },
      { property: "og:title", content: "Vaga — jogos com vaga aberta perto de você" },
      {
        property: "og:description",
        content: "Mapa em tempo real das partidas com vagas, lista de espera e pagamento no Pix.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: HomePage,
});

function HomePage() {
  const { user, profile, setProfile } = useAuth();
  const navigate = useNavigate();
  const [matches, setMatches] = useState<MatchRow[]>([]);
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [me, setMe] = useState(DEFAULT_CENTER);
  const [sport, setSport] = useState<string | null>(null);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);
  const [radius, setRadius] = useState(15);
  const [availableCount, setAvailableCount] = useState(0);


  useEffect(() => {
    void load();
    const channel = supabase
      .channel("home-matches")
      .on("postgres_changes", { event: "*", schema: "public", table: "matches" }, () => void load())
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "match_participants" },
        () => void load(),
      )
      .subscribe();
    return () => {
      supabase.removeChannel(channel);
    };
  }, []);

  useEffect(() => {
    if (profile?.lat && profile?.lng) setMe({ lat: profile.lat, lng: profile.lng });
    if (profile?.radius_km) setRadius(profile.radius_km);
  }, [profile?.lat, profile?.lng, profile?.radius_km]);

  async function load() {
    const { data } = await supabase
      .from("matches")
      .select("*")
      .eq("status", "open")
      .gte("starts_at", new Date().toISOString())
      .order("starts_at");
    const rows = (data as MatchRow[]) ?? [];
    setMatches(rows);

    const { data: parts } = await supabase
      .from("match_participants")
      .select("match_id,status")
      .eq("status", "confirmed");
    const map: Record<string, number> = {};
    (parts ?? []).forEach((p: { match_id: string }) => {
      map[p.match_id] = (map[p.match_id] ?? 0) + 1;
    });
    setCounts(map);

    const { count } = await supabase
      .from("profiles")
      .select("id", { count: "exact", head: true })
      .eq("is_available", true);
    setAvailableCount(count ?? 0);
  }


  async function changeRadius(next: number) {
    setRadius(next);
    if (!user) return;
    await supabase.from("profiles").update({ radius_km: next }).eq("id", user.id);
    setProfile((p) => (p ? { ...p, radius_km: next } : p));
  }

  function locate() {
    if (!navigator.geolocation) {
      toast.error("Localização não disponível");
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const coords = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        setMe(coords);
        setLocating(false);
        if (user) {
          await supabase.from("profiles").update(coords).eq("id", user.id);
          setProfile((p) => (p ? { ...p, ...coords } : p));
        }
        toast.success("Localização atualizada");
      },
      () => {
        setLocating(false);
        toast.error("Não consegui pegar sua localização");
      },
      { enableHighAccuracy: true, timeout: 8000 },
    );
  }

  async function toggleAvailable(next: boolean) {
    if (!user) {
      void navigate({ to: "/auth" });
      return;
    }
    if (next) await askNotificationPermission();
    await supabase
      .from("profiles")
      .update({
        is_available: next,
        available_until: next ? new Date(Date.now() + 6 * 3600_000).toISOString() : null,
        lat: me.lat,
        lng: me.lng,
        radius_km: radius,
      })
      .eq("id", user.id);
    setProfile((p) => (p ? { ...p, is_available: next } : p));
    toast[next ? "success" : "message"](
      next ? "Disponível! Você será avisado de vagas perto de você." : "Disponibilidade desligada",
    );
  }

  const list = useMemo(() => {
    return matches
      .filter((m) => !sport || m.sport === sport)
      .map((m) => ({
        ...m,
        km: distanceKm(me, { lat: m.lat, lng: m.lng }),
        filled: counts[m.id] ?? 0,
      }))
      .filter((m) => m.km <= radius)
      .sort((a, b) => a.km - b.km);
  }, [matches, counts, me, sport, radius]);

  const openSlots = list.reduce((acc, m) => acc + Math.max(m.total_slots - m.filled, 0), 0);

  const pins: Pin[] = list.map((m) => ({
    id: m.id,
    lat: m.lat,
    lng: m.lng,
    label: `${sportEmoji(m.sport)} ${Math.max(m.total_slots - m.filled, 0)} vagas`,
    sub: m.venue,
    full: m.total_slots - m.filled <= 0,
  }));

  return (
    <div className="min-h-screen bg-background pb-24">
      <header className="sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-border/60 bg-background/85 px-4 py-3 backdrop-blur-xl">
        <div>
          <p className="flex items-center gap-1.5 text-[0.62rem] font-black uppercase tracking-[0.34em] text-primary">
            <span className="animate-live inline-block size-1.5 rounded-full bg-primary" /> Vaga · ao vivo
          </p>
          <h1 className="text-hard text-xl font-black uppercase leading-tight">Jogos perto de você</h1>
        </div>
        <div className="flex items-center gap-2">
          <NotificationBell userId={user?.id ?? null} />
          {!user && (
            <Link to="/auth">
              <Button size="sm" variant="secondary">
                <LogIn className="size-4" /> Entrar
              </Button>
            </Link>
          )}
        </div>
      </header>

      <section className="scene-3d relative overflow-hidden">
        <MapView
          center={me}
          pins={pins}
          radiusKm={radius}
          activeId={activeId}
          onSelect={setActiveId}
          className="h-[38vh] min-h-[240px] w-full"
        />
        <div className="pointer-events-none absolute left-4 top-3 z-[500] flex items-center gap-2 rounded-xl border border-border/70 bg-background/70 px-3 py-1.5 text-[0.62rem] font-black uppercase tracking-[0.18em] backdrop-blur">
          <Radar className="size-3.5 text-primary" />
          {openSlots} vagas · raio {radius} km
        </div>

        {/* filtros direto no mapa */}
        <div className="absolute inset-x-0 top-12 z-[500] flex gap-2 overflow-x-auto px-4 pb-1">
          <Chip active={sport === null} onClick={() => setSport(null)}>
            Todos
          </Chip>
          {SPORTS.map((s) => (
            <Chip key={s.id} active={sport === s.id} onClick={() => setSport(s.id)}>
              {s.emoji} {s.label}
            </Chip>
          ))}
        </div>

        <button
          onClick={locate}
          className="card-3d card-3d-press absolute right-4 top-12 z-[500] grid size-11 place-items-center rounded-2xl"
          aria-label="Usar minha localização"
        >
          <Navigation className={`size-5 ${locating ? "animate-pulse text-primary" : "text-primary"}`} />
        </button>

        {/* carrossel ao vivo estilo Uber */}
        {list.length > 0 && (
          <div className="absolute inset-x-0 bottom-2 z-[500] flex gap-3 overflow-x-auto px-4 pb-1">
            {list.slice(0, 8).map((m) => {
              const left = Math.max(m.total_slots - m.filled, 0);
              return (
                <Link
                  key={m.id}
                  to="/partida/$id"
                  params={{ id: m.id }}
                  onMouseEnter={() => setActiveId(m.id)}
                  onFocus={() => setActiveId(m.id)}
                  className={`card-3d card-3d-press w-[62%] shrink-0 rounded-2xl p-3 ${
                    activeId === m.id ? "border-primary" : ""
                  }`}
                >
                  <p className="text-[0.55rem] font-black uppercase tracking-[0.24em] text-muted-foreground">
                    {sportEmoji(m.sport)} {formatDistance(m.km)} · {formatWhen(m.starts_at)}
                  </p>
                  <p className="text-hard mt-1 truncate text-sm font-black">{m.venue}</p>
                  <div className="mt-2 flex items-center justify-between">
                    <span
                      className={`rounded-lg px-2 py-0.5 text-[0.6rem] font-black uppercase bevel-top ${
                        left === 0 ? "bg-muted text-muted-foreground" : "action-gradient shadow-glow"
                      }`}
                    >
                      {left === 0 ? "Espera" : `${left} vaga${left > 1 ? "s" : ""}`}
                    </span>
                    <span className="text-xs font-black text-primary">
                      {formatBRLCents(m.price_cents)}
                    </span>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </section>

      <section className="mx-auto mt-3 grid max-w-md grid-cols-3 gap-2 px-4">
        <LiveStat value={list.length} label="Partidas" />
        <LiveStat value={openSlots} label="Vagas" accent />
        <LiveStat value={availableCount} label="Online" />
      </section>


      <section className="mx-auto mt-3 max-w-md rounded-t-[2rem] border-t border-border/70 bg-background px-4 pt-4 shadow-3d">
        <div className="card-3d flex items-center justify-between rounded-2xl p-3.5">
          <div className="flex items-center gap-2.5">
            <span className="grid size-10 place-items-center rounded-xl action-gradient shadow-glow">
              <Zap className="size-5" />
            </span>
            <div className="min-w-0">
              <p className="text-sm font-black uppercase tracking-tight">Estou disponível</p>
              <p className="text-xs text-muted-foreground">Aviso em tempo real quando abrir vaga</p>
            </div>
          </div>
          <Switch checked={!!profile?.is_available} onCheckedChange={toggleAvailable} />
        </div>

        <div className="card-3d mt-3 rounded-2xl p-3.5">
          <div className="flex items-center justify-between text-xs font-black uppercase tracking-[0.16em]">
            <span className="text-muted-foreground">Raio de busca</span>
            <span className="text-primary">{radius} km</span>
          </div>
          <Slider
            className="mt-3"
            value={[radius]}
            min={1}
            max={50}
            step={1}
            onValueChange={(v) => void changeRadius(v[0] ?? 15)}
          />
        </div>


        <div className="mt-4 space-y-3">
          {list.length === 0 && (
            <div className="rounded-3xl border border-dashed border-border p-6 text-center">
              <p className="text-sm text-muted-foreground">
                Nenhuma partida aberta nesse raio. Aumente a distância ou crie a sua.
              </p>
              <Link to="/criar">
                <Button className="mt-3">Criar partida</Button>
              </Link>
            </div>
          )}
          {list.map((m) => {
            const left = Math.max(m.total_slots - m.filled, 0);
            return (
              <div
                key={m.id}
                onMouseEnter={() => setActiveId(m.id)}
                className={`card-3d card-3d-press relative overflow-hidden rounded-3xl p-4 ${
                  activeId === m.id ? "border-primary" : ""
                }`}
              >
                {left > 0 && <div className="stripe-alert absolute inset-x-0 top-0 h-1" />}
                <Link to="/partida/$id" params={{ id: m.id }} className="block">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-[0.62rem] font-black uppercase tracking-[0.24em] text-muted-foreground">
                        {sportEmoji(m.sport)} {m.level}
                      </p>
                      <h3 className="text-hard mt-1 truncate text-lg font-black">{m.venue}</h3>
                      <p className="mt-0.5 flex items-center gap-1 text-xs text-muted-foreground">
                        <MapPin className="size-3.5" /> {m.neighborhood ?? "—"} · {formatDistance(m.km)}
                      </p>
                    </div>
                    <span
                      className={`shrink-0 rounded-xl px-3 py-1 text-xs font-black uppercase bevel-top ${
                        left === 0
                          ? "bg-muted text-muted-foreground"
                          : "action-gradient shadow-glow"
                      }`}
                    >
                      {left === 0 ? "Espera" : `${left} vaga${left > 1 ? "s" : ""}`}
                    </span>
                  </div>
                  <div className="mt-3 flex items-center justify-between border-t border-border/60 pt-3 text-sm">
                    <span className="flex items-center gap-1.5 font-bold">
                      <Clock className="size-4 text-primary" /> {formatWhen(m.starts_at)}
                    </span>
                    <span className="flex items-center gap-3">
                      <span className="flex items-center gap-1 text-xs text-muted-foreground">
                        <Users className="size-3.5" /> {m.filled}/{m.total_slots}
                      </span>
                      <span className="font-black text-primary">{formatBRLCents(m.price_cents)}</span>
                    </span>
                  </div>
                </Link>
                <button
                  onClick={() =>
                    openWhatsApp(
                      matchShareText({
                        venue: m.venue,
                        when: formatWhen(m.starts_at),
                        left,
                        price: formatBRLCents(m.price_cents),
                        id: m.id,
                      }),
                    )
                  }
                  className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl border border-border/70 py-2 text-xs font-black uppercase tracking-[0.16em] text-muted-foreground"
                >
                  <Share2 className="size-3.5" /> Chamar no WhatsApp
                </button>
              </div>
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

function LiveStat({ value, label, accent }: { value: number; label: string; accent?: boolean }) {
  return (
    <div className="card-3d rounded-2xl px-2 py-2.5 text-center">
      <p className={`text-hard text-xl font-black leading-none ${accent ? "text-primary" : ""}`}>
        {value}
      </p>
      <p className="mt-1 text-[0.55rem] font-black uppercase tracking-[0.22em] text-muted-foreground">
        {label}
      </p>
    </div>
  );
}
