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
exports.generateRecurringMatches = exports.cancelMatchSeries = exports.onParticipantChanged = exports.onMatchCreated = exports.submitReport = exports.submitMatchRating = exports.submitPlayerRating = exports.cancelMatch = exports.leaveMatch = exports.joinMatch = exports.exportUserData = exports.syncVerificationStatus = exports.adminSetModeration = exports.deleteAccount = exports.onUserCreate = void 0;
exports.requireEmptyPayload = requireEmptyPayload;
exports.shouldCancelOnOrganizerDeletion = shouldCancelOnOrganizerDeletion;
const app_1 = require("firebase-admin/app");
const auth_1 = require("firebase-admin/auth");
const firestore_1 = require("firebase-admin/firestore");
const functionsV1 = __importStar(require("firebase-functions/v1"));
const messaging_1 = require("firebase-admin/messaging");
const firestore_2 = require("firebase-functions/v2/firestore");
const https_1 = require("firebase-functions/v2/https");
const scheduler_1 = require("firebase-functions/v2/scheduler");
const geo_js_1 = require("./geo.js");
const notifications_js_1 = require("./notifications.js");
const verification_js_1 = require("./verification.js");
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
        rating: 0,
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
        // Nasce disponível de propósito. `selectRecipients` filtra por este campo
        // (regra B5), então `false` no cadastro significaria que quem se inscreve
        // e nunca abre o perfil não recebe aviso de partida nenhuma — o produto
        // vive de avisar sobre vaga, e um padrão que cala é pior do que um que
        // incomoda. Desligar é um toque no switch do perfil.
        //
        // `availableUntil: null` é "até eu desligar"; sem coordenada ninguém é
        // notificado de qualquer jeito (parseCandidate descarta), então isto não
        // dispara nada antes da pessoa permitir localização.
        isAvailable: true,
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
    // A ordem importa. Sair das partidas primeiro, porque isso lê o perfil e
    // libera vagas; apagar o usuário do Auth por último, para que uma falha no
    // meio deixe a pessoa capaz de repetir a chamada.
    await leaveAllMatches(uid);
    await cleanUpOrganizedMatches(uid);
    await anonymizeAuthoredContent(uid);
    await deleteModerationTrail(uid);
    await Promise.all([
        db.recursiveDelete(db.doc(`profiles/${uid}`)),
        db.recursiveDelete(db.doc(`users/${uid}`)),
    ]);
    await (0, auth_1.getAuth)()
        .deleteUser(uid)
        // Repetir a exclusão não pode falhar: o usuário já ter sumido é sucesso.
        .catch(() => undefined);
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
// adminSetModeration — Callable (painel de moderação)
//
// A decisão humana que o escalonamento automático nunca toma: banir, e também
// desfazer. Exige a custom claim `admin`.
//
// Existe porque firestore.rules nega escrita em moderation/{uid} até para
// admin: o mesmo movimento precisa espelhar `isBanned` no perfil, que é onde a
// regra de criar partida e o filtro da busca olham. Duas escritas que têm de
// andar juntas não podem sair do cliente.
// ---------------------------------------------------------------------------
exports.adminSetModeration = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    if (request.auth?.token.admin !== true) {
        throw new https_1.HttpsError("permission-denied", "Admin only.");
    }
    const data = (request.data ?? {});
    if (typeof data.userId !== "string" || data.userId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "userId is required.");
    }
    if (!(0, moderation_js_1.isModerationLevel)(data.level)) {
        throw new https_1.HttpsError("invalid-argument", "level is not a known moderation level.");
    }
    // Um admin se banindo por engano perderia o acesso ao próprio painel.
    if (data.userId === uid) {
        throw new https_1.HttpsError("failed-precondition", "You cannot moderate your own account.");
    }
    const reason = typeof data.reason === "string" ? data.reason.trim() : "";
    if (reason.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "reason is required.");
    }
    const nowMs = Date.now();
    const state = (0, moderation_js_1.manualModerationState)(data.level, typeof data.days === "number" ? data.days : null, nowMs);
    await db.runTransaction(async (txn) => {
        const moderationRef = db.doc(`moderation/${data.userId}`);
        const profileRef = db.doc(`profiles/${data.userId}`);
        const [current, profile] = await txn.getAll(moderationRef, profileRef);
        if (!profile.exists) {
            throw new https_1.HttpsError("not-found", "Profile not found.");
        }
        const history = Array.isArray(current.data()?.history) ? current.data().history : [];
        txn.set(moderationRef, {
            level: state.level,
            untilMs: state.untilMs,
            requiresReview: state.requiresReview,
            reason,
            decidedBy: uid,
            updatedAtMs: nowMs,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
            history: [
                ...history,
                { level: state.level, atMs: nowMs, decidedBy: uid, reason },
            ].slice(-MAX_HISTORY_ENTRIES),
        }, { merge: true });
        // Espelho lido por isBannedUser() nas regras e pelo filtro da busca.
        txn.update(profileRef, { isBanned: state.isBanned, updatedAt: firestore_1.FieldValue.serverTimestamp() });
    });
    return { userId: data.userId, level: state.level, untilMs: state.untilMs };
});
// ---------------------------------------------------------------------------
// syncVerificationStatus — Callable
//
// Espelha no perfil o que o ID token já afirma. Quem verifica é o Firebase
// Auth; aqui só se copia a claim assinada para um lugar que outras pessoas
// conseguem ler, porque o selo de verificado é sinal de confiança público e
// `profiles/{uid}` é o único documento com leitura aberta a quem está logado.
//
// O app precisa forçar refresh do token antes de chamar: o `email_verified`
// entra na próxima emissão, e um token velho faria a chamada não fazer nada.
// Chamar de novo é inofensivo — a operação é idempotente.
// ---------------------------------------------------------------------------
exports.syncVerificationStatus = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireEmptyPayload(request.data);
    const status = (0, verification_js_1.verificationFromClaims)(request.auth?.token);
    await db.doc(`profiles/${uid}`).set({
        emailVerified: status.emailVerified,
        phoneVerified: status.phoneVerified,
        verificationCheckedAtMs: Date.now(),
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
    }, { merge: true });
    return status;
});
/**
 * Barra a ação quando a política exigir verificação que a conta não tem.
 *
 * Lê direto das claims do token, e não do espelho no perfil: o espelho pode
 * estar desatualizado se o app ainda não chamou `syncVerificationStatus`, e
 * recusar alguém que já verificou seria pior que o contrário.
 *
 * Com a política toda desligada (o padrão), sai antes de fazer qualquer coisa.
 */
