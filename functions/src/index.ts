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
