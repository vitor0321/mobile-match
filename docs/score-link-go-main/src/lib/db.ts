export type MatchRow = {
  id: string;
  organizer_id: string;
  sport: string;
  title: string;
  venue: string;
  address: string | null;
  neighborhood: string | null;
  lat: number;
  lng: number;
  starts_at: string;
  duration_min: number;
  total_slots: number;
  price_cents: number;
  platform_fee_cents: number;
  level: string;
  rules: string[];
  pix_key: string | null;
  status: "open" | "full" | "cancelled" | "finished";
};

export type ParticipantRow = {
  id: string;
  match_id: string;
  user_id: string;
  status: "confirmed" | "waitlist" | "cancelled";
  payment_status: "pending" | "paid" | "expired" | "refunded";
  created_at: string;
};

export const SPORTS = [
  { id: "futebol", label: "Futebol", emoji: "⚽" },
  { id: "futsal", label: "Futsal", emoji: "🥅" },
  { id: "volei", label: "Vôlei", emoji: "🏐" },
  { id: "basquete", label: "Basquete", emoji: "🏀" },
] as const;

export function sportEmoji(id: string) {
  return SPORTS.find((s) => s.id === id)?.emoji ?? "🏅";
}

export const PLANS = [
  {
    id: "free" as const,
    name: "Gratuito",
    price: 0,
    priceCents: 0,
    perks: ["1 partida ativa por vez", "Até 14 jogadores", "Divulgação básica"],
  },
  {
    id: "pro" as const,
    name: "Pro",
    price: 29.9,
    priceCents: 2990,
    perks: ["Partidas ilimitadas", "Notificação turbinada na região", "Lista de espera automática", "Selo verificado"],
  },
  {
    id: "business" as const,
    name: "Profissional",
    price: 79.9,
    priceCents: 7990,
    perks: ["Tudo do Pro", "Múltiplas quadras e equipe", "Relatórios de arrecadação", "Suporte prioritário"],
  },
];

export type PlanId = (typeof PLANS)[number]["id"];
