import {initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, getFirestore} from "firebase-admin/firestore";
import * as functionsV1 from "firebase-functions/v1";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {
  DAY_IN_MILLIS,
  MAX_REPORT_DETAILS_LENGTH,
  type ModerationLevel,
  REPORT_WINDOW_DAYS,
  SUSPENSION_DAYS,
  blockedError,
  isBlocked,
  isReportReason,
  levelForReporterCount,
  requiresHumanReview,
  type ReportReason,
} from "./moderation.js";

initializeApp();

const db = getFirestore();
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

export const onUserCreate = functionsV1
  .region(REGION)
  .auth.user()
  .onCreate(async (user) => {
    const now = FieldValue.serverTimestamp();

    const profile = {
      fullName: user.displayName ?? "",
      nickname: null,
      avatarUrl: user.photoURL ?? null,
      position: null,
      level: "Livre",
      sports: [] as string[],
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
      availableSports: [] as string[],
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
    await getAuth().setCustomUserClaims(user.uid, {role: "user", plan: "free"});
  });

// ---------------------------------------------------------------------------
// deleteAccount — Callable (invocada por products/identity)
//
// Apaga o perfil público, os dados privados e tudo sob users/{uid}. Em fases
// futuras (LGPD, §6) será estendida para limpar partidas e participações.
// ---------------------------------------------------------------------------

export const deleteAccount = onCall(
  {region: REGION},
  async (request): Promise<{deleted: true}> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireRecentAuthentication(request.auth?.token.auth_time);
    requireEmptyPayload(request.data);

    await Promise.all([
      db.recursiveDelete(db.doc(`profiles/${uid}`)),
      db.recursiveDelete(db.doc(`users/${uid}`)),
    ]);

    return {deleted: true};
  },
);

function requireAuthentication(uid: string | undefined): asserts uid is string {
  if (!uid) throw new HttpsError("unauthenticated", "Authentication is required.");
}

function requireRecentAuthentication(authTime: unknown): void {
  if (typeof authTime !== "number" || Date.now() - authTime * 1_000 > RECENT_AUTH_WINDOW_MILLIS) {
    throw new HttpsError("failed-precondition", "Recent authentication is required.");
  }
}

export function requireEmptyPayload(value: unknown): void {
  if (typeof value !== "object" || value == null || Array.isArray(value)) {
    throw new HttpsError("invalid-argument", "Payload must be an object.");
  }
  if (Object.keys(value as Record<string, unknown>).length > 0) {
    throw new HttpsError("invalid-argument", "Payload must be empty.");
  }
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

export const joinMatch = onCall(
  {region: REGION},
  async (request): Promise<JoinMatchResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const data = request.data as Partial<JoinMatchRequest>;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
      throw new HttpsError("invalid-argument", "matchId is required.");
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
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      const status = String(match.status ?? "OPEN");
      if (status === "CANCELLED" || status === "FINISHED") {
        throw new HttpsError("failed-precondition", `Cannot join match in status ${status}.`);
      }

      const startsAt = readEpochSeconds(match.startsAtSeconds ?? match.startsAt);
      if (startsAt !== null && startsAt * 1000 < Date.now()) {
        throw new HttpsError("failed-precondition", "Match has already started.");
      }

      const totalSlots = Number(match.totalSlots ?? 0);
      const confirmedCount = Number(match.confirmedCount ?? 0);
      if (totalSlots < 2 || totalSlots > 40) {
        throw new HttpsError("failed-precondition", "Match slot range is invalid.");
      }

      const participants: string[] = Array.isArray(match.participants)
        ? match.participants.filter((x): x is string => typeof x === "string")
        : [];

      // Already in (organizer or another participant) → idempotent return.
      if (participants.includes(uid)) {
        const isConfirmed = confirmedCount > 0 && participants.indexOf(uid) < confirmedCount;
        return isConfirmed
          ? {status: "confirmed" as const, matchId}
          : {status: "already_joined" as const, matchId};
      }

      const left = Math.max(totalSlots - confirmedCount, 0);
      const displayName =
        request.auth?.token?.name ?? (typeof match.organizerName === "string" ? "Jogador" : "Jogador");

      const baseParticipant = {
        userId: uid,
        displayName,
        photoUrl: null,
        joinedAt: FieldValue.serverTimestamp(),
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
          confirmedCount: FieldValue.increment(1),
          participants: FieldValue.arrayUnion(uid),
          status: confirmedCount + 1 >= totalSlots ? "FULL" : "OPEN",
          updatedAt: FieldValue.serverTimestamp(),
        });
        return {status: "confirmed" as const, matchId};
      } else {
        // Full → push to waitlist. Position = current waitlist size + 1.
        const waitlistSnapshot = await txn.get(
          db.collection(`matches/${matchId}/participants`).where("isConfirmed", "==", false),
        );
        const position = waitlistSnapshot.size + 1;
        txn.set(participantRef, {
          ...baseParticipant,
          isConfirmed: false,
          positionInWaitlist: position,
        });
        txn.update(matchRef, {
          participants: FieldValue.arrayUnion(uid),
          updatedAt: FieldValue.serverTimestamp(),
        });
        return {status: "waitlist" as const, matchId, position};
      }
    });
  },
);