function requireVerification(token, requirement) {
    if (!(0, verification_js_1.isEnforcementEnabled)(requirement))
        return;
    const status = (0, verification_js_1.verificationFromClaims)(token);
    if ((0, verification_js_1.meetsRequirement)(status, requirement))
        return;
    throw new https_1.HttpsError("failed-precondition", `Verification required: ${(0, verification_js_1.missingVerification)(status, requirement)}`);
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
        requireVerification(request.auth?.token, verification_js_1.VERIFICATION_POLICY.joinMatch);
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
        if (totalSlots < 1 || totalSlots > 50) {
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
        // Organizer cannot leave — must cancel instead.
        if (match.organizerId === uid) {
            throw new https_1.HttpsError("failed-precondition", "Organizer must cancel the match, not leave it.");
        }
        const participants = Array.isArray(match.participants)
            ? match.participants.filter((x) => typeof x === "string")
            : [];
        // Retry-safe: a segunda chamada de um cliente que já saiu (ex. resposta
        // perdida por timeout de rede e o withRetry do client tenta de novo)
        // não deve virar erro — o resultado desejado (estar fora da partida)
        // já foi alcançado pela primeira chamada.
        if (!participants.includes(uid)) {
            return { matchId, status: "already_left" };
        }
        const promotedUserId = await removeParticipant(txn, matchId, uid);
        return { matchId, status: "left", promotedUserId };
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
    const { matchId, ratedUserId, rating, comment, dimensions } = parseSubmitRatingPayload(request.data, uid);
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
        const nextAverage = (0, moderation_js_1.nextRatingAverage)(previousAverage, previousCount, rating, RATING_AVERAGE_DECIMALS);
        // Toda avaliação traz as quatro dimensões, então as contagens caminham
        // juntas com ratingCount — não existe perfil com metade agregada.
        const dimensionAggregates = {};
        for (const dimension of moderation_js_1.RATING_DIMENSIONS) {
            const key = `${dimension}Average`;
            dimensionAggregates[key] = (0, moderation_js_1.nextRatingAverage)(Number(ratedProfile[key] ?? 0), previousCount, dimensions[dimension], RATING_AVERAGE_DECIMALS);
        }
        const now = Date.now();
        const ratingDocument = {
            matchId,
            ratedUserId,
            raterUserId: uid,
            rating,
            ...dimensions,
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
            ...dimensionAggregates,
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
    const dimensions = (0, moderation_js_1.parseRatingDimensions)(data, (dimension) => {
        throw new https_1.HttpsError("invalid-argument", `${dimension} is required and must be an integer between 1 and 5.`);
    });
    return {
        matchId: data.matchId,
        ratedUserId: data.ratedUserId,
        rating: data.rating,
        comment,
        dimensions,
    };
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
// ---------------------------------------------------------------------------
// submitMatchRating — Callable (invocada por products/games)
//
// Irmã de submitPlayerRating, mas avalia o evento em si, não outro jogador:
// nota 1-5 por participante, um voto por pessoa por partida
// (matches/{matchId}/matchRatings/{uid}).
//
// A agregação não vive no próprio documento da partida (isso ficaria
// congelado, igual organizerRating antes desta mudança) — vive em
// matchTemplates/{autoId}, uma linha por combinação organizador+local+esporte,
// localizada por query de igualdade (nunca por id determinístico, pra não
// duplicar em TS e Kotlin a mesma lógica de slug). Partidas futuras dessa
// mesma combinação nascem já lendo esse agregado (ver
// FirestoreGameSource.createMatch no client); a partida avaliada não muda o
// próprio matchRating depois de criada.
//
// Retorna {status: "recorded" | "already_rated", averageRating, ratingCount}.
// ---------------------------------------------------------------------------
exports.submitMatchRating = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const { matchId, rating } = parseSubmitMatchRatingPayload(request.data);
    return db.runTransaction(async (txn) => {
        await requireNotBlocked(txn, uid, Date.now());
        const matchRef = db.doc(`matches/${matchId}`);
        const matchRatingRef = db.doc(`matches/${matchId}/matchRatings/${uid}`);
        const [matchSnap, existingSnap] = await txn.getAll(matchRef, matchRatingRef);
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
        const organizerId = String(match.organizerId ?? "");
        const venueName = String(match.venueName ?? "");
        const sport = String(match.sport ?? "");
        // Todas as leituras (getAll + esta query) precisam terminar antes de
        // qualquer escrita — exigência da transação.
        const templateQuery = db
            .collection("matchTemplates")
            .where("organizerId", "==", organizerId)
            .where("venueName", "==", venueName)
            .where("sport", "==", sport)
            .limit(1);
        const templateSnap = await txn.get(templateQuery);
        const templateRef = templateSnap.empty
            ? db.collection("matchTemplates").doc()
            : templateSnap.docs[0].ref;
        const templateData = templateSnap.empty ? {} : templateSnap.docs[0].data();
        const previousCount = Number(templateData.ratingCount ?? 0);
        const previousAverage = Number(templateData.rating ?? 0);
        // Idempotente, como submitPlayerRating: reenviar não infla a média.
        if (existingSnap.exists) {
            return {
                status: "already_rated",
                matchId,
                averageRating: previousAverage,
                ratingCount: previousCount,
            };
        }
        const nextCount = previousCount + 1;
        const nextAverage = (0, moderation_js_1.nextRatingAverage)(previousAverage, previousCount, rating, RATING_AVERAGE_DECIMALS);
        const now = Date.now();
        txn.set(matchRatingRef, {
            matchId,
            raterUserId: uid,
            rating,
            createdAtMs: now,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
        });
        txn.set(templateRef, {
            organizerId,
            venueName,
            sport,
            rating: nextAverage,
            ratingCount: nextCount,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        }, { merge: true });
        return {
            status: "recorded",
            matchId,
            averageRating: nextAverage,
            ratingCount: nextCount,
        };
    });
});
function parseSubmitMatchRatingPayload(value) {
    const data = (value ?? {});
    if (typeof data.matchId !== "string" || data.matchId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "matchId is required.");
    }
    if (typeof data.rating !== "number" ||
        !Number.isInteger(data.rating) ||
        data.rating < 1 ||
        data.rating > 5) {
        throw new https_1.HttpsError("invalid-argument", "rating must be an integer between 1 and 5.");
    }
    return { matchId: data.matchId, rating: data.rating };
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
// ---------------------------------------------------------------------------
// cancelMatchSeries — Callable (invocada por products/games)
//
// Botão explícito "cancelar recorrência": o organizador para a série sem
// depender do efeito colateral de cancelar a última ocorrência. Não mexe na
// ocorrência atual nem em nenhuma já criada — só marca
// matchSeries/{seriesId}.active = false, que generateRecurringMatches passa a
// checar antes de gerar a próxima. Documento nasce sob demanda: uma série sem
// esse doc está sempre ativa.
// ---------------------------------------------------------------------------
exports.cancelMatchSeries = (0, https_1.onCall)({ region: REGION }, async (request) => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const { matchId } = parseCancelMatchSeriesPayload(request.data);
    return db.runTransaction(async (txn) => {
        const matchRef = db.doc(`matches/${matchId}`);
        const matchSnap = await txn.get(matchRef);
        if (!matchSnap.exists) {
            throw new https_1.HttpsError("not-found", "Match not found.");
        }
        const match = matchSnap.data() ?? {};
        if (match.organizerId !== uid) {
            throw new https_1.HttpsError("permission-denied", "Only the organizer can cancel the recurrence.");
        }
        const seriesId = typeof match.seriesId === "string" ? match.seriesId : "";
        if (!seriesId) {
            throw new https_1.HttpsError("failed-precondition", "This match is not part of a recurring series.");
        }
        txn.set(db.doc(`matchSeries/${seriesId}`), {
            organizerId: match.organizerId,
            active: false,
            cancelledAt: firestore_1.FieldValue.serverTimestamp(),
            cancelledBy: uid,
        }, { merge: true });
        return { status: "recorded", seriesId };
    });
});
function parseCancelMatchSeriesPayload(value) {
    const data = (value ?? {});
    if (typeof data.matchId !== "string" || data.matchId.length === 0) {
        throw new https_1.HttpsError("invalid-argument", "matchId is required.");
    }
    return { matchId: data.matchId };
}
// ---------------------------------------------------------------------------
// generateRecurringMatches — Scheduled function (1x por dia)
//
// O cliente só grava a preferência de repetição (`recurrence`); nada cria a
// próxima ocorrência sozinho. Aqui, para cada série (`seriesId`), olha a
// ocorrência mais recente da série — independente do status — e só gera a
// próxima se essa mais recente ainda estiver 'OPEN' E a série não tiver sido
// explicitamente cancelada via cancelMatchSeries (matchSeries/{id}.active).
// Cancelar a última ocorrência continua parando a série pelo mesmo efeito
// colateral de sempre — o botão explícito é o jeito confiável de fazer isso
// de qualquer ocorrência, não só da mais recente.
//
// Gera no máximo uma ocorrência nova por série a cada execução, quando a
// próxima data cai dentro de ~1 mês — mantém sempre pelo menos uma ocorrência
// futura visível com essa antecedência, sem nunca criar mais de uma de vez.
// ---------------------------------------------------------------------------
const RECURRENCE_VALUES = ["DAILY", "WEEKLY", "MONTHLY", "YEARLY"];
const RECURRENCE_LEAD_TIME_SECONDS = 30 * 24 * 60 * 60; // ~1 mês de antecedência
function nextOccurrenceSeconds(startsAtSeconds, recurrence) {
    const date = new Date(startsAtSeconds * 1000);
    switch (recurrence) {
        case "DAILY":
            date.setUTCDate(date.getUTCDate() + 1);
            break;
        case "WEEKLY":
            date.setUTCDate(date.getUTCDate() + 7);
            break;
        case "MONTHLY":
            date.setUTCMonth(date.getUTCMonth() + 1);
            break;
        case "YEARLY":
            date.setUTCFullYear(date.getUTCFullYear() + 1);
            break;
        default:
            return startsAtSeconds;
    }
    return Math.floor(date.getTime() / 1000);
}
exports.generateRecurringMatches = (0, scheduler_1.onSchedule)({ schedule: "every 24 hours", region: REGION, timeZone: "America/Sao_Paulo" }, async () => {
    const nowSeconds = Math.floor(Date.now() / 1000);
    const leadThresholdSeconds = nowSeconds + RECURRENCE_LEAD_TIME_SECONDS;
    const snapshot = await db
        .collection("matches")
        .where("recurrence", "in", RECURRENCE_VALUES)
        .get();
    // Última ocorrência de cada série por data, não por status — uma ocorrência
    // cancelada precisa continuar sendo "a mais recente" para travar a série.
    const latestBySeries = new Map();
    for (const doc of snapshot.docs) {
        const data = doc.data();
        const seriesId = typeof data.seriesId === "string" ? data.seriesId : doc.id;
        const startsAtSeconds = readEpochSeconds(data.startsAtSeconds) ?? 0;
        const current = latestBySeries.get(seriesId);
        const currentStartsAtSeconds = current ? (readEpochSeconds(current.data().startsAtSeconds) ?? 0) : -1;
        if (startsAtSeconds > currentStartsAtSeconds) {
            latestBySeries.set(seriesId, doc);
        }
    }
    const writes = [];
    for (const doc of latestBySeries.values()) {
        const data = doc.data();
        if (data.status !== "OPEN")
            continue;
        const recurrence = data.recurrence;
        const startsAtSeconds = readEpochSeconds(data.startsAtSeconds);
        const organizerId = typeof data.organizerId === "string" ? data.organizerId : null;
        if (typeof recurrence !== "string" || startsAtSeconds === null || !organizerId)
            continue;
        const nextStartsAtSeconds = nextOccurrenceSeconds(startsAtSeconds, recurrence);
        if (nextStartsAtSeconds > leadThresholdSeconds)
            continue;
        const seriesId = typeof data.seriesId === "string" ? data.seriesId : doc.id;
        const seriesSnap = await db.doc(`matchSeries/${seriesId}`).get();
        if (seriesSnap.exists && seriesSnap.data()?.active === false)
            continue;
        const nextMatch = {
            sport: data.sport,
            venueName: data.venueName,
            neighborhood: data.neighborhood,
            city: data.city,
            address: data.address,
            lat: data.lat,
            lng: data.lng,
            geohash: data.geohash,
            startsAtSeconds: nextStartsAtSeconds,
            durationMin: data.durationMin,
            recurrence,
            seriesId,
            confirmedCount: 1,
            totalSlots: data.totalSlots,
            priceCents: data.priceCents,
            status: "OPEN",
            organizerName: data.organizerName,
            organizerId,
            organizerRating: data.organizerRating,
            organizerRatingCount: data.organizerRatingCount ?? 0,
            matchRating: data.matchRating ?? 0,
            matchRatingCount: data.matchRatingCount ?? 0,
            currencyCode: data.currencyCode ?? "BRL",
            participants: [organizerId],
        };
        writes.push(db.collection("matches").add(nextMatch));
    }
    await Promise.all(writes);
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
/**
 * Tira alguém da partida e promove o primeiro da fila (regra B3).
 *
 * Compartilhado entre `leaveMatch` e `deleteAccount`: sair por vontade própria
 * e sair porque a conta acabou têm de mexer no contador exatamente igual, e
 * duas cópias dessa lógica divergiriam na primeira mudança.
 *
 * Precisa rodar dentro de uma transação já iniciada — todas as leituras aqui
 * acontecem antes de qualquer escrita.
 *
 * @returns o uid promovido, se houve promoção
 */
async function removeParticipant(txn, matchId, uid) {
    const matchRef = db.doc(`matches/${matchId}`);
    const participantRef = db.doc(`matches/${matchId}/participants/${uid}`);
    const participantSnap = await txn.get(participantRef);
    const wasConfirmed = participantSnap.exists
        ? Boolean(participantSnap.data()?.isConfirmed)
        : false;
    // Quem estava na fila só sai; a vaga confirmada é que precisa de sucessor.
    const firstWaitlist = wasConfirmed ? await firstInWaitlist(txn, matchId, uid) : undefined;
    txn.delete(participantRef);
    txn.update(matchRef, {
        participants: firestore_1.FieldValue.arrayRemove(uid),
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
    });
    if (!wasConfirmed)
        return undefined;
    if (firstWaitlist) {
        txn.update(firstWaitlist.ref, {
            isConfirmed: true,
            positionInWaitlist: null,
            promotedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        // Sai um confirmado, entra um: o total não muda. Somar aqui inflava o
        // contador a cada promoção e a partida ficava "cheia" com vaga sobrando.
        txn.update(matchRef, {
            status: "OPEN",
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return firstWaitlist.id;
    }
    txn.update(matchRef, {
        confirmedCount: firestore_1.FieldValue.increment(-1),
        status: "OPEN",
        updatedAt: firestore_1.FieldValue.serverTimestamp(),
    });
    return undefined;
}
/** Primeiro da fila por ordem de entrada, ignorando quem está saindo. */
async function firstInWaitlist(txn, matchId, leavingUid) {
    const snapshot = await txn.get(db
        .collection(`matches/${matchId}/participants`)
        .where("isConfirmed", "==", false)
        .orderBy("positionInWaitlist", "asc")
        .limit(2));
    return snapshot.docs.find((document) => document.id !== leavingUid);
}
/** Documentos lidos de uma vez em cada etapa da exclusão. */
const DELETION_PAGE_SIZE = 200;
/**
 * Sai de toda partida em que a pessoa aparece.
 *
 * Uma transação por partida, e não uma só para todas: elas são independentes, e
 * uma transação gigante falharia inteira por causa de uma partida em conflito.
 * Reaproveita [removeParticipant], então a fila é promovida igual a uma saída
 * comum — a vaga não fica presa.
 */
async function leaveAllMatches(uid) {
    const participations = await db
        .collectionGroup("participants")
        .where("userId", "==", uid)
        .limit(DELETION_PAGE_SIZE)
        .get();
    for (const participation of participations.docs) {
        const matchId = participation.ref.parent.parent?.id;
        if (!matchId)
            continue;
        await db
            .runTransaction(async (txn) => {
            const matchSnap = await txn.get(db.doc(`matches/${matchId}`));
            if (!matchSnap.exists)
                return;
            // Organizador não "sai" da própria partida — ela é tratada adiante.
            if (matchSnap.data()?.organizerId === uid)
                return;
            await removeParticipant(txn, matchId, uid);
        })
            // Uma partida problemática não pode impedir o resto da exclusão.
            .catch((error) => console.error(`leaveAllMatches ${matchId}`, error));
    }
}
/**
 * Cancela o que ainda vai acontecer e despersonaliza o que já passou.
 *
 * Apagar as partidas levaria junto o histórico de todo mundo que jogou. O que
 * precisa sumir é o nome do organizador, não o registro do jogo.
 */
async function cleanUpOrganizedMatches(uid) {
    const organized = await db
        .collection("matches")
        .where("organizerId", "==", uid)
        .limit(DELETION_PAGE_SIZE)
        .get();
    if (organized.empty)
        return;
    const nowMs = Date.now();
    const batch = db.batch();
    for (const match of organized.docs) {
        const data = match.data();
        const anonymous = {
            organizerName: ANONYMOUS_NAME,
            organizerAvatarUrl: null,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        };
        if (shouldCancelOnOrganizerDeletion(data, nowMs)) {
            batch.update(match.ref, {
                ...anonymous,
                status: "CANCELLED",
                cancelledAt: firestore_1.FieldValue.serverTimestamp(),
                cancelledBy: "account_deleted",
            });
        }
        else {
            batch.update(match.ref, anonymous);
        }
    }
    await batch.commit();
}
/**
 * Partida futura sem organizador não tem como acontecer — cancela, para que
 * quem ia jogar descubra agora e não na quadra.
 */
function shouldCancelOnOrganizerDeletion(match, nowMs) {
    const status = String(match.status ?? "OPEN").toUpperCase();
    if (status === "CANCELLED" || status === "FINISHED")
        return false;
    const startsAt = readEpochSeconds(match.startsAtSeconds ?? match.startsAt);
    // Sem horário legível, cancelar é a escolha conservadora.
    if (startsAt === null)
        return true;
    return startsAt * 1_000 > nowMs;
}
const ANONYMOUS_NAME = "Jogador removido";
/**
 * Tira o nome de quem sai das avaliações e denúncias que escreveu.
 *
 * Não apaga: a avaliação também é dado de quem foi avaliado, e a denúncia é a
 * prova contra outra pessoa — apagá-la deixaria qualquer um limpar o próprio
 * rastro excluindo a conta. Como o uid está no id do documento, o único jeito
 * de removê-lo é recriar o documento com id novo.
 */
async function anonymizeAuthoredContent(uid) {
    const [ratings, reports] = await Promise.all([
        db
            .collectionGroup("ratings")
            .where("raterUserId", "==", uid)
            .limit(DELETION_PAGE_SIZE)
            .get(),
        db.collection("reports").where("reporterId", "==", uid).limit(DELETION_PAGE_SIZE).get(),
    ]);
    const batch = db.batch();
    const anonymizedAtMs = Date.now();
    for (const rating of ratings.docs) {
        batch.set(rating.ref.parent.doc(), {
            ...rating.data(),
            raterUserId: null,
            anonymizedAtMs,
        });
        batch.delete(rating.ref);
    }
    for (const report of reports.docs) {
        batch.set(report.ref.parent.doc(), {
            ...report.data(),
            reporterId: null,
            anonymizedAtMs,
        });
        batch.delete(report.ref);
    }
    if (ratings.empty && reports.empty)
        return;
    await batch.commit();
}
/**
 * Apaga o que só existia por causa desta conta: o status de moderação dela e as
 * denúncias feitas contra ela.
 *
 * Guardar isso depois que o uid deixou de existir não protege ninguém — não há
 * mais conta para restringir — e seria dado pessoal sem finalidade.
 */
async function deleteModerationTrail(uid) {
    const against = await db
        .collection("reports")
        .where("reportedUserId", "==", uid)
        .limit(DELETION_PAGE_SIZE)
        .get();
    const batch = db.batch();
    for (const report of against.docs)
        batch.delete(report.ref);
    batch.delete(db.doc(`moderation/${uid}`));
    await batch.commit();
}
function readEpochSeconds(value) {
    if (typeof value === "number")
        return value;
    if (value && typeof value === "object" && "seconds" in value && typeof value.seconds === "number") {
        return value.seconds;
    }
    return null;
}
