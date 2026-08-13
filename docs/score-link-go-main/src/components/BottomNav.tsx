import { Link, useRouterState } from "@tanstack/react-router";
import { Home, Search, PlusCircle, CalendarDays, User } from "lucide-react";

const items = [
  { to: "/", label: "Início", icon: Home, exact: true },
  { to: "/buscar", label: "Encontrar", icon: Search, exact: false },
  { to: "/criar", label: "Criar", icon: PlusCircle, exact: false },
  { to: "/meus-jogos", label: "Meus jogos", icon: CalendarDays, exact: false },
  { to: "/perfil", label: "Perfil", icon: User, exact: false },
] as const;

export function BottomNav() {
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-border/70 bg-surface/90 backdrop-blur-xl bevel-top">
      <ul className="mx-auto flex max-w-md items-stretch justify-between px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2">
        {items.map(({ to, label, icon: Icon, exact }) => {
          const active = exact ? pathname === to : pathname.startsWith(to);
          return (
            <li key={to} className="flex-1">
              <Link
                to={to}
                className={`flex flex-col items-center gap-1 rounded-xl py-1.5 text-[0.6rem] font-black uppercase tracking-wide transition-colors ${
                  active ? "text-primary" : "text-muted-foreground"
                }`}
              >
                <Icon className="size-5" strokeWidth={active ? 2.4 : 1.8} />
                {label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
