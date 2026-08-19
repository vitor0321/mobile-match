"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.onParticipantChanged = exports.onMatchCreated = exports.submitReport = exports.submitPlayerRating = exports.cancelMatch = exports.leaveMatch = exports.joinMatch = exports.exportUserData = exports.deleteAccount = exports.onUserCreate = void 0;
exports.requireEmptyPayload = requireEmptyPayload;
const app_1 = require("firebase-admin/app");
const auth_1 = require("firebase-admin/auth");
const firestore_1 = require("firebase-admin/firestore");
const functionsV1 = __importStar(require("firebase-functions/v1"));
const messaging_1 = require("firebase-admin/messaging");
const firestore_2 = require("firebase-functions/v2/firestore");
const https_1 = require("firebase-functions/v2/https");
const geo_js_1 = require("./geo.js");
const notifications_js_1 = require("./notifications.js");
const moderation_js_1 = require("./moderation.js");
(0, app_1.initializeApp)();
const db = (0, firestore_1.getFirestore)();
const REGION = "southamerica-east1";
const RECENT_AUTH_WINDOW_MILLIS = 5 * 60 * 1_000;
const DEFAULT_RADIUS_KM = 15;
// ---------------------------------------------------------------------------
// onUserCreate — Auth trigger (substitui handle_new_user() do Postgres)
//
// No cadastro de qualquer usuário: cria o perfil público, o documento privado
// isolado (telefone, Pix, geo, disponibilidade), o espelho de assinatura free e
// a Custom Claim `role: user` que a regra isAdmin() e os produtos leem.
// ---------------------------------------------------------------------------
exports.onUserCreate = functionsV1
    .region(REGION)
    .auth.user()
    .onCreate(async (user) => {
    const now = firestore_1.FieldValue.serverTimestamp();
    const profile = {
        fullName: user.displayName ?? "",
        nickname: null,
        avatarUrl: user.photoURL ?? null,
        position: null,
        level: "Livre",
        sports: [],
        city: null,
        neighborhood: null,
        rating: 5,
        ratingCount: 0,
        matchesPlayed: 0,
        isBanned: false,
        createdAt: now,
        updatedAt: now,
    };
    const privateData = {
        email: user.email ?? null,
        phone: user.phoneNumber ?? null,
        pixKey: null,
        lat: null,
        lng: null,
        geohash: null,
        radiusKm: DEFAULT_RADIUS_KM,
        isAvailable: false,
        availableUntil: null,
        availableSports: [],
        updatedAt: now,
    };
    const subscription = {
        plan: "free",
        status: "active",
        currentPeriodEnd: null,
        source: "default",
        updatedAt: now,
    };
    const batch = db.batch();
    batch.set(db.doc(`profiles/${user.uid}`), profile);
    batch.set(db.doc(`profiles/${user.uid}/private/data`), privateData);
    batch.set(db.doc(`users/${user.uid}/subscription/current`), subscription);
    await batch.commit();
    // Substitui has_role('user') do Postgres. plan é espelhado pelo webhook do
    // RevenueCat quando o organizador assina (Fase 5).
    await (0, auth_1.getAuth)().setCustomUserClaims(user.uid, { role: "user", plan: "free" });
});
// ---------------------------------------------------------------------------
// deleteAccount — Callable (invocada por products/identity)
//
// Apaga o perfil público, os dados privados e tudo sob users/{uid}. Em fases
// futuras (LGPD, §6) será estendida para limpar partidas e participações.
// ---------------------------------------------------------------------------
exports.deleteAccount = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireRecentAuthentication(request.auth?.token.auth_time);
    requireEmptyPayload(request.data);
    await Promise.all([
        db.recursiveDelete(db.doc(`profiles/${uid}`)),
        db.recursiveDelete(db.doc(`users/${uid}`)),
    ]);
    return { deleted: true };
});
function requireAuthentication(uid) {
    if (!uid)
        throw new https_1.HttpsError("unauthenticated", "Authentication is required.");
}
function requireRecentAuthentication(authTime) {
    if (typeof authTime !== "number" || Date.now() - authTime * 1_000 > RECENT_AUTH_WINDOW_MILLIS) {
        throw new https_1.HttpsError("failed-precondition", "Recent authentication is required.");
    }
}
function requireEmptyPayload(value) {
    if (typeof value !== "object" || value == null || Array.isArray(value)) {
        throw new https_1.HttpsError("invalid-argument", "Payload must be an object.");
    }
    if (Object.keys(value).length > 0) {
        throw new https_1.HttpsError("invalid-argument", "Payload must be empty.");
    }
}
// ---------------------------------------------------------------------------
// exportUserData — Callable (LGPD, direito de acesso)
//
// Devolve tudo que o produto guarda sobre quem chama, em JSON. Somente leitura:
// a contraparte destrutiva é o deleteAccount.
//
// Uma exceção deliberada: denúncias FEITAS pela pessoa saem inteiras, mas
// denúncias CONTRA ela saem sem a identidade de quem denunciou. O direito de
// acesso é sobre os dados dela; revelar o denunciante entregaria dado de
// terceiro e abriria caminho para retaliação.
// ---------------------------------------------------------------------------
const EXPORT_COLLECTION_LIMIT = 500;
exports.exportUserData = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireRecentAuthentication(request.auth?.token.auth_time);
    requireEmptyPayload(request.data);
    const [profile, privateData, moderation, ratingsReceived, notificationHistory, devices, subscription, reportsFiled, reportsAgainst, organizedMatches, participations,] = await Promise.all([
        db.doc(`profiles/${uid}`).get(),
        db.doc(`profiles/${uid}/private/data`).get(),
        db.doc(`moderation/${uid}`).get(),
        db.collection(`profiles/${uid}/ratings`).limit(EXPORT_COLLECTION_LIMIT).get(),
        db.collection(`users/${uid}/notificationHistory`).limit(EXPORT_COLLECTION_LIMIT).get(),
        db.collection(`users/${uid}/devices`).limit(EXPORT_COLLECTION_LIMIT).get(),
        db.collection(`users/${uid}/subscription`).limit(EXPORT_COLLECTION_LIMIT).get(),
        db.collection("reports").where("reporterId", "==", uid).limit(EXPORT_COLLECTION_LIMIT).get(),
        db
            .collection("reports")
            .where("reportedUserId", "==", uid)
            .limit(EXPORT_COLLECTION_LIMIT)
            .get(),
        db.collection("matches").where("organizerId", "==", uid).limit(EXPORT_COLLECTION_LIMIT).get(),
        db
            .collectionGroup("participants")
            .where("userId", "==", uid)
            .limit(EXPORT_COLLECTION_LIMIT)
            .get(),
    ]);
    return {
        exportedAtMs: Date.now(),
        userId: uid,
        profile: profile.data() ?? null,
        private: privateData.data() ?? null,
        moderation: moderation.data() ?? null,
        ratingsReceived: documents(ratingsReceived),
        notificationHistory: documents(notificationHistory),
        devices: documents(devices),
        subscription: documents(subscription),
        reportsFiled: documents(reportsFiled),
        reportsAgainst: documents(reportsAgainst).map(redactReporter),
        organizedMatches: documents(organizedMatches),
        participations: documents(participations),
    };
});
function documents(snapshot) {
    return snapshot.docs.map((document) => ({ id: document.id, ...document.data() }));
}
/** Tira do export quem denunciou — é dado de terceiro, não da pessoa. */
function redactReporter(report) {
    const { reporterId, ...rest } = report;
    void reporterId;
    return rest;
}
// ---------------------------------------------------------------------------
// joinMatch — Callable (invocada por products/games)
//
// Substitui a lógica client-side de joinGame. Regras de negócio:
//   B2 (slot logic): left = max(totalSlots - confirmedCount, 0). Sem vaga →
//     entra na fila (waitlist) na próxima posição.
//   B6 (organizer auto-join): organizador já conta como 1 confirmed.
//   Validações: partida OPEN, não cancelada, slots >= 2 e <= 40, startsAt no
//     futuro, usuário ainda não é participante.
//
// Retorna {status: "confirmed" | "waitlist", position?: number, matchId}.
// ---------------------------------------------------------------------------
exports.joinMatch = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const data = request.data;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "matchId is required.");
    }
    const matchId = data.matchId;
    return db.runTransaction(async (txn) => {
        // Entrar em partida nova é exatamente o que uma conta restrita não pode
        // fazer. Sair e cancelar continuam liberados de propósito: bloquear a
        // saída prenderia a pessoa segurando uma vaga.
        await requireNotBlocked(txn, uid, Date.now());
        const matchRef = db.doc(`matches/${matchId}`);
        const participantRef = db.doc(`matches/${matchId}/participants/${uid}`);
        const matchSnap = await txn.get(matchRef);
        if (!matchSnap.exists) {
            throw new https_1.HttpsError("not-found", "Match not found.");
        }
        const match = matchSnap.data() ?? {};
        const status = String(match.status ?? "OPEN");
        if (status === "CANCELLED" || status === "FINISHED") {
            throw new https_1.HttpsError("failed-precondition", `Cannot join match in status ${status}.`);
        }
        const startsAt = readEpochSeconds(match.startsAtSeconds ?? match.startsAt);
        if (startsAt !== null && startsAt * 1000 < Date.now()) {
            throw new https_1.HttpsError("failed-precondition", "Match has already started.");
        }
        const totalSlots = Number(match.totalSlots ?? 0);
        const confirmedCount = Number(match.confirmedCount ?? 0);
        if (totalSlots < 2 || totalSlots > 40) {
            throw new https_1.HttpsError("failed-precondition", "Match slot range is invalid.");
        }
        const participants = Array.isArray(match.participants)
            ? match.participants.filter((x) => typeof x === "string")
            : [];
        // Already in (organizer or another participant) → idempotent return.
        if (participants.includes(uid)) {
            const isConfirmed = confirmedCount > 0 && participants.indexOf(uid) < confirmedCount;
            return isConfirmed
                ? { status: "confirmed", matchId }
                : { status: "already_joined", matchId };
        }
        const left = Math.max(totalSlots - confirmedCount, 0);
        const displayName = request.auth?.token?.name ?? (typeof match.organizerName === "string" ? "Jogador" : "Jogador");
        const baseParticipant = {
            userId: uid,
            displayName,
            photoUrl: null,
            joinedAt: firestore_1.FieldValue.serverTimestamp(),
            hasPaid: false,
        };
        if (left > 0) {
            // Slot available → join confirmed.
            txn.set(participantRef, {
                ...baseParticipant,
                isConfirmed: true,
                positionInWaitlist: null,
            });
            txn.update(matchRef, {
                confirmedCount: firestore_1.FieldValue.increment(1),
                participants: firestore_1.FieldValue.arrayUnion(uid),
                status: confirmedCount + 1 >= totalSlots ? "FULL" : "OPEN",
                updatedAt: firestore_1.FieldValue.serverTimestamp(),
            });
            return { status: "confirmed", matchId };
        }
        else {
            // Full → push to waitlist. Position = current waitlist size + 1.
            const waitlistSnapshot = await txn.get(db.collection(`matches/${matchId}/participants`).where("isConfirmed", "==", false));
            const position = waitlistSnapshot.size + 1;
            txn.set(participantRef, {
                ...baseParticipant,
                isConfirmed: false,
                positionInWaitlist: position,
            });
            txn.update(matchRef, {
                participants: firestore_1.FieldValue.arrayUnion(uid),
                updatedAt: firestore_1.FieldValue.serverTimestamp(),
            });
            return { status: "waitlist", matchId, position };
        }
    });
});
// ---------------------------------------------------------------------------
// leaveMatch — Callable (invocada por products/games)
//
// Remove o usuário da partida. Se ele era confirmado, decrementa confirmedCount
// e **promove automaticamente o primeiro da fila (FIFO)** — regra B3 do plano
// de migração (substitui o trigger `promote_waitlist()` do Postgres).
//
// Retorna {matchId, promotedUserId?} — promotedUserId é o uid que subiu da
// fila, se houver, para que o app possa mostrar o banner localmente.
// ---------------------------------------------------------------------------
exports.leaveMatch = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const data = request.data;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "matchId is required.");
    }
    const matchId = data.matchId;
    return db.runTransaction(async (txn) => {
        const matchRef = db.doc(`matches/${matchId}`);
        const matchSnap = await txn.get(matchRef);
        if (!matchSnap.exists) {
            throw new https_1.HttpsError("not-found", "Match not found.");
        }
        const match = matchSnap.data() ?? {};
        const status = String(match.status ?? "OPEN");
        if (status === "CANCELLED" || status === "FINISHED") {
            throw new https_1.HttpsError("failed-precondition", `Cannot leave match in status ${status}.`);
        }
        const participants = Array.isArray(match.participants)
            ? match.participants.filter((x) => typeof x === "string")
            : [];
        if (!participants.includes(uid)) {
            throw new https_1.HttpsError("not-found", "You are not in this match.");
        }
        // Organizer cannot leave — must cancel instead.
        if (match.organizerId === uid) {
            throw new https_1.HttpsError("failed-precondition", "Organizer must cancel the match, not leave it.");
        }
        const myParticipantRef = db.doc(`matches/${matchId}/participants/${uid}`);
        const mySnap = await txn.get(myParticipantRef);
        const wasConfirmed = mySnap.exists ? Boolean(mySnap.data()?.isConfirmed) : false;
        // Delete participant doc + remove from array.
        txn.delete(myParticipantRef);
        txn.update(matchRef, {
            participants: firestore_1.FieldValue.arrayRemove(uid),
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        let promotedUserId;
        if (wasConfirmed) {
            // B3: promote first FIFO from waitlist inside the same transaction.
            const waitlistQuery = db
                .collection(`matches/${matchId}/participants`)
                .where("isConfirmed", "==", false)
                .orderBy("positionInWaitlist", "asc")
                .limit(1);
            const waitlistSnap = await txn.get(waitlistQuery);
            const firstWaitlist = waitlistSnap.docs[0];
            if (firstWaitlist) {
                promotedUserId = firstWaitlist.id;
                // Flip to confirmed, clear waitlist position.
                txn.update(firstWaitlist.ref, {
                    isConfirmed: true,
                    positionInWaitlist: null,
                    promotedAt: firestore_1.FieldValue.serverTimestamp(),
                });
                txn.update(matchRef, {
                    confirmedCount: firestore_1.FieldValue.increment(1),
                    status: "OPEN", // opening back up since waitlist shrank
                    updatedAt: firestore_1.FieldValue.serverTimestamp(),
                });
            }
            else {
                // No one to promote — just decrement.
                txn.update(matchRef, {
                    confirmedCount: firestore_1.FieldValue.increment(-1),
                    status: "OPEN",
                    updatedAt: firestore_1.FieldValue.serverTimestamp(),
                });
            }
        }
        return { matchId, promotedUserId };
    });
});
// ---------------------------------------------------------------------------
// cancelMatch — Callable (invocada por products/games, organizer-only)
//
// Encerra a partida: marca status=CANCELLED, remove todos os participantes.
// Apenas o organizador pode cancelar.
//
// Em fases futuras, este callable deve disparar notificações FCM para os
// participantes (placeholder reservado — Phase 6).
// ---------------------------------------------------------------------------
exports.cancelMatch = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const data = request.data;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "matchId is required.");
    }
    const matchId = data.matchId;
    return db.runTransaction(async (txn) => {
        const matchRef = db.doc(`matches/${matchId}`);
        const matchSnap = await txn.get(matchRef);
        if (!matchSnap.exists) {
            throw new https_1.HttpsError("not-found", "Match not found.");
        }
        const match = matchSnap.data() ?? {};
        if (match.organizerId !== uid) {
            throw new https_1.HttpsError("permission-denied", "Only the organizer can cancel the match.");
        }
        const status = String(match.status ?? "OPEN");
        if (status === "CANCELLED") {
            return { matchId, status: "already_cancelled" };
        }
        if (status === "FINISHED") {
            throw new https_1.HttpsError("failed-precondition", "Cannot cancel a finished match.");
        }
        // Mark the match as cancelled; keep participant docs for audit but they
        // become unreachable via the UI (status filter). Cascade delete would
        // lose history, so we leave them and let a cleanup cron handle later.
        txn.update(matchRef, {
            status: "CANCELLED",
            cancelledAt: firestore_1.FieldValue.serverTimestamp(),
            cancelledBy: uid,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return { matchId, status: "cancelled" };
    });
});
// ---------------------------------------------------------------------------
// submitPlayerRating — Callable (invocada por products/games)
//
// Fecha o ciclo de reputação. Grava a avaliação em dois lugares, na mesma
// transação:
//   matches/{matchId}/ratings/{raterUid}_{ratedUid} — registro canônico. O id
//     composto é a própria trava de unicidade: um avaliador avalia cada colega
//     no máximo uma vez por partida, sem precisar de query.
//   profiles/{ratedUid}/ratings/{mesmo id} — modelo de leitura desnormalizado.
//     A tela de perfil precisa das avaliações *recebidas* por alguém; varrer
//     matches/* por collection group para montar isso sairia caro e obrigaria a
//     abrir regra de leitura em toda a árvore de partidas.
//
// A média do perfil é recalculada aqui porque é reputação: o cliente não pode
// escrever rating/ratingCount (ver firestore.rules).
//
// Retorna {status: "recorded" | "already_rated", averageRating, ratingCount}.
// ---------------------------------------------------------------------------
const MAX_RATING_COMMENT_LENGTH = 500;
const RATING_AVERAGE_DECIMALS = 2;
exports.submitPlayerRating = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const { matchId, ratedUserId, rating, comment } = parseSubmitRatingPayload(request.data, uid);
    return db.runTransaction(async (txn) => {
        // Uma conta restrita não mexe na reputação de ninguém.
        await requireNotBlocked(txn, uid, Date.now());
        const ratingId = `${uid}_${ratedUserId}`;
        const matchRef = db.doc(`matches/${matchId}`);
        const matchRatingRef = db.doc(`matches/${matchId}/ratings/${ratingId}`);
        const ratedProfileRef = db.doc(`profiles/${ratedUserId}`);
        // Todas as leituras antes de qualquer escrita — exigência da transação.
        // getAll é a forma documentada de ler vários documentos de uma vez dentro
        // de uma transação, em vez de encadear txn.get.
        const [matchSnap, existingSnap, ratedProfileSnap] = await txn.getAll(matchRef, matchRatingRef, ratedProfileRef);
        if (!matchSnap.exists) {
            throw new https_1.HttpsError("not-found", "Match not found.");
        }
        const match = matchSnap.data() ?? {};
        const status = String(match.status ?? "OPEN");
        if (status === "CANCELLED") {
            throw new https_1.HttpsError("failed-precondition", "Cannot rate a cancelled match.");
        }
        requireMatchIsOver(match);
        const participants = Array.isArray(match.participants)
            ? match.participants.filter((x) => typeof x === "string")
            : [];
        if (!participants.includes(uid)) {
            throw new https_1.HttpsError("permission-denied", "Only participants can rate this match.");
        }
        if (!participants.includes(ratedUserId)) {
            throw new https_1.HttpsError("failed-precondition", "The rated user did not play this match.");
        }
        if (!ratedProfileSnap.exists) {
            throw new https_1.HttpsError("not-found", "Rated player profile not found.");
        }
        const ratedProfile = ratedProfileSnap.data() ?? {};
        const previousCount = Number(ratedProfile.ratingCount ?? 0);
        const previousAverage = Number(ratedProfile.rating ?? 0);
        // Idempotente, como joinMatch/cancelMatch: reenviar não infla a média.
        if (existingSnap.exists) {
            return {
                status: "already_rated",
                matchId,
                ratedUserId,
                averageRating: previousAverage,
                ratingCount: previousCount,
            };
        }
        const nextCount = previousCount + 1;
        // Perfis nascem com rating 5 e ratingCount 0 (ver onUserCreate). Esse 5 é
        // um placeholder de exibição, não uma avaliação: não pode entrar na média.
        const nextAverage = previousCount === 0
            ? rating
            : roundTo((previousAverage * previousCount + rating) / nextCount, RATING_AVERAGE_DECIMALS);
        const now = Date.now();
        const ratingDocument = {
            matchId,
            ratedUserId,
            raterUserId: uid,
            rating,
            comment,
            // Número, não Timestamp: atravessa o interop Android/iOS sem conversão e
            // serve direto como cursor startAfter na paginação de avaliações.
            createdAtMs: now,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
        };
        txn.set(matchRatingRef, ratingDocument);
        txn.set(db.doc(`profiles/${ratedUserId}/ratings/${ratingId}`), ratingDocument);
        txn.update(ratedProfileRef, {
            rating: nextAverage,
            ratingCount: nextCount,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return {
            status: "recorded",
            matchId,
            ratedUserId,
            averageRating: nextAverage,
            ratingCount: nextCount,
        };
    });
});
function parseSubmitRatingPayload(value, uid) {
    const data = (value ?? {});
    if (typeof data.matchId !== "string" || data.matchId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "matchId is required.");
    }
    if (typeof data.ratedUserId !== "string" || data.ratedUserId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "ratedUserId is required.");
    }
    if (data.ratedUserId === uid) {
        throw new https_1.HttpsError("failed-precondition", "You cannot rate yourself.");
    }
    if (typeof data.rating !== "number" || !Number.isInteger(data.rating) || data.rating < 1 || data.rating > 5) {
        throw new https_1.HttpsError("invalid-argument", "rating must be an integer between 1 and 5.");
    }
    const comment = typeof data.comment === "string" ? data.comment.trim() : "";
    if (comment.length > MAX_RATING_COMMENT_LENGTH) {
        throw new https_1.HttpsError("invalid-argument", `comment must be at most ${MAX_RATING_COMMENT_LENGTH} characters.`);
    }
    return { matchId: data.matchId, ratedUserId: data.ratedUserId, rating: data.rating, comment };
}
/**
 * Avaliação é pós-partida. Não dá para exigir status FINISHED porque nada marca
 * esse status ainda — então a verdade é o relógio: início + duração no passado.
 */
