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
exports.deleteAccount = exports.onUserCreate = void 0;
exports.requireEmptyPayload = requireEmptyPayload;
const app_1 = require("firebase-admin/app");
const auth_1 = require("firebase-admin/auth");
const firestore_1 = require("firebase-admin/firestore");
const functionsV1 = __importStar(require("firebase-functions/v1"));
const https_1 = require("firebase-functions/v2/https");
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
