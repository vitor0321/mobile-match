"use strict";
// Trust & Safety — denúncias e moderação (Phase 6).
//
// Espelhado no cliente por `products/games/.../domain/model/ReportReason.kt` e
// `ModerationLevel.kt`. Os identificadores abaixo são o contrato: mudá-los
// invalida denúncias já gravadas, então só adicione, nunca renomeie.
Object.defineProperty(exports, "__esModule", { value: true });
exports.RATING_DIMENSIONS = exports.MODERATION_LEVELS = exports.DAY_IN_MILLIS = exports.REPORT_WINDOW_DAYS = exports.SUSPENSION_DAYS = exports.REVIEW_THRESHOLD = exports.SUSPENSION_THRESHOLD = exports.WARNING_THRESHOLD = exports.MAX_REPORT_DETAILS_LENGTH = exports.REPORT_REASONS = void 0;
exports.isReportReason = isReportReason;
exports.levelForReporterCount = levelForReporterCount;
exports.requiresHumanReview = requiresHumanReview;
exports.isBlocked = isBlocked;
exports.blockedError = blockedError;
exports.isModerationLevel = isModerationLevel;
exports.manualModerationState = manualModerationState;
exports.nextRatingAverage = nextRatingAverage;
exports.parseRatingDimensions = parseRatingDimensions;
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
exports.MODERATION_LEVELS = [
    "none",
    "warning",
    "suspended",
    "banned",
];
function isModerationLevel(value) {
    return typeof value === "string" && exports.MODERATION_LEVELS.includes(value);
}
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
function manualModerationState(level, days, nowMs) {
    const suspensionDays = typeof days === "number" && days > 0 ? days : exports.SUSPENSION_DAYS;
    return {
        level,
        untilMs: level === "suspended" ? nowMs + suspensionDays * exports.DAY_IN_MILLIS : null,
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
exports.RATING_DIMENSIONS = ["punctuality", "respect", "fairPlay", "behavior"];
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
function nextRatingAverage(previousAverage, previousCount, value, decimals = 2) {
    if (previousCount <= 0)
        return value;
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
function parseRatingDimensions(payload, onInvalid) {
    const parsed = {};
    for (const dimension of exports.RATING_DIMENSIONS) {
        const value = payload[dimension];
        if (typeof value !== "number" || !Number.isInteger(value) || value < 1 || value > 5) {
            onInvalid(dimension);
        }
        parsed[dimension] = value;
    }
    return parsed;
}
