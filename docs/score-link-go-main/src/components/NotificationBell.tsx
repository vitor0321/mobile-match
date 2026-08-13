import { useEffect, useState } from "react";
import { Bell } from "lucide-react";
import { Link } from "@tanstack/react-router";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";

type Notif = {
  id: string;
  title: string;
  body: string | null;
  match_id: string | null;
  read_at: string | null;
  created_at: string;
};

export function NotificationBell({ userId }: { userId: string | null }) {
  const [items, setItems] = useState<Notif[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!userId) return;
    supabase
      .from("notifications")
      .select("id,title,body,match_id,read_at,created_at")
      .order("created_at", { ascending: false })
      .limit(30)
      .then(({ data }) => setItems((data as Notif[]) ?? []));

    const channel = supabase
      .channel("notif-" + userId)
      .on(
        "postgres_changes",
        { event: "INSERT", schema: "public", table: "notifications", filter: `user_id=eq.${userId}` },
        (payload) => {
          const n = payload.new as Notif;
          setItems((prev) => [n, ...prev]);
          toast.success(n.title, { description: n.body ?? undefined });
          if (typeof Notification !== "undefined" && Notification.permission === "granted") {
            new Notification(n.title, { body: n.body ?? "", icon: "/favicon.ico" });
          }
        },
      )
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [userId]);

  const unread = items.filter((i) => !i.read_at).length;

  async function markAllRead() {
    if (!userId || unread === 0) return;
    setItems((prev) => prev.map((i) => ({ ...i, read_at: i.read_at ?? new Date().toISOString() })));
    await supabase
      .from("notifications")
      .update({ read_at: new Date().toISOString() })
      .is("read_at", null)
      .eq("user_id", userId);
  }

  return (
    <Sheet
      open={open}
      onOpenChange={(v) => {
        setOpen(v);
        if (v) void markAllRead();
      }}
    >
      <SheetTrigger className="relative grid size-10 place-items-center rounded-full border border-border/70 bg-card">
        <Bell className="size-5" />
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 grid min-w-5 place-items-center rounded-full bg-primary px-1 text-[0.6rem] font-bold text-primary-foreground">
            {unread}
          </span>
        )}
      </SheetTrigger>
      <SheetContent side="right" className="w-[88vw] sm:max-w-sm">
        <SheetHeader>
          <SheetTitle>Notificações</SheetTitle>
        </SheetHeader>
        <div className="mt-4 space-y-2 overflow-y-auto pb-8">
          {!userId && (
            <p className="text-sm text-muted-foreground">Entre na sua conta para receber avisos de vagas.</p>
          )}
          {userId && items.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Nada por aqui ainda. Ative sua disponibilidade para ser avisado de vagas perto de você.
            </p>
          )}
          {items.map((n) => {
            const content = (
              <div className="rounded-2xl border border-border/60 bg-card p-3">
                <p className="text-sm font-semibold">{n.title}</p>
                {n.body && <p className="mt-0.5 text-xs text-muted-foreground">{n.body}</p>}
                <p className="mt-1 text-[0.65rem] text-muted-foreground">
                  {new Date(n.created_at).toLocaleString("pt-BR")}
                </p>
              </div>
            );
            return n.match_id ? (
              <Link key={n.id} to="/partida/$id" params={{ id: n.match_id }} onClick={() => setOpen(false)}>
                {content}
              </Link>
            ) : (
              <div key={n.id}>{content}</div>
            );
          })}
        </div>
      </SheetContent>
    </Sheet>
  );
}

export async function askNotificationPermission() {
  if (typeof Notification === "undefined") return false;
  if (Notification.permission === "granted") return true;
  const res = await Notification.requestPermission();
  return res === "granted";
}