interface JoinMatchRequest {
  matchId: string;
}

type JoinMatchResponse =
  | {status: "confirmed"; matchId: string}
  | {status: "waitlist"; matchId: string; position: number}
  | {status: "already_joined"; matchId: string};

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

export const leaveMatch = onCall(
  {region: REGION},
  async (request): Promise<LeaveMatchResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const data = request.data as Partial<{matchId: string}>;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
      throw new HttpsError("invalid-argument", "matchId is required.");
    }
    const matchId = data.matchId;

    return db.runTransaction(async (txn) => {
      const matchRef = db.doc(`matches/${matchId}`);
      const matchSnap = await txn.get(matchRef);
      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      const status = String(match.status ?? "OPEN");
      if (status === "CANCELLED" || status === "FINISHED") {
        throw new HttpsError("failed-precondition", `Cannot leave match in status ${status}.`);
      }

      const participants: string[] = Array.isArray(match.participants)
        ? match.participants.filter((x): x is string => typeof x === "string")
        : [];
      if (!participants.includes(uid)) {
        throw new HttpsError("not-found", "You are not in this match.");
      }

      // Organizer cannot leave — must cancel instead.
      if (match.organizerId === uid) {
        throw new HttpsError(
          "failed-precondition",
          "Organizer must cancel the match, not leave it.",
        );
      }

      const myParticipantRef = db.doc(`matches/${matchId}/participants/${uid}`);
      const mySnap = await txn.get(myParticipantRef);
      const wasConfirmed = mySnap.exists ? Boolean(mySnap.data()?.isConfirmed) : false;

      // Delete participant doc + remove from array.
      txn.delete(myParticipantRef);
      txn.update(matchRef, {
        participants: FieldValue.arrayRemove(uid),
        updatedAt: FieldValue.serverTimestamp(),
      });

      let promotedUserId: string | undefined;

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
            promotedAt: FieldValue.serverTimestamp(),
          });
          txn.update(matchRef, {
            confirmedCount: FieldValue.increment(1),
            status: "OPEN", // opening back up since waitlist shrank
            updatedAt: FieldValue.serverTimestamp(),
          });
        } else {
          // No one to promote — just decrement.
          txn.update(matchRef, {
            confirmedCount: FieldValue.increment(-1),
            status: "OPEN",
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      }

      return {matchId, promotedUserId};
    });
  },
);

interface LeaveMatchResponse {
  matchId: string;
  /** UID of the user promoted from waitlist, if any. Undefined when no promotion happened. */
  promotedUserId?: string;
}

// ---------------------------------------------------------------------------
// cancelMatch — Callable (invocada por products/games, organizer-only)
//
// Encerra a partida: marca status=CANCELLED, remove todos os participantes.
// Apenas o organizador pode cancelar.
//
// Em fases futuras, este callable deve disparar notificações FCM para os
// participantes (placeholder reservado — Phase 6).
// ---------------------------------------------------------------------------

