export type SportId = "futsal" | "futebol" | "volei" | "basquete" | "tenis" | "outros";

export type Sport = {
  id: SportId;
  label: string;
  emoji: string;
  active: boolean;
};

export const SPORTS: Sport[] = [
  { id: "futebol", label: "Futebol", emoji: "⚽", active: true },
  { id: "futsal", label: "Futsal", emoji: "🥅", active: true },
  { id: "volei", label: "Vôlei", emoji: "🏐", active: false },
  { id: "basquete", label: "Basquete", emoji: "🏀", active: false },
  { id: "tenis", label: "Tênis", emoji: "🎾", active: false },
  { id: "outros", label: "Outros", emoji: "🏸", active: false },
];

export type Match = {
  id: string;
  sport: SportId;
  venue: string;
  neighborhood: string;
  address: string;
  distanceKm: number;
  day: "Hoje" | "Amanhã" | string;
  time: string;
  totalSlots: number;
  filledSlots: number;
  price: number;
  platformFee: number;
  level: "Iniciante" | "Intermediário" | "Avançado" | "Livre";
  organizer: { name: string; rating: number; matches: number };
  matchScore: number;
  rules: string[];
  waitlist: number;
};

export const MATCHES: Match[] = [
  {
    id: "green-ball-20h",
    sport: "futsal",
    venue: "Green Ball",
    neighborhood: "União dos Cegos",
    address: "Quadra 2 — Green Ball, União dos Cegos",
    distanceKm: 1.2,
    day: "Hoje",
    time: "20:00",
    totalSlots: 14,
    filledSlots: 12,
    price: 20,
    platformFee: 1.5,
    level: "Intermediário",
    organizer: { name: "Marcos Andrade", rating: 4.9, matches: 87 },
    matchScore: 95,
    rules: ["Chegar 10 min antes", "Tênis de quadra obrigatório", "Sem carrinho"],
    waitlist: 3,
  },
  {
    id: "green-ball-21h",
    sport: "futsal",
    venue: "Green Ball",
    neighborhood: "União dos Cegos",
    address: "Quadra 1 — Green Ball, União dos Cegos",
    distanceKm: 1.2,
    day: "Hoje",
    time: "21:30",
    totalSlots: 12,
    filledSlots: 12,
    price: 18,
    platformFee: 1.5,
    level: "Livre",
    organizer: { name: "Rafa Lopes", rating: 4.7, matches: 41 },
    matchScore: 78,
    rules: ["Time colete escuro", "Pagamento no Pix até o início"],
    waitlist: 5,
  },
  {
    id: "campo-do-zeca",
    sport: "futebol",
    venue: "Campo do Zeca",
    neighborhood: "Vila Nova",
    distanceKm: 3.4,
    address: "Rua das Palmeiras, 210 — Vila Nova",
    day: "Amanhã",
    time: "19:00",
    totalSlots: 22,
    filledSlots: 17,
    price: 25,
    platformFee: 1.5,
    level: "Intermediário",
    organizer: { name: "Dona Léa Sports", rating: 4.8, matches: 132 },
    matchScore: 88,
    rules: ["Chuteira de campo", "Dois tempos de 30 min"],
    waitlist: 0,
  },
  {
    id: "arena-central",
    sport: "futsal",
    venue: "Arena Central",
    neighborhood: "Centro",
    address: "Av. Central, 1002 — Centro",
    distanceKm: 4.8,
    day: "Amanhã",
    time: "07:00",
    totalSlots: 10,
    filledSlots: 6,
    price: 15,
    platformFee: 1.5,
    level: "Iniciante",
    organizer: { name: "Bia Ramos", rating: 5, matches: 23 },
    matchScore: 71,
    rules: ["Jogo leve, sem catimba"],
    waitlist: 0,
  },
];

export const getMatch = (id: string) => MATCHES.find((m) => m.id === id);

export const sportOf = (id: SportId) => SPORTS.find((s) => s.id === id)!;

export const slotsLeft = (m: Match) => m.totalSlots - m.filledSlots;

export const formatBRL = (value: number) =>
  value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export const PLAYER = {
  name: "Jonatha",
  location: "União dos Cegos",
  rating: 4.8,
  matches: 34,
  position: "Ala",
  level: "Intermediário",
};

export const MY_MATCHES = [
  { matchId: "green-ball-20h", status: "Confirmado" as const, position: 13, paid: false },
  { matchId: "campo-do-zeca", status: "Lista de espera" as const, position: 2, paid: false },
];

export const ORGANIZER_MATCHES = [
  { matchId: "green-ball-20h", paidCount: 9, waitlist: 3 },
  { matchId: "arena-central", paidCount: 4, waitlist: 0 },
];
