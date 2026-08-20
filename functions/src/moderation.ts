// Trust & Safety — denúncias e moderação (Phase 6).
//
// Espelhado no cliente por `products/games/.../domain/model/ReportReason.kt` e
// `ModerationLevel.kt`. Os identificadores abaixo são o contrato: mudá-los
// invalida denúncias já gravadas, então só adicione, nunca renomeie.

import {HttpsError} from "firebase-functions/v2/https";

/**
 * Os dez motivos de denúncia.
 *
 * `other` é obrigatório na lista: sem uma saída genérica as pessoas escolhem o
 * motivo errado só para conseguir denunciar, e a estatística vira ruído.
 */
export const REPORT_REASONS = [
  "no_show",
  "late",
  "no_payment",
  "aggressive_behavior",
  "verbal_abuse",
  "discrimination",
  "harassment",
  "dangerous_play",
  "fake_profile",
  "other",
] as const;

export type ReportReason = (typeof REPORT_REASONS)[number];

export function isReportReason(value: unknown): value is ReportReason {
  return typeof value === "string" && (REPORT_REASONS as readonly string[]).includes(value);
}

export const MAX_REPORT_DETAILS_LENGTH = 1_000;

/** Escalonamento: advertência -> suspensão -> revisão humana. */
export type ModerationLevel = "none" | "warning" | "suspended" | "banned";

/**
 * Quantos **denunciantes distintos** disparam cada nível.
 *
 * Contar denúncias cruas deixaria uma pessoa sozinha derrubar outra: bastaria
 * abrir dez denúncias. O que conta é quantas pessoas diferentes reclamaram.
 */
export const WARNING_THRESHOLD = 3;
export const SUSPENSION_THRESHOLD = 6;
export const REVIEW_THRESHOLD = 10;

/** Duração da suspensão automática. */
export const SUSPENSION_DAYS = 14;

/**
 * Janela de contagem.
 *
 * Sem ela a punição é permanente na prática: denúncias de dois anos atrás
 * continuariam somando e ninguém jamais sairia de uma suspensão.
 */
export const REPORT_WINDOW_DAYS = 180;

export const DAY_IN_MILLIS = 24 * 60 * 60 * 1_000;

/**
 * O banimento **não** é automático.
 *
 * Banir por contagem de denúncias é um vetor de brigading: um grupo coordenado
 * elimina qualquer jogador. Ao atingir [REVIEW_THRESHOLD] a conta é suspensa e
 * marcada para revisão; só um admin (custom claim) grava `banned`.
 */
export function levelForReporterCount(distinctReporters: number): ModerationLevel {
  if (distinctReporters >= SUSPENSION_THRESHOLD) return "suspended";
  if (distinctReporters >= WARNING_THRESHOLD) return "warning";
  return "none";
}

export function requiresHumanReview(distinctReporters: number): boolean {
  return distinctReporters >= REVIEW_THRESHOLD;
}

/**
 * A pessoa está impedida de agir no produto agora?
 *
 * `warning` não bloqueia nada — é aviso. `suspended` bloqueia até `untilMs`.
 * `banned` bloqueia para sempre.
 */
export function isBlocked(
  moderation: {level?: unknown; untilMs?: unknown} | undefined,
  nowMs: number,
): boolean {
  const level = moderation?.level;
  if (level === "banned") return true;
  if (level !== "suspended") return false;

  const until = typeof moderation?.untilMs === "number" ? moderation.untilMs : null;
  // Suspensão sem prazo é tratada como ativa: o campo faltando é um bug de
  // escrita, e destravar a pessoa por causa dele seria o erro mais caro.
  return until === null || until > nowMs;
}

/** Erro padronizado quando alguém bloqueado tenta agir. */
export function blockedError(): HttpsError {
  return new HttpsError("permission-denied", "Your account is restricted.");
}

export const MODERATION_LEVELS: readonly ModerationLevel[] = [
  "none",
  "warning",
  "suspended",
  "banned",
];

export function isModerationLevel(value: unknown): value is ModerationLevel {
  return typeof value === "string" && (MODERATION_LEVELS as readonly string[]).includes(value);
}

export type ManualModerationState = {
  level: ModerationLevel;
  untilMs: number | null;
  isBanned: boolean;
  requiresReview: boolean;
};

/**
 * O estado que uma decisão humana grava.
 *
 * Difere do automático em três pontos, todos de propósito:
 * - `requiresReview` sempre sai `false`: alguém acabou de revisar. Se ficasse
 *   ligado, a conta voltaria para a fila do painel para sempre.
 * - `isBanned` espelha o nível para `profiles/{uid}`, que é onde a regra
 *   `isBannedUser()` e o filtro da busca de jogadores olham.
 * - `none` limpa tudo, inclusive o prazo — é como se desfaz uma punição.
 *
 * @param days duração da suspensão; ignorado nos outros níveis
 */
export function manualModerationState(
  level: ModerationLevel,
  days: number | null,
  nowMs: number,
): ManualModerationState {
  const suspensionDays = typeof days === "number" && days > 0 ? days : SUSPENSION_DAYS;

  return {
    level,
    untilMs: level === "suspended" ? nowMs + suspensionDays * DAY_IN_MILLIS : null,
    isBanned: level === "banned",
    requiresReview: false,
  };
}

/**
 * Dimensões de uma avaliação pós-partida, além da nota geral.
 *
 * Obrigatórias. Havia uma versão que as aceitava ausentes, para conviver com um
 * cliente anterior — que nunca existiu em produção. Manter o ramo opcional
 * significaria carregar para sempre duas formas de avaliação e um perfil onde
 * metade das dimensões tem contagem e a outra metade não.
 */
export const RATING_DIMENSIONS = ["punctuality", "respect", "fairPlay", "behavior"] as const;

export type RatingDimension = (typeof RATING_DIMENSIONS)[number];

/**
 * Média corrente depois de somar uma nota.
 *
 * Perfis nascem com `rating: 0` e `ratingCount: 0`, então a matemática já dá o
 * resultado certo na primeira avaliação e não existe caso especial. A guarda de
 * `previousCount <= 0` sobrou só contra contador corrompido — não é regra de
 * negócio. (Antes a semente era `5`, um placeholder de exibição que, se
 * entrasse na conta, transformava a primeira nota 1 de alguém em 3.)
 *
 * Quem exibe é que decide o que mostrar sem avaliação nenhuma: média 0 com
 * contagem 0 significa "ainda não avaliado", não "péssimo".
 */
export function nextRatingAverage(
  previousAverage: number,
  previousCount: number,
  value: number,
  decimals = 2,
): number {
  if (previousCount <= 0) return value;

  const factor = 10 ** decimals;
  return Math.round(((previousAverage * previousCount + value) / (previousCount + 1)) * factor) /
    factor;
}

/**
 * Lê e valida as quatro dimensões de um payload.
 *
 * Ausente e fora de 1..5 são o mesmo erro, e explícito: descartar em silêncio
 * gravaria uma avaliação pela metade e o cliente nunca saberia.
 */
export function parseRatingDimensions(
  payload: Record<string, unknown>,
  onInvalid: (dimension: RatingDimension) => never,
): Record<RatingDimension, number> {
  const parsed = {} as Record<RatingDimension, number>;

  for (const dimension of RATING_DIMENSIONS) {
    const value = payload[dimension];
    if (typeof value !== "number" || !Number.isInteger(value) || value < 1 || value > 5) {
      onInvalid(dimension);
    }
    parsed[dimension] = value as number;
  }

  return parsed;
}
