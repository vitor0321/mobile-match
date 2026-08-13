import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Check, LogOut, Star, Shield } from "lucide-react";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/hooks/useAuth";
import { BottomNav } from "@/components/BottomNav";
import { PixDialog } from "@/components/PixDialog";
import { PLANS, type PlanId } from "@/lib/db";
import { formatBRLCents } from "@/lib/pix";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/perfil")({
  head: () => ({
    meta: [
      { title: "Meu perfil — Vaga" },
      {
        name: "description",
        content: "Gerencie seus dados, chave Pix, posição em campo e o plano de organizador na Vaga.",
      },
      { property: "og:title", content: "Meu perfil — Vaga" },
      { property: "og:description", content: "Dados, chave Pix, avaliações e planos de organizador." },
      { property: "og:type", content: "profile" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: ProfilePage,
});

const PLATFORM_PIX_KEY = "pagamentos@vaga.app";

function ProfilePage() {
  const { user, profile, setProfile, signOut } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ full_name: "", position: "", pix_key: "", phone: "" });
  const [plan, setPlan] = useState<PlanId>("free");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (profile) {
      setForm({
        full_name: profile.full_name ?? "",
        position: profile.position ?? "",
        pix_key: profile.pix_key ?? "",
        phone: profile.phone ?? "",
      });
      void supabase
        .from("subscriptions")
        .select("plan")
        .eq("user_id", profile.id)
        .maybeSingle()
        .then(({ data }) => setPlan(((data?.plan as PlanId) ?? "free")));
    }
  }, [profile?.id]);

  if (!user) {
    return (
      <div className="min-h-screen bg-background px-6 pt-24 text-center">
        <h1 className="text-xl font-black">Entre na sua conta</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Para confirmar vagas, criar partidas e receber avisos.
        </p>
        <Link to="/auth">
          <Button className="mt-4">Entrar ou criar conta</Button>
        </Link>
        <BottomNav />
      </div>
    );
  }

  async function save() {
    setSaving(true);
    const { error } = await supabase.from("profiles").update(form).eq("id", user!.id);
    setSaving(false);
    if (error) {
      toast.error(error.message);
      return;
    }
    setProfile((p) => (p ? { ...p, ...form } : p));
    toast.success("Perfil salvo");
  }

  async function subscribe(id: PlanId) {
    const p = PLANS.find((x) => x.id === id)!;
    await supabase.from("subscriptions").upsert(
      {
        user_id: user!.id,
        plan: id,
        status: p.priceCents === 0 ? "paid" : "pending",
        current_period_end: new Date(Date.now() + 30 * 86400_000).toISOString(),
      },
      { onConflict: "user_id" },
    );
    if (p.priceCents > 0) {
      await supabase.from("payments").insert({
        user_id: user!.id,
        kind: "subscription",
        amount_cents: p.priceCents,
        pix_key: PLATFORM_PIX_KEY,
        status: "pending",
      });
    }
    setPlan(id);
    toast.success(p.priceCents === 0 ? "Plano gratuito ativo" : "Assinatura registrada! Confirmação em breve.");
  }

  return (
    <div className="min-h-screen bg-background pb-24">
      <header className="px-5 pt-6">
        <div className="flex items-center gap-4">
          <div className="grid size-16 place-items-center rounded-2xl bg-gradient-to-br from-primary to-accent text-2xl font-black text-primary-foreground">
            {(form.full_name || user.email || "V").charAt(0).toUpperCase()}
          </div>
          <div className="min-w-0">
            <h1 className="truncate text-xl font-black">{form.full_name || "Jogador"}</h1>
            <p className="truncate text-sm text-muted-foreground">{user.email}</p>
            <p className="mt-1 flex items-center gap-1 text-sm font-semibold">
              <Star className="size-4 fill-warning text-warning" />
              {Number(profile?.rating ?? 5).toFixed(1)}
            </p>
          </div>
        </div>
      </header>

      <section className="mx-auto max-w-md space-y-4 px-5 pt-6">
        <div className="space-y-3 rounded-3xl border border-border/70 bg-card p-4">
          <h2 className="text-sm font-bold">Dados</h2>
          <div className="space-y-1.5">
            <Label>Nome</Label>
            <Input
              value={form.full_name}
              onChange={(e) => setForm((f) => ({ ...f, full_name: e.target.value }))}
            />
          </div>
          <div className="space-y-1.5">
            <Label>Telefone</Label>
            <Input value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
          </div>
          <div className="space-y-1.5">
            <Label>Posição</Label>
            <Input
              value={form.position}
              onChange={(e) => setForm((f) => ({ ...f, position: e.target.value }))}
              placeholder="Goleiro, zagueiro, atacante…"
            />
          </div>
          <div className="space-y-1.5">
            <Label>Minha chave Pix</Label>
            <Input
              value={form.pix_key}
              onChange={(e) => setForm((f) => ({ ...f, pix_key: e.target.value }))}
              placeholder="Para receber como organizador"
            />
          </div>
          <Button onClick={save} disabled={saving} className="w-full">
            Salvar
          </Button>
        </div>

        <div className="rounded-3xl border border-border/70 bg-card p-4">
          <h2 className="flex items-center gap-2 text-sm font-bold">
            <Shield className="size-4 text-primary" /> Planos de organizador
          </h2>
          <div className="mt-3 space-y-3">
            {PLANS.map((p) => {
              const active = plan === p.id;
              return (
                <div
                  key={p.id}
                  className={`rounded-2xl border p-4 ${
                    active ? "border-primary bg-primary/5" : "border-border/70"
                  }`}
                >
                  <div className="flex items-baseline justify-between">
                    <h3 className="font-bold">{p.name}</h3>
                    <span className="font-black text-primary">
                      {p.priceCents === 0 ? "Grátis" : `${formatBRLCents(p.priceCents)}/mês`}
                    </span>
                  </div>
                  <ul className="mt-2 space-y-1 text-xs text-muted-foreground">
                    {p.perks.map((perk) => (
                      <li key={perk} className="flex items-center gap-1.5">
                        <Check className="size-3.5 text-success" /> {perk}
                      </li>
                    ))}
                  </ul>
                  {active ? (
                    <p className="mt-3 text-xs font-bold text-primary">Plano atual</p>
                  ) : p.priceCents === 0 ? (
                    <Button variant="secondary" className="mt-3 w-full" onClick={() => subscribe(p.id)}>
                      Usar grátis
                    </Button>
                  ) : (
                    <PixDialog
                      amountCents={p.priceCents}
                      pixKey={PLATFORM_PIX_KEY}
                      merchant="Vaga"
                      description={`Plano ${p.name}`}
                      onConfirm={() => subscribe(p.id)}
                      trigger={<Button className="mt-3 w-full">Assinar com Pix</Button>}
                    />
                  )}
                </div>
              );
            })}
          </div>
        </div>

        <Button
          variant="ghost"
          className="w-full text-muted-foreground"
          onClick={async () => {
            await signOut();
            void navigate({ to: "/" });
          }}
        >
          <LogOut className="size-4" /> Sair da conta
        </Button>
      </section>

      <BottomNav />
    </div>
  );
}
