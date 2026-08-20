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
function selectRecipients(candidates, match, maxRecipients = exports.MAX_RECIPIENTS, nowMs = Date.now()) {
    const center = { lat: match.lat, lng: match.lng };
    const recipients = [];
    for (const candidate of candidates) {
        if (candidate.userId === match.organizerId)
            continue;
        if (!isAvailableAt(candidate, nowMs))
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
/**
 * Disponível agora: o toggle ligado e a janela ainda aberta.
 *
 * `availableUntilMs` nulo é "até eu desligar", não "já venceu" — é o que o
 * toggle grava. Janela vencida vale como indisponível sem ninguém precisar
 * varrer a base para desligar o campo.
 */
function isAvailableAt(candidate, nowMs) {
    if (!candidate.isAvailable)
        return false;
    return candidate.availableUntilMs === null || candidate.availableUntilMs > nowMs;
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
function readEpochMillis(value) {
    if (typeof value === "number" && Number.isFinite(value))
        return value;
    const timestamp = value;
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
function isWaitlistPromotion(before, after) {
    if (!before || !after)
        return false;
    return before.isConfirmed === false && after.isConfirmed === true;
}
