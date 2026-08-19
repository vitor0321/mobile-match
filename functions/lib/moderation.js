"use strict";
// Trust & Safety — denúncias e moderação (Phase 6).
//
// Espelhado no cliente por `products/games/.../domain/model/ReportReason.kt` e
// `ModerationLevel.kt`. Os identificadores abaixo são o contrato: mudá-los
// invalida denúncias já gravadas, então só adicione, nunca renomeie.
Object.defineProperty(exports, "__esModule", { value: true });
exports.DAY_IN_MILLIS = exports.REPORT_WINDOW_DAYS = exports.SUSPENSION_DAYS = exports.REVIEW_THRESHOLD = exports.SUSPENSION_THRESHOLD = exports.WARNING_THRESHOLD = exports.MAX_REPORT_DETAILS_LENGTH = exports.REPORT_REASONS = void 0;
exports.isReportReason = isReportReason;
exports.levelForReporterCount = levelForReporterCount;
exports.requiresHumanReview = requiresHumanReview;
exports.isBlocked = isBlocked;
exports.blockedError = blockedError;
const https_1 = require("firebase-functions/v2/https");
/**
 * Os dez motivos de denúncia.
 *
 * `other` é obrigatório na lista: sem uma saída genérica as pessoas escolhem o
 * motivo errado só para conseguir denunciar, e a estatística vira ruído.
 */
exports.REPORT_REASONS = [
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
];
function isReportReason(value) {
    return typeof value === "string" && exports.REPORT_REASONS.includes(value);
}
exports.MAX_REPORT_DETAILS_LENGTH = 1_000;
/**
 * Quantos **denunciantes distintos** disparam cada nível.
 *
 * Contar denúncias cruas deixaria uma pessoa sozinha derrubar outra: bastaria
 * abrir dez denúncias. O que conta é quantas pessoas diferentes reclamaram.
 */
exports.WARNING_THRESHOLD = 3;
exports.SUSPENSION_THRESHOLD = 6;
exports.REVIEW_THRESHOLD = 10;
/** Duração da suspensão automática. */
exports.SUSPENSION_DAYS = 14;
/**
 * Janela de contagem.
 *
 * Sem ela a punição é permanente na prática: denúncias de dois anos atrás
 * continuariam somando e ninguém jamais sairia de uma suspensão.
 */
exports.REPORT_WINDOW_DAYS = 180;
exports.DAY_IN_MILLIS = 24 * 60 * 60 * 1_000;
/**
 * O banimento **não** é automático.
 *
 * Banir por contagem de denúncias é um vetor de brigading: um grupo coordenado
 * elimina qualquer jogador. Ao atingir [REVIEW_THRESHOLD] a conta é suspensa e
 * marcada para revisão; só um admin (custom claim) grava `banned`.
 */
function levelForReporterCount(distinctReporters) {
    if (distinctReporters >= exports.SUSPENSION_THRESHOLD)
        return "suspended";
    if (distinctReporters >= exports.WARNING_THRESHOLD)
        return "warning";
    return "none";
}
function requiresHumanReview(distinctReporters) {
    return distinctReporters >= exports.REVIEW_THRESHOLD;
}
/**
 * A pessoa está impedida de agir no produto agora?
 *
 * `warning` não bloqueia nada — é aviso. `suspended` bloqueia até `untilMs`.
 * `banned` bloqueia para sempre.
 */
function isBlocked(moderation, nowMs) {
    const level = moderation?.level;
    if (level === "banned")
        return true;
    if (level !== "suspended")
        return false;
    const until = typeof moderation?.untilMs === "number" ? moderation.untilMs : null;
    // Suspensão sem prazo é tratada como ativa: o campo faltando é um bug de
    // escrita, e destravar a pessoa por causa dele seria o erro mais caro.
    return until === null || until > nowMs;
}
/** Erro padronizado quando alguém bloqueado tenta agir. */
function blockedError() {
    return new https_1.HttpsError("permission-denied", "Your account is restricted.");
}
