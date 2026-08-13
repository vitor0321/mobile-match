import { useEffect, useState } from "react";
import { useServerFn } from "@tanstack/react-start";
import { Copy, Loader2, Send, Users2 } from "lucide-react";
import { toast } from "sonner";
import { nearbyAvailablePlayers, type NearbyPlayer } from "@/lib/broadcast.functions";
import { openWhatsApp, waLink } from "@/lib/share";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";

export function WhatsAppBroadcast({
  matchId,
  message,
  trigger,
  autoOpen = false,
}: {
  matchId: string;
  message: string;
  trigger: React.ReactNode;
  autoOpen?: boolean;
}) {
  const fetchNearby = useServerFn(nearbyAvailablePlayers);
  const [players, setPlayers] = useState<NearbyPlayer[] | null>(null);
  const [sent, setSent] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  async function open(next: boolean) {
    setIsOpen(next);
    if (!next || players) return;
    setLoading(true);
    try {
      setPlayers(await fetchNearby({ data: { matchId } }));
    } catch {
      toast.error("Não consegui carregar os jogadores por perto");
      setPlayers([]);
    }
    setLoading(false);
  }

  useEffect(() => {
    if (autoOpen) void open(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoOpen]);

  const withPhone = (players ?? []).filter((p) => p.phone);
  const pending = withPhone.filter((p) => !sent[p.id]);

  return (
    <Sheet open={isOpen} onOpenChange={(v) => void open(v)}>
      <SheetTrigger asChild>{trigger}</SheetTrigger>
      <SheetContent side="bottom" className="max-h-[85vh] overflow-y-auto rounded-t-[2rem]">
        <SheetHeader>
          <SheetTitle className="text-hard flex items-center gap-2 text-lg font-black uppercase">
            <Users2 className="size-5 text-primary" /> Chamar galera no WhatsApp
          </SheetTitle>
        </SheetHeader>

        <div className="card-3d mt-4 rounded-2xl p-3 text-xs leading-relaxed whitespace-pre-wrap text-muted-foreground">
          {message}
        </div>
        <div className="mt-3 flex gap-2">
          <Button
            variant="secondary"
            className="flex-1"
            onClick={() => {
              void navigator.clipboard.writeText(message);
              toast.success("Mensagem copiada");
            }}
          >
            <Copy className="size-4" /> Copiar
          </Button>
          <Button className="flex-1" onClick={() => openWhatsApp(message)}>
            <Send className="size-4" /> Enviar p/ grupo
          </Button>
        </div>

        {pending.length > 0 && (
          <Button
            className="mt-2 w-full"
            variant="outline"
            onClick={() => {
              const next = pending[0];
              if (!next) return;
              setSent((s) => ({ ...s, [next.id]: true }));
              openWhatsApp(message, next.phone);
            }}
          >
            <Send className="size-4" /> Avisar próximo ({pending.length} restantes)
          </Button>
        )}

        <p className="mt-5 text-[0.62rem] font-black uppercase tracking-[0.24em] text-muted-foreground">
          Disponíveis por perto {loading ? "" : `· ${withPhone.length}`}
        </p>


        {loading && (
          <div className="mt-4 flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" /> Buscando jogadores…
          </div>
        )}

        {!loading && withPhone.length === 0 && (
          <p className="mt-3 text-sm text-muted-foreground">
            Ninguém com WhatsApp cadastrado disponível agora. Use o botão de grupo acima.
          </p>
        )}

        <div className="mt-3 space-y-2 pb-8">
          {withPhone.map((p) => (
            <div key={p.id} className="card-3d flex items-center justify-between rounded-2xl p-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-black">{p.full_name}</p>
                <p className="text-xs text-muted-foreground">a {p.km.toFixed(1)} km</p>
              </div>
              <a
                href={waLink(message, p.phone)}
                target="_blank"
                rel="noopener noreferrer"
                onClick={() => setSent((s) => ({ ...s, [p.id]: true }))}
                className={`rounded-xl px-3 py-2 text-xs font-black uppercase tracking-[0.14em] bevel-top ${
                  sent[p.id] ? "bg-muted text-muted-foreground" : "action-gradient shadow-glow"
                }`}
              >
                {sent[p.id] ? "Enviado" : "Avisar"}
              </a>
            </div>
          ))}
        </div>
      </SheetContent>
    </Sheet>
  );
}