export const cancelMatch = onCall(
  {region: REGION},
  async (request): Promise<CancelMatchResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    const data = request.data as Partial<{matchId: string}>;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
      throw new HttpsError("invalid-argument", "matchId is required.");
    }
    const matchId = data.matchId;

    return db.runTransaction(async (txn) => {
      const matchRef = db.doc(`matches/${matchId}`);
      const matchSnap = await txn.get(matchRef);
      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      if (match.organizerId !== uid) {
        throw new HttpsError("permission-denied", "Only the organizer can cancel the match.");
      }

      const status = String(match.status ?? "OPEN");
      if (status === "CANCELLED") {
        return {matchId, status: "already_cancelled" as const};
      }
      if (status === "FINISHED") {
        throw new HttpsError("failed-precondition", "Cannot cancel a finished match.");
      }

      // Mark the match as cancelled; keep participant docs for audit but they
      // become unreachable via the UI (status filter). Cascade delete would
      // lose history, so we leave them and let a cleanup cron handle later.
      txn.update(matchRef, {
        status: "CANCELLED",
        cancelledAt: FieldValue.serverTimestamp(),
        cancelledBy: uid,
        updatedAt: FieldValue.serverTimestamp(),
      });

      return {matchId, status: "cancelled" as const};
    });
  },
);

interface CancelMatchResponse {
  matchId: string;
  status: "cancelled" | "already_cancelled";
}

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

export const submitPlayerRating = onCall(
  {region: REGION},
  async (request): Promise<SubmitPlayerRatingResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);

    const {matchId, ratedUserId, rating, comment} = parseSubmitRatingPayload(request.data, uid);

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
      const [matchSnap, existingSnap, ratedProfileSnap] = await txn.getAll(
        matchRef,
        matchRatingRef,
        ratedProfileRef,
      );

      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      const status = String(match.status ?? "OPEN");
      if (status === "CANCELLED") {
        throw new HttpsError("failed-precondition", "Cannot rate a cancelled match.");
      }
      requireMatchIsOver(match);

      const participants: string[] = Array.isArray(match.participants)
        ? match.participants.filter((x): x is string => typeof x === "string")
        : [];
      if (!participants.includes(uid)) {
        throw new HttpsError("permission-denied", "Only participants can rate this match.");
      }
      if (!participants.includes(ratedUserId)) {
        throw new HttpsError("failed-precondition", "The rated user did not play this match.");
      }

      if (!ratedProfileSnap.exists) {
        throw new HttpsError("not-found", "Rated player profile not found.");
      }
      const ratedProfile = ratedProfileSnap.data() ?? {};

      const previousCount = Number(ratedProfile.ratingCount ?? 0);
      const previousAverage = Number(ratedProfile.rating ?? 0);

      // Idempotente, como joinMatch/cancelMatch: reenviar não infla a média.
      if (existingSnap.exists) {
        return {
          status: "already_rated" as const,
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
        createdAt: FieldValue.serverTimestamp(),
      };

      txn.set(matchRatingRef, ratingDocument);
      txn.set(db.doc(`profiles/${ratedUserId}/ratings/${ratingId}`), ratingDocument);
      txn.update(ratedProfileRef, {
        rating: nextAverage,
        ratingCount: nextCount,
        updatedAt: FieldValue.serverTimestamp(),
      });

      return {
        status: "recorded" as const,
        matchId,
        ratedUserId,
        averageRating: nextAverage,
        ratingCount: nextCount,
      };
    });
  },
);

interface SubmitPlayerRatingResponse {
  status: "recorded" | "already_rated";
  matchId: string;
  ratedUserId: string;
  averageRating: number;
  ratingCount: number;
}

interface SubmitRatingPayload {
  matchId: string;
  ratedUserId: string;
  rating: number;
  comment: string;
}