function requireMatchIsOver(match) {
    const startsAt = readEpochSeconds(match.startsAtSeconds ?? match.startsAt);
    if (startsAt === null) {
        throw new https_1.HttpsError("failed-precondition", "Match has no start time.");
    }
    const durationMin = Number(match.durationMin ?? 0);
    const endsAtMillis = (startsAt + Math.max(durationMin, 0) * 60) * 1_000;
    if (endsAtMillis > Date.now()) {
        throw new https_1.HttpsError("failed-precondition", "Match has not finished yet.");
    }
}
function roundTo(value, decimals) {
    const factor = 10 ** decimals;
    return Math.round(value * factor) / factor;
}
// ---------------------------------------------------------------------------
// submitReport — Callable (invocada por products/games)
//
// Denunciar alguém com quem se jogou. Duas travas contra abuso, ambas
// estruturais e não configuráveis:
//   1. Denunciante e denunciado precisam estar na mesma partida. Não dá para
//      denunciar um estranho.
//   2. O id do documento é {matchId}_{reporter}_{reported}, então uma pessoa
//      conta no máximo uma vez por partida contra a mesma pessoa.
//
// Depois de gravar, reavalia moderation/{reportedUserId} contando
// DENUNCIANTES DISTINTOS na janela recente — ver ./moderation.ts para os
// limiares e para por que o banimento não é automático.
// ---------------------------------------------------------------------------
exports.submitReport = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const { matchId, reportedUserId, reason, details } = parseReportPayload(request.data, uid);
    const nowMs = Date.now();
    const windowStartMs = nowMs - moderation_js_1.REPORT_WINDOW_DAYS * moderation_js_1.DAY_IN_MILLIS;
    return db.runTransaction(async (txn) => {
        const reportId = `${matchId}_${uid}_${reportedUserId}`;
        const matchRef = db.doc(`matches/${matchId}`);
        const reportRef = db.doc(`reports/${reportId}`);
        const moderationRef = db.doc(`moderation/${reportedUserId}`);
        // Denúncia vinda de conta restrita costuma ser retaliação.
        await requireNotBlocked(txn, uid, nowMs);
        const [matchSnap, existingSnap, moderationSnap] = await txn.getAll(matchRef, reportRef, moderationRef);
        if (!matchSnap.exists) {
            throw new https_1.HttpsError("not-found", "Match not found.");
        }
        const participants = Array.isArray(matchSnap.data()?.participants)
            ? (matchSnap.data()?.participants).filter((x) => typeof x === "string")
            : [];
        if (!participants.includes(uid)) {
            throw new https_1.HttpsError("permission-denied", "Only participants can report in this match.");
        }
        if (!participants.includes(reportedUserId)) {
            throw new https_1.HttpsError("failed-precondition", "The reported user did not play this match.");
        }
        const moderation = moderationSnap.data();
        // Idempotente, como as outras callables: reenviar não conta de novo.
        if (existingSnap.exists) {
            return {
                status: "already_reported",
                reportId,
                moderationLevel: moderation?.level ?? "none",
            };
        }
        // Denúncias recentes contra a mesma pessoa, lidas ainda na fase de leitura.
        const recentReports = await txn.get(db
            .collection("reports")
            .where("reportedUserId", "==", reportedUserId)
            .where("createdAtMs", ">=", windowStartMs));
        const reporters = new Set([uid]);
        for (const document of recentReports.docs) {
            const reporterId = document.data().reporterId;
            if (typeof reporterId === "string")
                reporters.add(reporterId);
        }
        const distinctReporters = reporters.size;
        txn.set(reportRef, {
            reporterId: uid,
            reportedUserId,
            matchId,
            reason,
            details,
            status: "open",
            createdAtMs: nowMs,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
        });
        const nextLevel = applyModeration({
            txn,
            moderationRef,
            current: moderation,
            distinctReporters,
            nowMs,
            reportedUserId,
        });
        return { status: "recorded", reportId, moderationLevel: nextLevel };
    });
});
function parseReportPayload(value, uid) {
    const data = (value ?? {});
    if (typeof data.matchId !== "string" || data.matchId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "matchId is required.");
    }
    if (typeof data.reportedUserId !== "string" || data.reportedUserId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "reportedUserId is required.");
    }
    if (data.reportedUserId === uid) {
        throw new https_1.HttpsError("failed-precondition", "You cannot report yourself.");
    }
    if (!(0, moderation_js_1.isReportReason)(data.reason)) {
        throw new https_1.HttpsError("invalid-argument", "reason is not a known report reason.");
    }
    const details = typeof data.details === "string" ? data.details.trim() : "";
    if (details.length > moderation_js_1.MAX_REPORT_DETAILS_LENGTH) {
        throw new https_1.HttpsError("invalid-argument", `details must be at most ${moderation_js_1.MAX_REPORT_DETAILS_LENGTH} characters.`);
    }
    return { matchId: data.matchId, reportedUserId: data.reportedUserId, reason: data.reason, details };
}
/**
 * Escreve o novo estado de moderação e devolve o nível resultante.
 *
 * Nunca rebaixa um `banned`: esse nível só vem de decisão humana, e uma
 * recontagem automática não pode desfazê-la.
 */
