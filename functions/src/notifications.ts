// Quem recebe aviso de partida nova, e por quê.
//
// A seleção fica isolada aqui, sem Firestore, porque é a única parte com regra
// de negócio de verdade — e é a única que dá para testar sem emulador.

import {distanceKm, type Coordinates} from "./geo.js";

/**
 * Raio mínimo de notificação (regra B4 do plano).
 *
 * Mesmo quem configurou um raio pequeno de busca é avisado de partidas até
 * 20 km: procurar e ser avisado são intenções diferentes.
 */
export const MIN_NOTIFY_RADIUS_KM = 20;

/**
 * Teto do raio, e por consequência o tamanho da consulta por geohash.
 *
 * Espelha o limite do slider de raio no app (5-50 km). Sem teto, um perfil com
 * raio absurdo faria toda criação de partida varrer a base inteira.
 */
export const MAX_NOTIFY_RADIUS_KM = 50;

/**
 * Teto de destinatários por partida (risco R7: custo do Firestore).
 *
 * Quando estoura, os mais distantes é que ficam de fora — por isso a lista sai
 * ordenada por distância.
 */
export const MAX_RECIPIENTS = 60;

export type NotificationCandidate = {
  userId: string;
  lat: number;
  lng: number;
  /** Raio de busca do próprio jogador, em km. */
  radiusKm: number;
  /**
   * Esportes que o jogador marcou em disponibilidade. Vazio = qualquer esporte.
   */
  availableSports: string[];
  /** O toggle "estou disponível" (regra B5). */
  isAvailable: boolean;
  /**
   * Fim da janela de disponibilidade, em epoch millis. `null` = "até eu
   * desligar", que é o que o toggle grava hoje.
   */
  availableUntilMs: number | null;
};

export type MatchInvite = {
  matchId: string;
  organizerId: string;
  sport: string;
  lat: number;
  lng: number;
};

export type Recipient = {userId: string; distanceKm: number};

/**
 * Raio que vale para este jogador: nunca menos que [MIN_NOTIFY_RADIUS_KM],
 * nunca mais que [MAX_NOTIFY_RADIUS_KM].
 */
export function effectiveRadiusKm(candidateRadiusKm: unknown): number {
  const radius = typeof candidateRadiusKm === "number" && Number.isFinite(candidateRadiusKm)
    ? candidateRadiusKm
    : 0;
  return Math.min(Math.max(radius, MIN_NOTIFY_RADIUS_KM), MAX_NOTIFY_RADIUS_KM);
}

/**
 * Quem deve ser avisado da partida, do mais perto para o mais longe.
 *
 * Agora filtra por `isAvailable` (regra B5): o toggle existe no app, então o
 * campo finalmente quer dizer alguma coisa. Antes o filtro estava desligado
 * porque `isAvailable` nascia `false` em onUserCreate e nada o ligava —
 * aplicá-lo teria zerado todas as notificações.
 *
 * Cuidado que sobra: `onUserCreate` continua criando o documento privado com
 * `isAvailable: false`. Quem se cadastra e nunca abre o perfil não recebe aviso
 * nenhum. Se isso for indesejável, o lugar de mudar é o padrão em onUserCreate,
 * não aqui.
 *
 * @param nowMs relógio para a janela de disponibilidade; injetado para o teste
 *   conseguir atravessar o vencimento sem esperar
 */
export function selectRecipients(
  candidates: NotificationCandidate[],
  match: MatchInvite,
  maxRecipients: number = MAX_RECIPIENTS,
  nowMs: number = Date.now(),
): Recipient[] {
  const center: Coordinates = {lat: match.lat, lng: match.lng};

  const recipients: Recipient[] = [];

  for (const candidate of candidates) {
    if (candidate.userId === match.organizerId) continue;
    if (!isAvailableAt(candidate, nowMs)) continue;
    if (!matchesSportPreference(candidate, match.sport)) continue;

    const distance = distanceKm(center, {lat: candidate.lat, lng: candidate.lng});
    if (distance > effectiveRadiusKm(candidate.radiusKm)) continue;

    recipients.push({userId: candidate.userId, distanceKm: distance});
  }

  // Ordena antes de cortar: se alguém tem de ficar de fora, que sejam os mais
  // distantes.
  return recipients
    .sort((a, b) => a.distanceKm - b.distanceKm)
    .slice(0, maxRecipients);
}

/**
 * Disponível agora: o toggle ligado e a janela ainda aberta.
 *
 * `availableUntilMs` nulo é "até eu desligar", não "já venceu" — é o que o
 * toggle grava. Janela vencida vale como indisponível sem ninguém precisar
 * varrer a base para desligar o campo.
 */
function isAvailableAt(candidate: NotificationCandidate, nowMs: number): boolean {
  if (!candidate.isAvailable) return false;
  return candidate.availableUntilMs === null || candidate.availableUntilMs > nowMs;
}

/** Lista de esportes vazia significa "tanto faz", não "nenhum". */
function matchesSportPreference(candidate: NotificationCandidate, sport: string): boolean {
  if (candidate.availableSports.length === 0) return true;
  return candidate.availableSports.some(
    (preferred) => preferred.toLowerCase() === sport.toLowerCase(),
  );
}

/**
 * Converte um documento `profiles/{uid}/private/data` em candidato.
 *
 * Devolve `null` para quem não tem coordenada: sem posição não dá para medir
 * distância, e chutar o centro da cidade notificaria a pessoa errada.
 *
 * Vive aqui, e não dentro do trigger, porque é a parte que erra de verdade —
 * campo ausente, tipo errado, lista com lixo dentro — e a única testável sem
 * emulador.
 */
export function parseCandidate(
  userId: string,
  data: Record<string, unknown>,
  defaultRadiusKm: number,
): NotificationCandidate | null {
  if (typeof data.lat !== "number" || typeof data.lng !== "number") return null;
  if (!Number.isFinite(data.lat) || !Number.isFinite(data.lng)) return null;

  return {
    userId,
    lat: data.lat,
    lng: data.lng,
    radiusKm: typeof data.radiusKm === "number" ? data.radiusKm : defaultRadiusKm,
    availableSports: Array.isArray(data.availableSports)
      ? data.availableSports.filter((sport): sport is string => typeof sport === "string")
      : [],
    // Ausente ou de tipo errado conta como indisponível: na dúvida, não
    // incomodar. É o lado seguro do erro para notificação.
    isAvailable: data.isAvailable === true,
    availableUntilMs: readEpochMillis(data.availableUntil),
  };
}

/**
 * Aceita número (epoch millis) e `Timestamp` do Firestore, que é o que o SDK
 * devolve quando o campo foi gravado como data. Qualquer outra coisa vira
 * `null`, ou seja "sem vencimento".
 */
function readEpochMillis(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return value;

  const timestamp = value as {toMillis?: () => number} | null | undefined;
  if (timestamp && typeof timestamp.toMillis === "function") {
    const millis = timestamp.toMillis();
    return Number.isFinite(millis) ? millis : null;
  }

  return null;
}

/**
 * A escrita no participante representa alguém subindo da fila?
 *
 * Só interessa a transição fila -> confirmado num documento que já existia.
 * Entrar já confirmado não é promoção, e sair também não — sem essa checagem o
 * trigger dispararia "você subiu da fila" em toda escrita.
 */
export function isWaitlistPromotion(
  before: {isConfirmed?: unknown} | undefined,
  after: {isConfirmed?: unknown} | undefined,
): boolean {
  if (!before || !after) return false;
  return before.isConfirmed === false && after.isConfirmed === true;
}
