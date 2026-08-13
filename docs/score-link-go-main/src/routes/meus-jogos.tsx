import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Clock, Users, Wallet } from "lucide-react";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/hooks/useAuth";
import { BottomNav } from "@/components/BottomNav";
import { sportEmoji, type MatchRow } from "@/lib/db";
import { formatWhen } from "@/lib/geo";
import { formatBRLCents } from "@/lib/pix";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/meus-jogos")({
  head: () => ({
    meta: [
      { title: "Meus jogos — Vaga" },
      {
        name: "description",
        content: "Acompanhe as partidas que você confirmou e gerencie as peladas que você organiza.",
      },
      { property: "og:title", content: "Meus jogos — Vaga" },
      { property: "og:description", content: "Suas confirmações, lista de espera e partidas organizadas." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: MyGamesPage,
});

function MyGamesPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState<"jogador" | "organizador">("jogador");
  const [playing, setPlaying] = useState<{ match: MatchRow; status: string; paid: boolean }[]>([]);
  const [organized, setOrganized] = useState<{ match: MatchRow; filled: number }[]>([]);

  useEffect(() => {
    if (!user) return;
    void (async () => {
      const { data: parts } = await supabase
        .from("match_participants")
        .select("match_id,status,payment_status")
        .eq("user_id", user.id)
        .neq("status", "cancelled");
      const ids = (parts ?? []).map((p: { match_id: string }) => p.match_id);
      const { data: ms } = ids.length
        ? await supabase.from("matches").select("*").in("id", ids).order("starts_at")
        : { data: [] };
      const byId = new Map(((ms as MatchRow[]) ?? []).map((m) => [m.id, m]));
      setPlaying(
        (parts ?? [])
          .map((p: { match_id: string; status: string; payment_status: string }) => {
            const match = byId.get(p.match_id);
            return match ? { match, status: p.status, paid: p.payment_status === "paid" } : null;
          })
          .filter((x): x is { match: MatchRow; status: string; paid: boolean } => !!x),
      );

      const { data: mine } = await supabase
        .from("matches")
        .select("*")
        .eq("organizer_id", user.id)
        .order("starts_at");
      const mineRows = (mine as MatchRow[]) ?? [];
      const { data: allParts } = mineRows.length
        ? await supabase
            .from("match_participants")
            .select("match_id")
            .eq("status", "confirmed")
            .in(
              "match_id",
              mineRows.map((m) => m.id),
            )
        : { data: [] };
      const counts: Record<string, number> = {};
      ((allParts as { match_id: string }[]) ?? []).forEach((p) => {
        counts[p.match_id] = (counts[p.match_id] ?? 0) + 1;
      });
      setOrganized(mineRows.map((m) => ({ match: m, filled: counts[m.id] ?? 0 })));
    })();
  }, [user?.id]);

  if (!user) {
    return (
      <div className="min-h-screen bg-background px-6 pt-24 text-center">
        <h1 className="text-xl font-black">Entre para ver seus jogos</h1>
        <Link to="/auth">
          <Button className="mt-4">Entrar</Button>
        </Link>
        <BottomNav />
      </div>
    );
  }

  const revenue = organized.reduce((sum, o) => sum + o.filled * o.match.price_cents, 0);

  return (
    <div className="min-h-screen bg-background pb-24">
      <header className="px-5 pt-6">
        <h1 className="text-2xl font-black">Meus jogos</h1>
        <div className="mt-4 grid grid-cols-2 gap-2 rounded-2xl bg-secondary p-1">
          {(["jogador", "organizador"] as const).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`rounded-xl py-2 text-sm font-semibold capitalize ${
                tab === t ? "bg-card text-foreground shadow-lift" : "text-muted-foreground"
              }`}
            >
              {t}
            </button>
          ))}
        </div>
      </header>

      <section className="mx-auto max-w-md space-y-3 px-5 pt-4">
        {tab === "jogador" &&
          (playing.length === 0 ? (
            <Empty text="Você ainda não confirmou nenhuma partida." />
          ) : (
            playing.map(({ match, status, paid }) => (
              <Card key={match.id} match={match}>
                <span
                  className={`rounded-full px-2.5 py-1 text-xs font-bold ${
                    status === "confirmed" ? "bg-success/15 text-success" : "bg-warning/15 text-warning"
                  }`}
                >
                  {status === "confirmed" ? "Confirmado" : "Lista de espera"}
                </span>
                <span className={`text-xs font-semibold ${paid ? "text-success" : "text-muted-foreground"}`}>
                  {paid ? "pago" : "pagamento pendente"}
                </span>
              </Card>
            ))
          ))}

        {tab === "organizador" && (
          <>
            <div className="flex items-center justify-between rounded-2xl border border-border/70 bg-card p-4">
              <span className="flex items-center gap-2 text-sm font-semibold">
                <Wallet className="size-4 text-primary" /> Arrecadação prevista
              </span>
              <span className="text-lg font-black text-primary">{formatBRLCents(revenue)}</span>
            </div>
            {organized.length === 0 ? (
              <Empty text="Você ainda não criou partidas." />
            ) : (
              organized.map(({ match, filled }) => (
                <Card key={match.id} match={match}>
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Users className="size-3.5" /> {filled}/{match.total_slots} confirmados
                  </span>
                  <span className="text-xs font-semibold text-primary">
                    {formatBRLCents(filled * match.price_cents)}
                  </span>
                </Card>
              ))
            )}
            <Link to="/criar">
              <Button className="w-full">Criar nova partida</Button>
            </Link>
          </>
        )}
      </section>

      <BottomNav />
    </div>
  );
}

function Card({ match, children }: { match: MatchRow; children: React.ReactNode }) {
  return (
    <Link
      to="/partida/$id"
      params={{ id: match.id }}
      className="block rounded-3xl border border-border/70 bg-card p-4 shadow-lift"
    >
      <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
        {sportEmoji(match.sport)} {match.level}
      </p>
      <h3 className="mt-1 text-lg font-bold">{match.venue}</h3>
      <p className="mt-0.5 flex items-center gap-1.5 text-xs text-muted-foreground">
        <Clock className="size-3.5" /> {formatWhen(match.starts_at)}
      </p>
      <div className="mt-3 flex items-center justify-between border-t border-border/60 pt-3">{children}</div>
    </Link>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <p className="rounded-3xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
      {text}
    </p>
  );
}