function applyModeration(input) {
    const { txn, moderationRef, current, distinctReporters, nowMs } = input;
    if (current?.level === "banned")
        return "banned";
    const level = (0, moderation_js_1.levelForReporterCount)(distinctReporters);
    if (level === "none")
        return "none";
    const untilMs = level === "suspended" ? nowMs + moderation_js_1.SUSPENSION_DAYS * moderation_js_1.DAY_IN_MILLIS : null;
    const history = Array.isArray(current?.history) ? current.history : [];
    txn.set(moderationRef, {
        level,
        untilMs,
        distinctReporters,
        requiresReview: (0, moderation_js_1.requiresHumanReview)(distinctReporters),
        reason: "automatic_report_threshold",
        updatedAtMs: nowMs,
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
        history: [...history, { level, distinctReporters, atMs: nowMs }].slice(-MAX_HISTORY_ENTRIES),
    }, { merge: true });
    return level;
}
const MAX_HISTORY_ENTRIES = 20;
/**
 * Recusa a ação quando a conta está banida ou com suspensão ativa.
 *
 * Roda dentro da transação de quem chama, junto das outras leituras — a decisão
 * precisa ver o mesmo instante do resto da operação.
 */
async function requireNotBlocked(txn, uid, nowMs) {
    const snapshot = await txn.get(db.doc(`moderation/${uid}`));
    if ((0, moderation_js_1.isBlocked)(snapshot.data(), nowMs))
        throw (0, moderation_js_1.blockedError)();
}
// ---------------------------------------------------------------------------
// onMatchCreated — Trigger (matches/{matchId} criado)
//
// Avisa quem está por perto que abriu partida nova. A consulta é por faixas de
// geohash sobre profiles/{uid}/private/data (collection group `private`), com o
// teto de MAX_NOTIFY_RADIUS_KM; o recorte fino por distância e por raio de cada
// jogador acontece em selectRecipients.
//
// A escrita no histórico é em lote e o push é best-effort: falhar em entregar
// notificação não pode derrubar a criação da partida.
// ---------------------------------------------------------------------------
exports.onMatchCreated = (0, firestore_2.onDocumentCreated)({ region: REGION, document: "matches/{matchId}" }, async (event) => {
    const match = event.data?.data();
    if (!match)
        return;
    const lat = typeof match.lat === "number" ? match.lat : null;
    const lng = typeof match.lng === "number" ? match.lng : null;
    const organizerId = typeof match.organizerId === "string" ? match.organizerId : "";
    if (lat === null || lng === null || organizerId === "")
        return;
    const candidates = await findNearbyCandidates({ lat, lng });
    const recipients = (0, notifications_js_1.selectRecipients)(candidates, {
        matchId: event.params.matchId,
        organizerId,
        sport: String(match.sport ?? ""),
        lat,
        lng,
    });
    if (recipients.length === 0)
        return;
    const title = "Partida nova perto de você";
    const body = [match.sport, match.venue, match.neighborhood]
        .filter((part) => typeof part === "string" && part.length > 0)
        .join(" · ");
    await writeNotifications(recipients.map((recipient) => recipient.userId), { type: "new_match", title, body, matchId: event.params.matchId });
});
// ---------------------------------------------------------------------------
// onParticipantChanged — Trigger (matches/{matchId}/participants/{uid} escrito)
//
// Avisa quem subiu da fila. A promoção em si já acontece na transação do
// leaveMatch (regra B3) — aqui só entra a notificação, para que ela também
// alcance quem estava com o app fechado.
// ---------------------------------------------------------------------------
exports.onParticipantChanged = (0, firestore_2.onDocumentWritten)({ region: REGION, document: "matches/{matchId}/participants/{participantId}" }, async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!(0, notifications_js_1.isWaitlistPromotion)(before, after))
        return;
    const matchId = event.params.matchId;
    const matchSnap = await db.doc(`matches/${matchId}`).get();
    const match = matchSnap.data() ?? {};
    const body = [match.sport, match.venue]
        .filter((part) => typeof part === "string" && part.length > 0)
        .join(" · ");
    await writeNotifications([event.params.participantId], {
        type: "promoted",
        title: "Você subiu da fila!",
        body,
        matchId,
    });
});
/**
 * Jogadores dentro do raio máximo, lidos por faixa de geohash.
 *
 * Cada faixa é uma consulta; o `boundsForRadius` já devolve o menor conjunto de
 * faixas que cobre a caixa em volta do ponto.
 */