function parseSubmitRatingPayload(value: unknown, uid: string): SubmitRatingPayload {
  const data = (value ?? {}) as Partial<SubmitRatingPayload>;

  if (typeof data.matchId !== "string" || data.matchId.length === 0) {
    throw new HttpsError("invalid-argument", "matchId is required.");
  }
  if (typeof data.ratedUserId !== "string" || data.ratedUserId.length === 0) {
    throw new HttpsError("invalid-argument", "ratedUserId is required.");
  }
  if (data.ratedUserId === uid) {
    throw new HttpsError("failed-precondition", "You cannot rate yourself.");
  }
  if (typeof data.rating !== "number" || !Number.isInteger(data.rating) || data.rating < 1 || data.rating > 5) {
    throw new HttpsError("invalid-argument", "rating must be an integer between 1 and 5.");
  }
  const comment = typeof data.comment === "string" ? data.comment.trim() : "";
  if (comment.length > MAX_RATING_COMMENT_LENGTH) {
    throw new HttpsError(
      "invalid-argument",
      `comment must be at most ${MAX_RATING_COMMENT_LENGTH} characters.`,
    );
  }

  return {matchId: data.matchId, ratedUserId: data.ratedUserId, rating: data.rating, comment};
}

/**
 * Avaliação é pós-partida. Não dá para exigir status FINISHED porque nada marca
 * esse status ainda — então a verdade é o relógio: início + duração no passado.
 */
function requireMatchIsOver(match: Record<string, unknown>): void {
  const startsAt = readEpochSeconds(match.startsAtSeconds ?? match.startsAt);
  if (startsAt === null) {
    throw new HttpsError("failed-precondition", "Match has no start time.");
  }
  const durationMin = Number(match.durationMin ?? 0);
  const endsAtMillis = (startsAt + Math.max(durationMin, 0) * 60) * 1_000;
  if (endsAtMillis > Date.now()) {
    throw new HttpsError("failed-precondition", "Match has not finished yet.");
  }
}

function roundTo(value: number, decimals: number): number {
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

export const submitReport = onCall(
  {region: REGION},
  async (request): Promise<SubmitReportResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);

    const {matchId, reportedUserId, reason, details} = parseReportPayload(request.data, uid);
    const nowMs = Date.now();
    const windowStartMs = nowMs - REPORT_WINDOW_DAYS * DAY_IN_MILLIS;

    return db.runTransaction(async (txn) => {
      const reportId = `${matchId}_${uid}_${reportedUserId}`;
      const matchRef = db.doc(`matches/${matchId}`);
      const reportRef = db.doc(`reports/${reportId}`);
      const moderationRef = db.doc(`moderation/${reportedUserId}`);

      // Denúncia vinda de conta restrita costuma ser retaliação.
      await requireNotBlocked(txn, uid, nowMs);

      const [matchSnap, existingSnap, moderationSnap] = await txn.getAll(
        matchRef,
        reportRef,
        moderationRef,
      );

      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const participants: string[] = Array.isArray(matchSnap.data()?.participants)
        ? (matchSnap.data()?.participants as unknown[]).filter((x): x is string => typeof x === "string")
        : [];

      if (!participants.includes(uid)) {
        throw new HttpsError("permission-denied", "Only participants can report in this match.");
      }
      if (!participants.includes(reportedUserId)) {
        throw new HttpsError("failed-precondition", "The reported user did not play this match.");
      }

      const moderation = moderationSnap.data();

      // Idempotente, como as outras callables: reenviar não conta de novo.
      if (existingSnap.exists) {
        return {
          status: "already_reported" as const,
          reportId,
          moderationLevel: (moderation?.level as ModerationLevel) ?? "none",
        };
      }

      // Denúncias recentes contra a mesma pessoa, lidas ainda na fase de leitura.
      const recentReports = await txn.get(
        db
          .collection("reports")
          .where("reportedUserId", "==", reportedUserId)
          .where("createdAtMs", ">=", windowStartMs),
      );

      const reporters = new Set<string>([uid]);
      for (const document of recentReports.docs) {
        const reporterId = document.data().reporterId;
        if (typeof reporterId === "string") reporters.add(reporterId);
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
        createdAt: FieldValue.serverTimestamp(),
      });

      const nextLevel = applyModeration({
        txn,
        moderationRef,
        current: moderation,
        distinctReporters,
        nowMs,
        reportedUserId,
      });

      return {status: "recorded" as const, reportId, moderationLevel: nextLevel};
    });
  },
);

