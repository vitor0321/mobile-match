"use strict";
// Quem recebe aviso de partida nova, e por quê.
//
// A seleção fica isolada aqui, sem Firestore, porque é a única parte com regra
// de negócio de verdade — e é a única que dá para testar sem emulador.
Object.defineProperty(exports, "__esModule", { value: true });
exports.MAX_RECIPIENTS = exports.MAX_NOTIFY_RADIUS_KM = exports.MIN_NOTIFY_RADIUS_KM = void 0;
exports.effectiveRadiusKm = effectiveRadiusKm;
exports.selectRecipients = selectRecipients;
exports.parseCandidate = parseCandidate;
exports.isWaitlistPromotion = isWaitlistPromotion;
const geo_js_1 = require("./geo.js");
/**
 * Raio mínimo de notificação (regra B4 do plano).
 *
 * Mesmo quem configurou um raio pequeno de busca é avisado de partidas até
 * 20 km: procurar e ser avisado são intenções diferentes.
 */
exports.MIN_NOTIFY_RADIUS_KM = 20;
/**
 * Teto do raio, e por consequência o tamanho da consulta por geohash.
 *
 * Espelha o limite do slider de raio no app (5-50 km). Sem teto, um perfil com
 * raio absurdo faria toda criação de partida varrer a base inteira.
 */
exports.MAX_NOTIFY_RADIUS_KM = 50;
/**
 * Teto de destinatários por partida (risco R7: custo do Firestore).
 *
 * Quando estoura, os mais distantes é que ficam de fora — por isso a lista sai
 * ordenada por distância.
 */
exports.MAX_RECIPIENTS = 60;
/**
 * Raio que vale para este jogador: nunca menos que [MIN_NOTIFY_RADIUS_KM],
 * nunca mais que [MAX_NOTIFY_RADIUS_KM].
 */
function effectiveRadiusKm(candidateRadiusKm) {
    const radius = typeof candidateRadiusKm === "number" && Number.isFinite(candidateRadiusKm)
        ? candidateRadiusKm
        : 0;
    return Math.min(Math.max(radius, exports.MIN_NOTIFY_RADIUS_KM), exports.MAX_NOTIFY_RADIUS_KM);
}
/**
 * Quem deve ser avisado da partida, do mais perto para o mais longe.
 *
 * NÃO filtra por `isAvailable` de propósito. Esse campo nasce `false` em
 * onUserCreate e nada no app o liga ainda (regra B5, o toggle "estou
 * disponível", segue pendente) — filtrar por ele hoje significaria não notificar
 * ninguém, nunca. Quando o toggle existir, este é o lugar de apertar a regra.
 */
function selectRecipients(candidates, match, maxRecipients = exports.MAX_RECIPIENTS) {
    const center = { lat: match.lat, lng: match.lng };
    const recipients = [];
    for (const candidate of candidates) {
        if (candidate.userId === match.organizerId)
            continue;
        if (!matchesSportPreference(candidate, match.sport))
            continue;
        const distance = (0, geo_js_1.distanceKm)(center, { lat: candidate.lat, lng: candidate.lng });
        if (distance > effectiveRadiusKm(candidate.radiusKm))
            continue;
        recipients.push({ userId: candidate.userId, distanceKm: distance });
    }
    // Ordena antes de cortar: se alguém tem de ficar de fora, que sejam os mais
    // distantes.
    return recipients
        .sort((a, b) => a.distanceKm - b.distanceKm)
        .slice(0, maxRecipients);
}
/** Lista de esportes vazia significa "tanto faz", não "nenhum". */
function matchesSportPreference(candidate, sport) {
    if (candidate.availableSports.length === 0)
        return true;
    return candidate.availableSports.some((preferred) => preferred.toLowerCase() === sport.toLowerCase());
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
function parseCandidate(userId, data, defaultRadiusKm) {
    if (typeof data.lat !== "number" || typeof data.lng !== "number")
        return null;
    if (!Number.isFinite(data.lat) || !Number.isFinite(data.lng))
        return null;
    return {
        userId,
        lat: data.lat,
        lng: data.lng,
        radiusKm: typeof data.radiusKm === "number" ? data.radiusKm : defaultRadiusKm,
        availableSports: Array.isArray(data.availableSports)
            ? data.availableSports.filter((sport) => typeof sport === "string")
            : [],
    };
}
/**
 * A escrita no participante representa alguém subindo da fila?
 *
 * Só interessa a transição fila -> confirmado num documento que já existia.
 * Entrar já confirmado não é promoção, e sair também não — sem essa checagem o
 * trigger dispararia "você subiu da fila" em toda escrita.
 */
function isWaitlistPromotion(before, after) {
    if (!before || !after)
        return false;
    return before.isConfirmed === false && after.isConfirmed === true;
}
