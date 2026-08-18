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