async function findNearbyCandidates(center) {
    const ranges = (0, geo_js_1.boundsForRadius)(center, notifications_js_1.MAX_NOTIFY_RADIUS_KM);
    const snapshots = await Promise.all(ranges.map((range) => db
        .collectionGroup("private")
        .orderBy("geohash")
        .startAt(range.start)
        .endAt(range.endInclusive)
        .get()));
    const byUserId = new Map();
    for (const snapshot of snapshots) {
        for (const document of snapshot.docs) {
            // profiles/{uid}/private/data -> o uid é o avô do documento.
            const userId = document.ref.parent.parent?.id;
            if (!userId)
                continue;
            const candidate = (0, notifications_js_1.parseCandidate)(userId, document.data(), DEFAULT_RADIUS_KM);
            // Faixas de geohash podem se sobrepor; o Map deduplica.
            if (candidate)
                byUserId.set(userId, candidate);
        }
    }
    return [...byUserId.values()];
}
/**
 * Grava o histórico de todos os destinatários e dispara o push.
 *
 * O histórico é a fonte da verdade: o push pode ser negado, o token pode estar
 * morto, o aparelho pode estar offline. Por isso a entrega falha em silêncio e
 * o item continua lá para o app mostrar quando abrir.
 */
async function writeNotifications(userIds, payload) {
    const nowMs = Date.now();
    const batch = db.batch();
    for (const userId of userIds) {
        batch.set(db.collection(`users/${userId}/notificationHistory`).doc(), {
            type: payload.type,
            title: payload.title,
            body: payload.body,
            data: { matchId: payload.matchId },
            isRead: false,
            receivedAt: nowMs,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
        });
    }
    await batch.commit();
    await sendPush(userIds, payload);
}
async function sendPush(userIds, payload) {
    const tokenSnapshots = await Promise.all(userIds.map((userId) => db.collection(`users/${userId}/devices`).get()));
    const tokens = tokenSnapshots
        .flatMap((snapshot) => snapshot.docs.map((document) => document.id))
        .filter((token) => token.length > 0);
    if (tokens.length === 0)
        return;
    try {
        await (0, messaging_1.getMessaging)().sendEachForMulticast({
            tokens,
            notification: { title: payload.title, body: payload.body },
            // Só strings: o FCM recusa qualquer outro tipo no data payload.
            data: { type: payload.type, matchId: payload.matchId },
        });
    }
    catch (error) {
        // Notificação é acessório. Uma falha de entrega não pode propagar para o
        // trigger e provocar retentativa da escrita já feita.
        console.error("push delivery failed", error);
    }
}
function readEpochSeconds(value) {
    if (typeof value === "number")
        return value;
    if (value && typeof value === "object" && "seconds" in value && typeof value.seconds === "number") {
        return value.seconds;
    }
    return null;
}
