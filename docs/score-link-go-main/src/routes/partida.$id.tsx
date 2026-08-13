import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { ArrowLeft, Clock, MapPin, Users, Flag, Star, MessageCircle, Share2 } from "lucide-react";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/hooks/useAuth";
import { MapView } from "@/components/MapView";
import { PixDialog } from "@/components/PixDialog";
import { sportEmoji, type MatchRow, type ParticipantRow } from "@/lib/db";
import { formatWhen } from "@/lib/geo";
import { formatBRLCents } from "@/lib/pix";
import { matchShareText, openWhatsApp } from "@/lib/share";
import { Button } from "@/components/ui/button";
import { WhatsAppBroadcast } from "@/components/WhatsAppBroadcast";


export const Route = createFileRoute("/partida/$id")({
  validateSearch: (search: Record<string, unknown>): { wa?: boolean } =>
    search["wa"] === "1" || search["wa"] === true ? { wa: true } : {},
  head: () => ({
    meta: [
      { title: "Detalhes da partida — Vaga" },
      {
        name: "description",
        content: "Veja horário, local, vagas restantes e confirme sua presença na partida com pagamento via Pix.",
      },
      { property: "og:title", content: "Detalhes da partida — Vaga" },
      { property: "og:description", content: "Confirme presença, entre na lista de espera e pague no Pix." },
      { property: "og:type", content: "article" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: MatchPage,
});

type PlayerRow = ParticipantRow & { profiles: { full_name: string; rating: number } | null };

function MatchPage() {
  const { id } = Route.useParams();
  const { wa } = Route.useSearch();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [match, setMatch] = useState<MatchRow | null>(null);
  const [organizer, setOrganizer] = useState<{
    full_name: string;
    rating: number;
    phone: string | null;
  } | null>(null);
  const [players, setPlayers] = useState<PlayerRow[]>([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void load();
    const channel = supabase
      .channel(`match-${id}`)
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "match_participants", filter: `match_id=eq.${id}` },
        () => void load(),
      )
      .subscribe();
    return () => {
      supabase.removeChannel(channel);
    };
  }, [id]);

  async function load() {
    const { data: m } = await supabase.from("matches").select("*").eq("id", id).maybeSingle();
    setMatch((m as MatchRow) ?? null);
    if (m) {
      const { data: org } = await supabase
        .from("profiles")
        .select("full_name,rating,phone")
        .eq("id", (m as MatchRow).organizer_id)
        .maybeSingle();
      setOrganizer(org as { full_name: string; rating: number; phone: string | null } | null);
    }
    const { data: p } = await supabase
      .from("match_participants")
      .select("*")
      .eq("match_id", id)
      .neq("status", "cancelled")
      .order("created_at");
    const rows = (p as ParticipantRow[]) ?? [];
    const ids = rows.map((r) => r.user_id);
    const { data: profs } = ids.length
      ? await supabase.from("profiles").select("id,full_name,rating").in("id", ids)
      : { data: [] };
    const byId = new Map(
      ((profs as { id: string; full_name: string; rating: number }[]) ?? []).map((x) => [x.id, x]),
    );
    setPlayers(rows.map((r) => ({ ...r, profiles: byId.get(r.user_id) ?? null })));
  }

  if (!match) {
    return (
      <div className="grid min-h-screen place-items-center text-sm text-muted-foreground">Carregando…</div>
    );
  }

  const confirmed = players.filter((p) => p.status === "confirmed");
  const waitlist = players.filter((p) => p.status === "waitlist");
  const left = Math.max(match.total_slots - confirmed.length, 0);
  const mine = players.find((p) => p.user_id === user?.id);
  const isOrganizer = user?.id === match.organizer_id;
  const shareMsg = matchShareText({
    venue: match.venue,
    when: formatWhen(match.starts_at),
    left,
    price: formatBRLCents(match.price_cents),
    id: match.id,
  });


  async function join() {
    if (!user) {
      void navigate({ to: "/auth" });
      return;
    }
    setBusy(true);
    const status = left > 0 ? "confirmed" : "waitlist";
    const { error } = await supabase
      .from("match_participants")
      .upsert({ match_id: id, user_id: user.id, status }, { onConflict: "match_id,user_id" });
    setBusy(false);
    if (error) {
      toast.error(error.message);
      return;
    }
    toast.success(status === "confirmed" ? "Vaga confirmada!" : "Você entrou na lista de espera");
    void load();
  }

  async function leave() {
    if (!mine) return;
    setBusy(true);
    await supabase.from("match_participants").update({ status: "cancelled" }).eq("id", mine.id);
    setBusy(false);
    toast.message("Você saiu da partida");
    void load();
  }

  async function payPix() {
    if (!user || !mine) return;
    await supabase.from("payments").insert({
      user_id: user.id,
      match_id: id,
      kind: "match_fee",
      amount_cents: match!.price_cents,
      pix_key: match!.pix_key,
      status: "pending",
    });
    await supabase.from("match_participants").update({ payment_status: "paid" }).eq("id", mine.id);
    toast.success("Pagamento registrado. O organizador vai confirmar.");
    void load();
  }

  async function report() {
    if (!user) {
      void navigate({ to: "/auth" });
      return;
    }
    await supabase.from("reports").insert({
      reporter_id: user.id,
      reported_user_id: match!.organizer_id,
      match_id: id,
      reason: "denuncia_partida",
    });
    toast.success("Denúncia enviada para moderação");
  }

  return (
    <div className="min-h-screen bg-background pb-32">
      <MapView
        center={{ lat: match.lat, lng: match.lng }}
        pins={[
          {
            id: match.id,
            lat: match.lat,
            lng: match.lng,
            label: match.venue,
            sub: "",
            full: left === 0,
          },
        ]}
        className="h-56 w-full"
      />
      <Link
        to="/"
        className="absolute left-4 top-4 z-[500] grid size-10 place-items-center rounded-full bg-card shadow-lift"
      >
        <ArrowLeft className="size-5" />
      </Link>

      <main className="mx-auto -mt-6 max-w-md rounded-t-3xl bg-background px-5 pt-5">
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
          {sportEmoji(match.sport)} {match.level}
        </p>
        <h1 className="text-hard mt-1 text-2xl font-black uppercase">{match.venue}</h1>
        <p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground">
          <MapPin className="size-4" /> {match.address ?? match.neighborhood ?? "Local combinado"}
        </p>

        <div className="mt-4 grid grid-cols-3 gap-2 text-center">
          <Stat icon={<Clock className="size-4" />} label={formatWhen(match.starts_at)} />
          <Stat icon={<Users className="size-4" />} label={`${confirmed.length}/${match.total_slots}`} />
          <Stat label={formatBRLCents(match.price_cents)} />
        </div>

        {organizer && (
          <div className="card-3d mt-4 flex items-center justify-between rounded-2xl p-3">
            <div>
              <p className="text-xs text-muted-foreground">Organizador</p>
              <p className="text-sm font-bold">{organizer.full_name || "Organizador"}</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="flex items-center gap-1 text-sm font-semibold">
                <Star className="size-4 fill-warning text-warning" /> {Number(organizer.rating).toFixed(1)}
              </span>
              {organizer.phone && (
                <button
                  onClick={() =>
                    openWhatsApp(
                      `Opa! Vi a partida no ${match!.venue} (${formatWhen(match!.starts_at)}) e quero falar sobre a vaga.`,
                      organizer.phone,
                    )
                  }
                  className="card-3d card-3d-press grid size-9 place-items-center rounded-xl text-primary"
                  aria-label="Falar com o organizador no WhatsApp"
                >
                  <MessageCircle className="size-4" />
                </button>
              )}
            </div>
          </div>
        )}

        {match.rules.length > 0 && (
          <section className="mt-5">
            <h2 className="text-sm font-bold">Regras</h2>
            <ul className="mt-2 space-y-1 text-sm text-muted-foreground">
              {match.rules.map((r) => (
                <li key={r}>• {r}</li>
              ))}
            </ul>
          </section>
        )}

        <section className="mt-5">
          <h2 className="text-sm font-bold">Confirmados ({confirmed.length})</h2>
          <ul className="mt-2 space-y-1.5">
            {confirmed.map((p) => (
              <li
                key={p.id}
                className="flex items-center justify-between rounded-xl bg-card px-3 py-2 text-sm"
              >
                <span>{p.profiles?.full_name || "Jogador"}</span>
                <span
                  className={`text-xs font-semibold ${
                    p.payment_status === "paid" ? "text-success" : "text-muted-foreground"
                  }`}
                >
                  {p.payment_status === "paid" ? "pago" : "pendente"}
                </span>
              </li>
            ))}
          </ul>
          {waitlist.length > 0 && (
            <p className="mt-2 text-xs text-muted-foreground">
              {waitlist.length} na lista de espera · entram automaticamente se alguém sair
            </p>
          )}
        </section>

        {isOrganizer ? (
          <WhatsAppBroadcast
            matchId={match.id}
            message={shareMsg}
            autoOpen={wa === true}
            trigger={
              <button className="card-3d card-3d-press mt-6 flex w-full items-center justify-center gap-2 rounded-2xl py-3 text-sm font-black uppercase tracking-[0.16em] text-primary">
                <Share2 className="size-4" /> Avisar jogadores no WhatsApp
              </button>
            }
          />
        ) : (
          <button
            onClick={() => openWhatsApp(shareMsg)}
            className="card-3d card-3d-press mt-6 flex w-full items-center justify-center gap-2 rounded-2xl py-3 text-sm font-black uppercase tracking-[0.16em] text-primary"
          >
            <Share2 className="size-4" /> Chamar galera no WhatsApp
          </button>
        )}


        <button onClick={report} className="mt-6 flex items-center gap-1.5 text-xs text-muted-foreground">
          <Flag className="size-3.5" /> Denunciar partida
        </button>
      </main>

      <div className="fixed inset-x-0 bottom-0 z-40 border-t border-border/70 bg-surface/95 px-5 pb-[max(1rem,env(safe-area-inset-bottom))] pt-3 backdrop-blur">
        <div className="mx-auto flex max-w-md gap-2">
          {isOrganizer ? (
            <Link to="/meus-jogos" className="flex-1">
              <Button className="w-full" variant="secondary">
                Gerenciar partida
              </Button>
            </Link>
          ) : mine ? (
            <>
              <Button variant="secondary" className="flex-1" onClick={leave} disabled={busy}>
                Sair
              </Button>
              {mine.payment_status !== "paid" && match.price_cents > 0 && (
                <PixDialog
                  amountCents={match.price_cents}
                  pixKey={match.pix_key}
                  merchant={organizer?.full_name ?? "Organizador"}
                  description={`Vaga ${match.venue}`}
                  onConfirm={payPix}
                  trigger={<Button className="flex-1">Pagar Pix</Button>}
                />
              )}
            </>
          ) : (
            <Button className="w-full" onClick={join} disabled={busy}>
              {left > 0 ? "Quero jogar" : "Entrar na lista de espera"}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

function Stat({ icon, label }: { icon?: React.ReactNode; label: string }) {
  return (
    <div className="card-3d rounded-2xl p-3">
      <span className="flex items-center justify-center gap-1.5 text-sm font-bold">
        {icon}
        {label}
      </span>
    </div>
  );
}