interface SubmitReportResponse {
  status: "recorded" | "already_reported";
  reportId: string;
  moderationLevel: ModerationLevel;
}

interface ReportPayload {
  matchId: string;
  reportedUserId: string;
  reason: ReportReason;
  details: string;
}

function parseReportPayload(value: unknown, uid: string): ReportPayload {
  const data = (value ?? {}) as Partial<ReportPayload>;

  if (typeof data.matchId !== "string" || data.matchId.length === 0) {
    throw new HttpsError("invalid-argument", "matchId is required.");
  }
  if (typeof data.reportedUserId !== "string" || data.reportedUserId.length === 0) {
    throw new HttpsError("invalid-argument", "reportedUserId is required.");
  }
  if (data.reportedUserId === uid) {
    throw new HttpsError("failed-precondition", "You cannot report yourself.");
  }
  if (!isReportReason(data.reason)) {
    throw new HttpsError("invalid-argument", "reason is not a known report reason.");
  }
  const details = typeof data.details === "string" ? data.details.trim() : "";
  if (details.length > MAX_REPORT_DETAILS_LENGTH) {
    throw new HttpsError(
      "invalid-argument",
      `details must be at most ${MAX_REPORT_DETAILS_LENGTH} characters.`,
    );
  }

  return {matchId: data.matchId, reportedUserId: data.reportedUserId, reason: data.reason, details};
}

/**
 * Escreve o novo estado de moderação e devolve o nível resultante.
 *
 * Nunca rebaixa um `banned`: esse nível só vem de decisão humana, e uma
 * recontagem automática não pode desfazê-la.
 */
function applyModeration(input: {
  txn: FirebaseFirestore.Transaction;
  moderationRef: FirebaseFirestore.DocumentReference;
  current: FirebaseFirestore.DocumentData | undefined;
  distinctReporters: number;
  nowMs: number;
  reportedUserId: string;
}): ModerationLevel {
  const {txn, moderationRef, current, distinctReporters, nowMs} = input;

  if (current?.level === "banned") return "banned";

  const level = levelForReporterCount(distinctReporters);
  if (level === "none") return "none";

  const untilMs = level === "suspended" ? nowMs + SUSPENSION_DAYS * DAY_IN_MILLIS : null;
  const history = Array.isArray(current?.history) ? current.history : [];

  txn.set(
    moderationRef,
    {
      level,
      untilMs,
      distinctReporters,
      requiresReview: requiresHumanReview(distinctReporters),
      reason: "automatic_report_threshold",
      updatedAtMs: nowMs,
      updatedAt: FieldValue.serverTimestamp(),
      history: [...history, {level, distinctReporters, atMs: nowMs}].slice(-MAX_HISTORY_ENTRIES),
    },
    {merge: true},
  );

  return level;
}

const MAX_HISTORY_ENTRIES = 20;

/**
 * Recusa a ação quando a conta está banida ou com suspensão ativa.
 *
 * Roda dentro da transação de quem chama, junto das outras leituras — a decisão
 * precisa ver o mesmo instante do resto da operação.
 */
async function requireNotBlocked(
  txn: FirebaseFirestore.Transaction,
  uid: string,
  nowMs: number,
): Promise<void> {
  const snapshot = await txn.get(db.doc(`moderation/${uid}`));
  if (isBlocked(snapshot.data(), nowMs)) throw blockedError();
}

function readEpochSeconds(value: unknown): number | null {
  if (typeof value === "number") return value;
  if (value && typeof value === "object" && "seconds" in value && typeof (value as {seconds: unknown}).seconds === "number") {
    return (value as {seconds: number}).seconds;
  }
  return null;
}
