import {initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, getFirestore} from "firebase-admin/firestore";
import * as functionsV1 from "firebase-functions/v1";
import {HttpsError, onCall} from "firebase-functions/v2/https";

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

function readEpochSeconds(value: unknown): number | null {
  if (typeof value === "number") return value;
  if (value && typeof value === "object" && "seconds" in value && typeof (value as {seconds: unknown}).seconds === "number") {
    return (value as {seconds: number}).seconds;
  }
  return null;
}
