import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { MapPin, Navigation } from "lucide-react";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/hooks/useAuth";
import { BottomNav } from "@/components/BottomNav";
import { SPORTS } from "@/lib/db";
import { DEFAULT_CENTER } from "@/lib/geo";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

export const Route = createFileRoute("/criar")({
  head: () => ({
    meta: [
      { title: "Criar partida na Vaga — preencha suas vagas rápido" },
      {
        name: "description",
        content:
          "Publique sua partida, defina vagas e valor, e avise automaticamente os jogadores disponíveis na região.",
      },
      { property: "og:title", content: "Criar partida na Vaga" },
      {
        property: "og:description",
        content: "Publique sua pelada e complete o time com jogadores da sua região.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: CreatePage,
});

function CreatePage() {
  const navigate = useNavigate();
  const { user, profile } = useAuth();
  const [form, setForm] = useState({
    sport: "futsal",
    venue: "",
    neighborhood: "",
    address: "",
    date: "",
    time: "20:00",
    total_slots: 12,
    price: 20,
    level: "Livre",
    rules: "",
    pix_key: "",
  });
  const [coords, setCoords] = useState(DEFAULT_CENTER);
  const [hasGeo, setHasGeo] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (profile?.pix_key && !form.pix_key) setForm((f) => ({ ...f, pix_key: profile.pix_key! }));
    if (profile?.lat && profile?.lng) {
      setCoords({ lat: profile.lat, lng: profile.lng });
      setHasGeo(true);
    }
  }, [profile?.pix_key, profile?.lat, profile?.lng]);

  function locate() {
    if (!navigator.geolocation) {
      toast.error("Localização não disponível");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        setHasGeo(true);
        toast.success("Local marcado no mapa");
      },
      () => toast.error("Não consegui pegar a localização"),
      { enableHighAccuracy: true, timeout: 8000 },
    );
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!user) {
      void navigate({ to: "/auth" });
      return;
    }
    setBusy(true);
    try {
      const starts = new Date(`${form.date}T${form.time}:00`);
      const priceCents = Math.round(form.price * 100);
      const { data, error } = await supabase
        .from("matches")
        .insert({
          organizer_id: user.id,
          sport: form.sport,
          title: `${form.venue} · ${form.level}`,
          venue: form.venue,
          neighborhood: form.neighborhood,
          address: form.address,
          lat: coords.lat,
          lng: coords.lng,
          starts_at: starts.toISOString(),
          total_slots: form.total_slots,
          price_cents: priceCents,
          platform_fee_cents: Math.round(priceCents * 0.07),
          level: form.level,
          rules: form.rules
            .split("\n")
            .map((r) => r.trim())
            .filter(Boolean),
          pix_key: form.pix_key || null,
        })
        .select("id")
        .single();
      if (error) throw error;
      await supabase.from("match_participants").insert({
        match_id: data.id,
        user_id: user.id,
        status: "confirmed",
        payment_status: "paid",
      });
      toast.success("Partida publicada! Avisando jogadores próximos…");
      void navigate({ to: "/partida/$id", params: { id: data.id }, search: { wa: true } });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Não deu para publicar");
    } finally {
      setBusy(false);
    }
  }

  const fee = Math.round(form.price * 100 * 0.07) / 100;

  return (
    <div className="min-h-screen bg-background pb-28">
      <header className="px-5 pt-6">
        <h1 className="text-2xl font-black">Criar partida</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Publique e a gente avisa quem está disponível por perto.
        </p>
      </header>

      <form onSubmit={submit} className="mx-auto mt-6 max-w-md space-y-4 px-5">
        <div className="flex gap-2 overflow-x-auto pb-1">
          {SPORTS.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => setForm((f) => ({ ...f, sport: s.id }))}
              className={`shrink-0 rounded-full border px-3.5 py-1.5 text-xs font-semibold ${
                form.sport === s.id
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border/70 bg-card text-muted-foreground"
              }`}
            >
              {s.emoji} {s.label}
            </button>
          ))}
        </div>

        <Field label="Local (quadra/campo)">
          <Input
            value={form.venue}
            onChange={(e) => setForm((f) => ({ ...f, venue: e.target.value }))}
            placeholder="Green Ball"
            required
          />
        </Field>
        <Field label="Bairro">
          <Input
            value={form.neighborhood}
            onChange={(e) => setForm((f) => ({ ...f, neighborhood: e.target.value }))}
            placeholder="União dos Cegos"
          />
        </Field>
        <Field label="Endereço">
          <Input
            value={form.address}
            onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
            placeholder="Rua, número"
          />
        </Field>

        <button
          type="button"
          onClick={locate}
          className="flex w-full items-center justify-between rounded-2xl border border-border/70 bg-card p-3 text-left"
        >
          <span className="flex items-center gap-2 text-sm font-semibold">
            <MapPin className="size-4 text-primary" />
            {hasGeo ? "Local marcado no mapa" : "Marcar local no mapa"}
          </span>
          <Navigation className="size-4 text-muted-foreground" />
        </button>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Data">
            <Input
              type="date"
              value={form.date}
              onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
              required
            />
          </Field>
          <Field label="Hora">
            <Input
              type="time"
              value={form.time}
              onChange={(e) => setForm((f) => ({ ...f, time: e.target.value }))}
              required
            />
          </Field>
          <Field label="Vagas">
            <Input
              type="number"
              min={2}
              max={40}
              value={form.total_slots}
              onChange={(e) => setForm((f) => ({ ...f, total_slots: Number(e.target.value) }))}
            />
          </Field>
          <Field label="Valor por jogador (R$)">
            <Input
              type="number"
              min={0}
              step="0.5"
              value={form.price}
              onChange={(e) => setForm((f) => ({ ...f, price: Number(e.target.value) }))}
            />
          </Field>
        </div>

        <Field label="Nível">
          <div className="flex gap-2">
            {["Livre", "Iniciante", "Intermediário", "Avançado"].map((l) => (
              <button
                key={l}
                type="button"
                onClick={() => setForm((f) => ({ ...f, level: l }))}
                className={`flex-1 rounded-xl border px-2 py-2 text-xs font-semibold ${
                  form.level === l
                    ? "border-primary bg-primary/10 text-primary"
                    : "border-border/70 bg-card text-muted-foreground"
                }`}
              >
                {l}
              </button>
            ))}
          </div>
        </Field>

        <Field label="Chave Pix para receber">
          <Input
            value={form.pix_key}
            onChange={(e) => setForm((f) => ({ ...f, pix_key: e.target.value }))}
            placeholder="CPF, telefone, e-mail ou aleatória"
          />
        </Field>

        <Field label="Regras (uma por linha)">
          <Textarea
            rows={3}
            value={form.rules}
            onChange={(e) => setForm((f) => ({ ...f, rules: e.target.value }))}
            placeholder={"Chegar 10 min antes\nTênis de quadra"}
          />
        </Field>

        <div className="rounded-2xl bg-secondary p-3 text-xs text-muted-foreground">
          Taxa da plataforma prevista: <strong className="text-foreground">R$ {fee.toFixed(2)}</strong> por
          jogador. No MVP a cobrança é feita direto no seu Pix.
        </div>

        <Button type="submit" className="w-full" disabled={busy}>
          Publicar partida
        </Button>
      </form>

      <BottomNav />
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
