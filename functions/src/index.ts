import {initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, getFirestore} from "firebase-admin/firestore";
import * as functionsV1 from "firebase-functions/v1";
import {getMessaging} from "firebase-admin/messaging";
import {onDocumentCreated, onDocumentWritten} from "firebase-functions/v2/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {boundsForRadius} from "./geo.js";
import {
  MAX_NOTIFY_RADIUS_KM,
  type NotificationCandidate,
  isWaitlistPromotion,
  parseCandidate,
  selectRecipients,
} from "./notifications.js";
import {
  FULL_VERIFICATION,
  meetsRequirement,
  missingVerification,
  parseEnforcementFlag,
  phoneNumberFromClaims,
  verificationFromClaims,
} from "./verification.js";
import {
  DAY_IN_MILLIS,
  MAX_REPORT_DETAILS_LENGTH,
  type ModerationLevel,
  REPORT_WINDOW_DAYS,
  SUSPENSION_DAYS,
  blockedError,
  isBlocked,
  isReportReason,
  isModerationLevel,
  levelForReporterCount,
  manualModerationState,
  nextRatingAverage,
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
// Tira o uid de todo lugar onde ele aparece: sai das partidas dos outros,
// apaga as que organizou e as séries delas, remove VIP e banimento, tira o
// nome do que escreveu e derruba perfil, dados privados e users/{uid}.
//
// O que sobrevive é o que também é dado de terceiro — a avaliação que ele deu
// continua existindo sem autor, porque a média dela está guardada no perfil de
// quem foi avaliado, e apagá-la deixaria a nota de outra pessoa errada.
// ---------------------------------------------------------------------------

export const deleteAccount = onCall(
  {region: REGION},
  async (request): Promise<{deleted: true}> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireRecentAuthentication(request.auth?.token.auth_time);
    requireEmptyPayload(request.data);

    // A ordem importa. Sair das partidas primeiro, porque isso lê o perfil e
    // libera vagas; apagar o usuário do Auth por último, para que uma falha no
    // meio deixe a pessoa capaz de repetir a chamada.
    await leaveAllMatches(uid);
    await deleteOrganizedMatches(uid);
    await deleteUidKeyedRecords(uid);
    await anonymizeAuthoredContent(uid);
    await deleteModerationTrail(uid);

    await Promise.all([
      db.recursiveDelete(db.doc(`profiles/${uid}`)),
      db.recursiveDelete(db.doc(`users/${uid}`)),
    ]);

    await getAuth()
      .deleteUser(uid)
      // Repetir a exclusão não pode falhar: o usuário já ter sumido é sucesso.
      .catch(() => undefined);

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

export const adminSetModeration = onCall(
  {region: REGION},
  async (request): Promise<AdminModerationResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    if (request.auth?.token.admin !== true) {
      throw new HttpsError("permission-denied", "Admin only.");
    }

    const data = (request.data ?? {}) as Partial<AdminModerationRequest>;
    if (typeof data.userId !== "string" || data.userId.length === 0) {
      throw new HttpsError("invalid-argument", "userId is required.");
    }
    if (!isModerationLevel(data.level)) {
      throw new HttpsError("invalid-argument", "level is not a known moderation level.");
    }
    // Um admin se banindo por engano perderia o acesso ao próprio painel.
    if (data.userId === uid) {
      throw new HttpsError("failed-precondition", "You cannot moderate your own account.");
    }
    const reason = typeof data.reason === "string" ? data.reason.trim() : "";
    if (reason.length === 0) {
      throw new HttpsError("invalid-argument", "reason is required.");
    }

    const nowMs = Date.now();
    const state = manualModerationState(
      data.level,
      typeof data.days === "number" ? data.days : null,
      nowMs,
    );

    await db.runTransaction(async (txn) => {
      const moderationRef = db.doc(`moderation/${data.userId}`);
      const profileRef = db.doc(`profiles/${data.userId}`);
      const [current, profile] = await txn.getAll(moderationRef, profileRef);

      if (!profile.exists) {
        throw new HttpsError("not-found", "Profile not found.");
      }

      const history = Array.isArray(current.data()?.history) ? current.data()!.history : [];

      txn.set(
        moderationRef,
        {
          level: state.level,
          untilMs: state.untilMs,
          requiresReview: state.requiresReview,
          reason,
          decidedBy: uid,
          updatedAtMs: nowMs,
          updatedAt: FieldValue.serverTimestamp(),
          history: [
            ...history,
            {level: state.level, atMs: nowMs, decidedBy: uid, reason},
          ].slice(-MAX_HISTORY_ENTRIES),
        },
        {merge: true},
      );

      // Espelho lido por isBannedUser() nas regras e pelo filtro da busca.
      txn.update(profileRef, {isBanned: state.isBanned, updatedAt: FieldValue.serverTimestamp()});
    });

    return {userId: data.userId, level: state.level, untilMs: state.untilMs};
  },
);

interface AdminModerationRequest {
  userId: string;
  level: string;
  days: number;
  reason: string;
}

interface AdminModerationResponse {
  userId: string;
  level: ModerationLevel;
  untilMs: number | null;
}

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

export const syncVerificationStatus = onCall(
  {region: REGION},
  async (request): Promise<VerificationSyncResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireEmptyPayload(request.data);

    const claims = request.auth?.token as unknown as Record<string, unknown>;
    const status = verificationFromClaims(claims);
    const phone = phoneNumberFromClaims(claims);

    const batch = db.batch();
    batch.set(
      db.doc(`profiles/${uid}`),
      {
        emailVerified: status.emailVerified,
        phoneVerified: status.phoneVerified,
        verificationCheckedAtMs: Date.now(),
        updatedAt: FieldValue.serverTimestamp(),
      },
      {merge: true},
    );

    // O número vem da claim assinada, nunca do payload: é o que o torna
    // confiável para uso futuro. Fica em private/data, que só o dono lê.
    if (phone !== null) {
      batch.set(
        db.doc(`profiles/${uid}/private/data`),
        {phone, updatedAt: FieldValue.serverTimestamp()},
        {merge: true},
      );
    }

    await batch.commit();

    return status;
  },
);

interface VerificationSyncResponse {
  emailVerified: boolean;
  phoneVerified: boolean;
}

/** Quanto tempo uma leitura de `config/verification` vale na instância. */
const VERIFICATION_CONFIG_CACHE_MILLIS = 60_000;

let verificationEnforcementCache: {enforced: boolean; readAtMs: number} | null = null;

/**
 * O interruptor mora num documento, não numa constante: ligar e desligar a
 * exigência vira mudar um campo no console, sem deploy. O cache poupa uma
 * leitura por chamada; no emulador ele é zerado para os testes poderem
 * alternar o valor entre um caso e outro.
 */
async function isVerificationEnforced(): Promise<boolean> {
  const cacheMillis = process.env.FUNCTIONS_EMULATOR === "true" ? 0 : VERIFICATION_CONFIG_CACHE_MILLIS;
  const nowMs = Date.now();
  if (verificationEnforcementCache && nowMs - verificationEnforcementCache.readAtMs < cacheMillis) {
    return verificationEnforcementCache.enforced;
  }
  // Uma leitura que falha (Firestore fora do ar, permissão, timeout) não pode
  // derrubar as 11 callables que passam por aqui: o interruptor é um detalhe
  // operacional, não a função em si. Cai no último valor conhecido e, sem ele,
  // no modo desligado — negar tudo por causa de uma leitura seria pior.
  let enforced: boolean;
  try {
    const snapshot = await db.doc("config/verification").get();
    enforced = parseEnforcementFlag(snapshot.data());
  } catch (error) {
    logger.error("isVerificationEnforced: falha ao ler config/verification", error);
    return verificationEnforcementCache?.enforced ?? false;
  }
  verificationEnforcementCache = {enforced, readAtMs: nowMs};
  return enforced;
}

/**
 * Barra a ação quando a exigência está ligada e a conta não tem e-mail e
 * telefone verificados. Lê das claims do token, não do espelho no perfil: o
 * espelho pode estar atrasado, e recusar quem já verificou seria pior.
 */
async function requireVerification(token: unknown): Promise<void> {
  if (!(await isVerificationEnforced())) return;

  const status = verificationFromClaims(token as Record<string, unknown>);
  if (meetsRequirement(status, FULL_VERIFICATION)) return;

  throw new HttpsError(
    "failed-precondition",
    `Verification required: ${missingVerification(status, FULL_VERIFICATION)}`,
  );
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

export const exportUserData = onCall(
  {region: REGION},
  async (request): Promise<UserDataExport> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireRecentAuthentication(request.auth?.token.auth_time);
    requireEmptyPayload(request.data);

    const [
      profile,
      privateData,
      moderation,
      ratingsReceived,
      notificationHistory,
      devices,
      subscription,
      reportsFiled,
      reportsAgainst,
      organizedMatches,
      participations,
    ] = await Promise.all([
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
  },
);

interface UserDataExport {
  exportedAtMs: number;
  userId: string;
  profile: FirebaseFirestore.DocumentData | null;
  private: FirebaseFirestore.DocumentData | null;
  moderation: FirebaseFirestore.DocumentData | null;
  ratingsReceived: ExportedDocument[];
  notificationHistory: ExportedDocument[];
  devices: ExportedDocument[];
  subscription: ExportedDocument[];
  reportsFiled: ExportedDocument[];
  reportsAgainst: ExportedDocument[];
  organizedMatches: ExportedDocument[];
  participations: ExportedDocument[];
}

type ExportedDocument = FirebaseFirestore.DocumentData & {id: string};

function documents(snapshot: FirebaseFirestore.QuerySnapshot): ExportedDocument[] {
  return snapshot.docs.map((document) => ({id: document.id, ...document.data()}));
}

/** Tira do export quem denunciou — é dado de terceiro, não da pessoa. */
function redactReporter(report: ExportedDocument): ExportedDocument {
  const {reporterId, ...rest} = report;
  void reporterId;
  return rest as ExportedDocument;
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
    await requireVerification(request.auth?.token);
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

      const bannedSnap = await txn.get(db.doc(`matches/${matchId}/bannedUsers/${uid}`));
      if (bannedSnap.exists) {
        throw new HttpsError("permission-denied", "You have been banned from this match.");
      }

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
      if (totalSlots < 1 || totalSlots > 50) {
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

      const seriesId = typeof match.seriesId === "string" ? match.seriesId : "";
      const isVip =
        seriesId.length > 0 &&
        (await txn.get(db.doc(`matchSeries/${seriesId}/vipPlayers/${uid}`))).exists;

      const baseParticipant = {
        userId: uid,
        displayName,
        photoUrl: null,
        joinedAt: FieldValue.serverTimestamp(),
        hasPaid: false,
        isVip,
      };

      if (isVip && left > 0) {
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
          isVip: false,
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
// O organizador também pode sair — ele só entra em `participants` se quiser,
// pelo mesmo joinMatch de qualquer outro jogador (ver createMatch no client:
// participants nasce vazio). Sair não cancela a partida nem tira o cargo de
// organizador, só libera a vaga de jogador que ele mesmo pegou.
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
      // Retry-safe: a segunda chamada de um cliente que já saiu (ex. resposta
      // perdida por timeout de rede e o withRetry do client tenta de novo)
      // não deve virar erro — o resultado desejado (estar fora da partida)
      // já foi alcançado pela primeira chamada.
      if (!participants.includes(uid)) {
        return {matchId, status: "already_left" as const};
      }

      const promotedUserId = await removeParticipant(txn, matchId, uid);

      return {matchId, status: "left" as const, promotedUserId};
    });
  },
);

interface LeaveMatchResponse {
  matchId: string;
  status: "left" | "already_left";
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
    await requireVerification(request.auth?.token);
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
    await requireVerification(request.auth?.token);

      const {matchId, ratedUserId, rating, comment} = parseSubmitRatingPayload(
      request.data,
      uid,
    );

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

      if (match.organizerId !== uid) {
        throw new HttpsError("permission-denied", "Only the organizer can rate players in this match.");
      }

      const participants: string[] = Array.isArray(match.participants)
        ? match.participants.filter((x): x is string => typeof x === "string")
        : [];
      if (!participants.includes(ratedUserId)) {
        throw new HttpsError("failed-precondition", "The rated user did not play this match.");
      }

      if (!ratedProfileSnap.exists) {
        throw new HttpsError("not-found", "Rated player profile not found.");
      }
      const ratedProfile = ratedProfileSnap.data() ?? {};

      const previousCount = Number(ratedProfile.ratingCount ?? 0);
      const previousAverage = Number(ratedProfile.rating ?? 0);

      // Reenviar edita a nota em vez de ser ignorado: recalcula a média
      // trocando o valor antigo pelo novo, sem inflar a contagem.
      const isEdit = existingSnap.exists;
      const previousRatingValue = isEdit ? Number(existingSnap.data()?.rating ?? 0) : null;

      const nextCount = isEdit ? previousCount : previousCount + 1;
      const nextAverage = isEdit
        ? roundTo(
            (previousAverage * previousCount - (previousRatingValue as number) + rating) / previousCount,
            RATING_AVERAGE_DECIMALS,
          )
        : nextRatingAverage(previousAverage, previousCount, rating, RATING_AVERAGE_DECIMALS);

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

      const responseStatus: "recorded" | "updated" = isEdit ? "updated" : "recorded";
      return {
        status: responseStatus,
        matchId,
        ratedUserId,
        averageRating: nextAverage,
        ratingCount: nextCount,
      };
    });
  },
);

function roundTo(value: number, decimals: number): number {
  const factor = 10 ** decimals;
  return Math.round(value * factor) / factor;
}

// ---------------------------------------------------------------------------
// submitOrganizerRating — Callable (invocada por products/games)
//
// Irmã de submitPlayerRating, mas o sentido é invertido: quem avalia é o
// PARTICIPANTE, o alvo é sempre o organizerId da própria partida. Nota
// separada de profiles/{uid}.rating (habilidade como jogador) — vive em
// profiles/{uid}.asOrganizerRating/.asOrganizerRatingCount, campos próprios,
// pra não contaminar a nota que os organizadores usam pra escalar jogador.
//
// matches/{matchId}/organizerRatings/{raterUid} é o registro canônico — id só
// do avaliador, porque o alvo já é fixo (um voto por participante por
// partida). Sem espelho em profiles/{organizerId}/organizerRatings — não há
// tela de "avaliações recebidas como organizador" hoje.
//
// Reenviar edita a nota em vez de ser ignorado, igual submitPlayerRating.
// Retorna {status: "recorded" | "updated", averageRating, ratingCount}.
// ---------------------------------------------------------------------------

export const submitOrganizerRating = onCall(
  {region: REGION},
  async (request): Promise<SubmitOrganizerRatingResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    await requireVerification(request.auth?.token);

    const {matchId, rating} = parseSubmitOrganizerRatingPayload(request.data);

    return db.runTransaction(async (txn) => {
      await requireNotBlocked(txn, uid, Date.now());

      const matchRef = db.doc(`matches/${matchId}`);
      const ratingRef = db.doc(`matches/${matchId}/organizerRatings/${uid}`);

      const [matchSnap, existingSnap] = await txn.getAll(matchRef, ratingRef);

      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      const status = String(match.status ?? "OPEN");
      if (status === "CANCELLED") {
        throw new HttpsError("failed-precondition", "Cannot rate a cancelled match.");
      }

      const organizerId = String(match.organizerId ?? "");
      if (uid === organizerId) {
        throw new HttpsError("invalid-argument", "Organizer cannot rate themselves.");
      }

      const participants: string[] = Array.isArray(match.participants)
        ? match.participants.filter((x): x is string => typeof x === "string")
        : [];
      if (!participants.includes(uid)) {
        throw new HttpsError("permission-denied", "Only confirmed participants can rate the organizer.");
      }

      const organizerProfileRef = db.doc(`profiles/${organizerId}`);
      const organizerProfileSnap = await txn.get(organizerProfileRef);
      if (!organizerProfileSnap.exists) {
        throw new HttpsError("not-found", "Organizer profile not found.");
      }
      const organizerProfile = organizerProfileSnap.data() ?? {};

      const previousCount = Number(organizerProfile.asOrganizerRatingCount ?? 0);
      const previousAverage = Number(organizerProfile.asOrganizerRating ?? 0);

      const isEdit = existingSnap.exists;
      const previousRatingValue = isEdit ? Number(existingSnap.data()?.rating ?? 0) : null;

      const nextCount = isEdit ? previousCount : previousCount + 1;
      const nextAverage = isEdit
        ? roundTo(
            (previousAverage * previousCount - (previousRatingValue as number) + rating) / previousCount,
            RATING_AVERAGE_DECIMALS,
          )
        : nextRatingAverage(previousAverage, previousCount, rating, RATING_AVERAGE_DECIMALS);

      const now = Date.now();
      txn.set(ratingRef, {
        matchId,
        organizerId,
        raterUserId: uid,
        rating,
        createdAtMs: now,
        createdAt: FieldValue.serverTimestamp(),
      });
      txn.update(organizerProfileRef, {
        asOrganizerRating: nextAverage,
        asOrganizerRatingCount: nextCount,
        updatedAt: FieldValue.serverTimestamp(),
      });

      const responseStatus: "recorded" | "updated" = isEdit ? "updated" : "recorded";
      return {
        status: responseStatus,
        matchId,
        organizerId,
        averageRating: nextAverage,
        ratingCount: nextCount,
      };
    });
  },
);

interface SubmitOrganizerRatingResponse {
  status: "recorded" | "updated";
  matchId: string;
  organizerId: string;
  averageRating: number;
  ratingCount: number;
}

interface SubmitOrganizerRatingPayload {
  matchId: string;
  rating: number;
}

function parseSubmitOrganizerRatingPayload(value: unknown): SubmitOrganizerRatingPayload {
  const data = (value ?? {}) as Partial<SubmitOrganizerRatingPayload>;

  if (typeof data.matchId !== "string" || data.matchId.length === 0) {
    throw new HttpsError("invalid-argument", "matchId is required.");
  }
  if (
    typeof data.rating !== "number" ||
    !Number.isInteger(data.rating) ||
    data.rating < 1 ||
    data.rating > 5
  ) {
    throw new HttpsError("invalid-argument", "rating must be an integer between 1 and 5.");
  }

  return {matchId: data.matchId, rating: data.rating};
}

interface SubmitPlayerRatingResponse {
  status: "recorded" | "updated";
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

  return {
    matchId: data.matchId,
    ratedUserId: data.ratedUserId,
    rating: data.rating,
    comment,
  };
}

// ---------------------------------------------------------------------------
// submitSkillRating — Callable (invocada por products/games)
//
// Nota de habilidade (1-10) que o organizador atribui a um jogador que jogou
// com ele — inclusive a si mesmo, se também jogou a própria partida.
// Independente de submitPlayerRating (reputação geral, qualquer avaliador) e
// de profiles/{uid}.rating: aqui quem avalia é sempre o organizador, a nota
// mede capacidade técnica pra montar times equilibrados. uid precisa ser o
// organizerId da partida, e ratedUserId precisa ter isConfirmed em
// matches/{matchId}/participants/{ratedUserId} — a mesma fonte que
// observeParticipants usa pra montar a lista de times, não o array
// matches/{matchId}.participants (que também guarda quem só entrou na fila).
//
// profiles/{ratedUserId}/skillRatings/{organizerId} — um documento por
// organizador avaliador, id = uid de quem avalia, então reenviar edita em vez
// de duplicar (mesmo padrão dos outros dois). profiles/{ratedUserId}
// .skillRating/.skillRatingCount é a média recalculada aqui, exibida em
// qualquer partida futura desse jogador — não só na que gerou a nota.
//
// Retorna {status: "recorded" | "updated", averageRating, ratingCount}.
// ---------------------------------------------------------------------------

const SKILL_RATING_MIN = 1;
const SKILL_RATING_MAX = 10;

export const submitSkillRating = onCall(
  {region: REGION},
  async (request): Promise<SubmitSkillRatingResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    await requireVerification(request.auth?.token);

    const {matchId, ratedUserId, rating} = parseSubmitSkillRatingPayload(request.data);

    return db.runTransaction(async (txn) => {
      await requireNotBlocked(txn, uid, Date.now());

      const matchRef = db.doc(`matches/${matchId}`);
      const skillRatingRef = db.doc(`profiles/${ratedUserId}/skillRatings/${uid}`);
      const ratedProfileRef = db.doc(`profiles/${ratedUserId}`);
      const ratedParticipantRef = db.doc(`matches/${matchId}/participants/${ratedUserId}`);

      const [matchSnap, existingSnap, ratedProfileSnap, ratedParticipantSnap] = await txn.getAll(
        matchRef,
        skillRatingRef,
        ratedProfileRef,
        ratedParticipantRef,
      );

      if (!matchSnap.exists) {
        logger.warn("submitSkillRating: match not found", {matchId, uid, ratedUserId});
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      if (match.organizerId !== uid) {
        logger.warn("submitSkillRating: caller is not the organizer", {
          matchId,
          uid,
          ratedUserId,
          organizerId: match.organizerId,
        });
        throw new HttpsError("permission-denied", "Only the organizer can rate players' skill.");
      }

      // A fonte da verdade de "quem jogou" é o subdocumento de participante
      // (o mesmo que a tela de sortear times lê via observeParticipants), não
      // o array matches/{matchId}.participants — esse array também inclui
      // quem só entrou na fila de espera, e partidas antigas podem tê-lo
      // desatualizado.
      if (ratedParticipantSnap.data()?.isConfirmed !== true) {
        logger.warn("submitSkillRating: rated user is not a confirmed participant", {
          matchId,
          uid,
          ratedUserId,
          participantExists: ratedParticipantSnap.exists,
          participantData: ratedParticipantSnap.data() ?? null,
        });
        throw new HttpsError("failed-precondition", "The rated user did not play this match.");
      }

      // Contas antigas podem ter ficado sem profiles/{uid} (onUserCreate nunca
      // rodou ou falhou silenciosamente pra elas) — a pessoa consegue se
      // autenticar e organizar partidas normalmente porque nada mais no app
      // exige esse documento, mas a nota de habilidade precisa dele pra
      // guardar a média. Em vez de travar, recria o perfil com o mesmo
      // formato do onUserCreate a partir do registro real de Auth.
      let ratedProfile = ratedProfileSnap.data();
      if (ratedProfile === undefined) {
        logger.warn("submitSkillRating: rated player profile missing, backfilling from Auth", {
          matchId,
          uid,
          ratedUserId,
        });
        const ratedUser = await getAuth().getUser(ratedUserId);
        ratedProfile = {
          fullName: ratedUser.displayName ?? "",
          nickname: null,
          avatarUrl: ratedUser.photoURL ?? null,
          position: null,
          level: "Livre",
          sports: [],
          city: null,
          neighborhood: null,
          rating: 0,
          ratingCount: 0,
          matchesPlayed: 0,
          isBanned: false,
          createdAt: FieldValue.serverTimestamp(),
        };
        txn.set(ratedProfileRef, ratedProfile);
      }

      const previousCount = Number(ratedProfile.skillRatingCount ?? 0);
      const previousAverage = Number(ratedProfile.skillRating ?? 0);

      const isEdit = existingSnap.exists;
      const previousRatingValue = isEdit ? Number(existingSnap.data()?.rating ?? 0) : null;

      const nextCount = isEdit ? previousCount : previousCount + 1;
      const nextAverage = isEdit
        ? roundTo(
            (previousAverage * previousCount - (previousRatingValue as number) + rating) / previousCount,
            RATING_AVERAGE_DECIMALS,
          )
        : nextRatingAverage(previousAverage, previousCount, rating, RATING_AVERAGE_DECIMALS);

      const now = Date.now();
      txn.set(skillRatingRef, {
        matchId,
        ratedUserId,
        organizerId: uid,
        rating,
        createdAtMs: now,
        createdAt: FieldValue.serverTimestamp(),
      });
      txn.update(ratedProfileRef, {
        skillRating: nextAverage,
        skillRatingCount: nextCount,
        updatedAt: FieldValue.serverTimestamp(),
      });

      const responseStatus: "recorded" | "updated" = isEdit ? "updated" : "recorded";
      return {
        status: responseStatus,
        matchId,
        ratedUserId,
        averageRating: nextAverage,
        ratingCount: nextCount,
      };
    });
  },
);

interface SubmitSkillRatingResponse {
  status: "recorded" | "updated";
  matchId: string;
  ratedUserId: string;
  averageRating: number;
  ratingCount: number;
}

interface SubmitSkillRatingPayload {
  matchId: string;
  ratedUserId: string;
  rating: number;
}

function parseSubmitSkillRatingPayload(value: unknown): SubmitSkillRatingPayload {
  const data = (value ?? {}) as Partial<SubmitSkillRatingPayload>;

  if (typeof data.matchId !== "string" || data.matchId.length === 0) {
    throw new HttpsError("invalid-argument", "matchId is required.");
  }
  if (typeof data.ratedUserId !== "string" || data.ratedUserId.length === 0) {
    throw new HttpsError("invalid-argument", "ratedUserId is required.");
  }
  if (
    typeof data.rating !== "number" ||
    !Number.isInteger(data.rating) ||
    data.rating < SKILL_RATING_MIN ||
    data.rating > SKILL_RATING_MAX
  ) {
    throw new HttpsError(
      "invalid-argument",
      `rating must be an integer between ${SKILL_RATING_MIN} and ${SKILL_RATING_MAX}.`,
    );
  }

  return {matchId: data.matchId, ratedUserId: data.ratedUserId, rating: data.rating};
}

async function promoteToConfirmed(
  txn: FirebaseFirestore.Transaction,
  matchId: string,
  targetUserId: string,
  currentConfirmedCount: number,
  totalSlots: number,
): Promise<void> {
  const participantRef = db.doc(`matches/${matchId}/participants/${targetUserId}`);
  const matchRef = db.doc(`matches/${matchId}`);
  txn.update(participantRef, {
    isConfirmed: true,
    positionInWaitlist: null,
    promotedAt: FieldValue.serverTimestamp(),
  });
  txn.update(matchRef, {
    confirmedCount: FieldValue.increment(1),
    status: currentConfirmedCount + 1 >= totalSlots ? "FULL" : "OPEN",
    updatedAt: FieldValue.serverTimestamp(),
  });
}

interface SetVipStatusRequest {
  matchId: string;
  targetUserId: string;
  isVip: boolean;
}

type SetVipStatusResponse = {matchId: string; isVip: boolean};

export const setVipStatus = onCall(
  {region: REGION},
  async (request): Promise<SetVipStatusResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    await requireVerification(request.auth?.token);
    const data = request.data as Partial<SetVipStatusRequest>;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
      throw new HttpsError("invalid-argument", "matchId is required.");
    }
    if (typeof data?.targetUserId !== "string" || data.targetUserId.length === 0) {
      throw new HttpsError("invalid-argument", "targetUserId is required.");
    }
    if (typeof data?.isVip !== "boolean") {
      throw new HttpsError("invalid-argument", "isVip is required.");
    }
    const matchId = data.matchId;
    const targetUserId = data.targetUserId;
    const isVip = data.isVip;

    return db.runTransaction(async (txn) => {
      await requireNotBlocked(txn, uid, Date.now());

      const matchRef = db.doc(`matches/${matchId}`);
      const targetParticipantRef = db.doc(`matches/${matchId}/participants/${targetUserId}`);
      const [matchSnap, targetParticipantSnap] = await txn.getAll(matchRef, targetParticipantRef);

      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      if (match.organizerId !== uid) {
        throw new HttpsError("permission-denied", "Only the organizer can set VIP status.");
      }

      const seriesId = typeof match.seriesId === "string" ? match.seriesId : "";
      if (!seriesId) {
        throw new HttpsError("failed-precondition", "This match is not part of a recurring series.");
      }

      if (!targetParticipantSnap.exists) {
        throw new HttpsError("failed-precondition", "Player is not in this match.");
      }

      const vipRef = db.doc(`matchSeries/${seriesId}/vipPlayers/${targetUserId}`);

      if (isVip) {
        txn.set(vipRef, {
          userId: targetUserId,
          addedAt: FieldValue.serverTimestamp(),
          addedBy: uid,
        });
        txn.set(db.doc(`matchSeries/${seriesId}`), {organizerId: match.organizerId}, {merge: true});
        txn.update(targetParticipantRef, {isVip: true});

        const wasConfirmed = Boolean(targetParticipantSnap.data()?.isConfirmed);
        if (!wasConfirmed) {
          const totalSlots = Number(match.totalSlots ?? 0);
          const confirmedCount = Number(match.confirmedCount ?? 0);
          await promoteToConfirmed(txn, matchId, targetUserId, confirmedCount, totalSlots);
        }
      } else {
        txn.delete(vipRef);
        txn.update(targetParticipantRef, {isVip: false});
      }

      return {matchId, isVip};
    });
  },
);

interface ConfirmWaitlistedPlayerRequest {
  matchId: string;
  targetUserId: string;
}

type ConfirmWaitlistedPlayerResponse = {matchId: string};

export const confirmWaitlistedPlayer = onCall(
  {region: REGION},
  async (request): Promise<ConfirmWaitlistedPlayerResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    await requireVerification(request.auth?.token);
    const data = request.data as Partial<ConfirmWaitlistedPlayerRequest>;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
      throw new HttpsError("invalid-argument", "matchId is required.");
    }
    if (typeof data?.targetUserId !== "string" || data.targetUserId.length === 0) {
      throw new HttpsError("invalid-argument", "targetUserId is required.");
    }
    const matchId = data.matchId;
    const targetUserId = data.targetUserId;

    return db.runTransaction(async (txn) => {
      await requireNotBlocked(txn, uid, Date.now());

      const matchRef = db.doc(`matches/${matchId}`);
      const targetParticipantRef = db.doc(`matches/${matchId}/participants/${targetUserId}`);
      const [matchSnap, targetParticipantSnap] = await txn.getAll(matchRef, targetParticipantRef);

      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      if (match.organizerId !== uid) {
        throw new HttpsError("permission-denied", "Only the organizer can confirm waitlisted players.");
      }

      const isWaitlisted = targetParticipantSnap.exists && targetParticipantSnap.data()?.isConfirmed === false;
      if (!isWaitlisted) {
        throw new HttpsError("failed-precondition", "Player is not on the waitlist.");
      }

      const totalSlots = Number(match.totalSlots ?? 0);
      const confirmedCount = Number(match.confirmedCount ?? 0);
      await promoteToConfirmed(txn, matchId, targetUserId, confirmedCount, totalSlots);

      return {matchId};
    });
  },
);

interface BanPlayerFromMatchRequest {
  matchId: string;
  targetUserId: string;
}

type BanPlayerFromMatchResponse = {matchId: string; promotedUserId?: string};

export const banPlayerFromMatch = onCall(
  {region: REGION},
  async (request): Promise<BanPlayerFromMatchResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    await requireVerification(request.auth?.token);
    const data = request.data as Partial<BanPlayerFromMatchRequest>;
    if (typeof data?.matchId !== "string" || data.matchId.length === 0) {
      throw new HttpsError("invalid-argument", "matchId is required.");
    }
    if (typeof data?.targetUserId !== "string" || data.targetUserId.length === 0) {
      throw new HttpsError("invalid-argument", "targetUserId is required.");
    }
    const matchId = data.matchId;
    const targetUserId = data.targetUserId;

    return db.runTransaction(async (txn) => {
      await requireNotBlocked(txn, uid, Date.now());

      const matchRef = db.doc(`matches/${matchId}`);
      const matchSnap = await txn.get(matchRef);
      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      if (match.organizerId !== uid) {
        throw new HttpsError("permission-denied", "Only the organizer can ban players.");
      }
      if (targetUserId === uid) {
        throw new HttpsError("failed-precondition", "Organizer cannot ban themselves.");
      }

      const promotedUserId = await removeParticipant(txn, matchId, targetUserId);

      txn.set(db.doc(`matches/${matchId}/bannedUsers/${targetUserId}`), {
        userId: targetUserId,
        bannedAt: FieldValue.serverTimestamp(),
        bannedBy: uid,
      });

      return {matchId, promotedUserId};
    });
  },
);

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

export const submitMatchRating = onCall(
  {region: REGION},
  async (request): Promise<SubmitMatchRatingResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    await requireVerification(request.auth?.token);

    const {matchId, rating} = parseSubmitMatchRatingPayload(request.data);

    return db.runTransaction(async (txn) => {
      await requireNotBlocked(txn, uid, Date.now());

      const matchRef = db.doc(`matches/${matchId}`);
      const matchRatingRef = db.doc(`matches/${matchId}/matchRatings/${uid}`);

      const [matchSnap, existingSnap] = await txn.getAll(matchRef, matchRatingRef);

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
          status: "already_rated" as const,
          matchId,
          averageRating: previousAverage,
          ratingCount: previousCount,
        };
      }

      const nextCount = previousCount + 1;
      const nextAverage = nextRatingAverage(
        previousAverage,
        previousCount,
        rating,
        RATING_AVERAGE_DECIMALS,
      );

      const now = Date.now();
      txn.set(matchRatingRef, {
        matchId,
        raterUserId: uid,
        rating,
        createdAtMs: now,
        createdAt: FieldValue.serverTimestamp(),
      });
      txn.set(
        templateRef,
        {
          organizerId,
          venueName,
          sport,
          rating: nextAverage,
          ratingCount: nextCount,
          updatedAt: FieldValue.serverTimestamp(),
        },
        {merge: true},
      );

      return {
        status: "recorded" as const,
        matchId,
        averageRating: nextAverage,
        ratingCount: nextCount,
      };
    });
  },
);

interface SubmitMatchRatingResponse {
  status: "recorded" | "already_rated";
  matchId: string;
  averageRating: number;
  ratingCount: number;
}

interface SubmitMatchRatingPayload {
  matchId: string;
  rating: number;
}

function parseSubmitMatchRatingPayload(value: unknown): SubmitMatchRatingPayload {
  const data = (value ?? {}) as Partial<SubmitMatchRatingPayload>;

  if (typeof data.matchId !== "string" || data.matchId.length === 0) {
    throw new HttpsError("invalid-argument", "matchId is required.");
  }
  if (
    typeof data.rating !== "number" ||
    !Number.isInteger(data.rating) ||
    data.rating < 1 ||
    data.rating > 5
  ) {
    throw new HttpsError("invalid-argument", "rating must be an integer between 1 and 5.");
  }

  return {matchId: data.matchId, rating: data.rating};
}

// ---------------------------------------------------------------------------
// submitReport — Callable (invocada por products/games)
//
// Denunciar um jogador confirmado na própria partida. Duas travas contra
// abuso, ambas estruturais e não configuráveis:
//   1. Só o organizador da partida denuncia — mesma regra de
//      submitPlayerRating. Denúncia deixou de ser peer-to-peer: agora vive
//      dentro do mesmo fluxo de avaliar jogador, que já é organizer-only.
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
    await requireVerification(request.auth?.token);

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
      if (matchSnap.data()?.organizerId !== uid) {
        throw new HttpsError("permission-denied", "Only the organizer can report players in this match.");
      }
      const participants: string[] = Array.isArray(matchSnap.data()?.participants)
        ? (matchSnap.data()?.participants as unknown[]).filter((x): x is string => typeof x === "string")
        : [];

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

export const onMatchCreated = onDocumentCreated(
  {region: REGION, document: "matches/{matchId}"},
  async (event) => {
    const match = event.data?.data();
    if (!match) return;

    const lat = typeof match.lat === "number" ? match.lat : null;
    const lng = typeof match.lng === "number" ? match.lng : null;
    const organizerId = typeof match.organizerId === "string" ? match.organizerId : "";
    if (lat === null || lng === null || organizerId === "") return;

    const candidates = await findNearbyCandidates({lat, lng});
    const recipients = selectRecipients(candidates, {
      matchId: event.params.matchId,
      organizerId,
      sport: String(match.sport ?? ""),
      lat,
      lng,
    });
    if (recipients.length === 0) return;

    const title = "Partida nova perto de você";
    const body = [match.sport, match.venueName, match.neighborhood]
      .filter((part): part is string => typeof part === "string" && part.length > 0)
      .join(" · ");

    await writeNotifications(
      recipients.map((recipient) => recipient.userId),
      {type: "new_match", title, body, matchId: event.params.matchId},
    );
  },
);

// ---------------------------------------------------------------------------
// onParticipantChanged — Trigger (matches/{matchId}/participants/{uid} escrito)
//
// Avisa quem subiu da fila. A promoção em si já acontece na transação do
// leaveMatch (regra B3) — aqui só entra a notificação, para que ela também
// alcance quem estava com o app fechado.
// ---------------------------------------------------------------------------

export const onParticipantChanged = onDocumentWritten(
  {region: REGION, document: "matches/{matchId}/participants/{participantId}"},
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!isWaitlistPromotion(before, after)) return;

    const matchId = event.params.matchId;
    const matchSnap = await db.doc(`matches/${matchId}`).get();
    const match = matchSnap.data() ?? {};
    const body = [match.sport, match.venueName]
      .filter((part): part is string => typeof part === "string" && part.length > 0)
      .join(" · ");

    await writeNotifications([event.params.participantId], {
      type: "promoted",
      title: "Você subiu da fila!",
      body,
      matchId,
    });
  },
);

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

export const cancelMatchSeries = onCall(
  {region: REGION},
  async (request): Promise<CancelMatchSeriesResponse> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    await requireVerification(request.auth?.token);

    const {matchId} = parseCancelMatchSeriesPayload(request.data);

    return db.runTransaction(async (txn) => {
      const matchRef = db.doc(`matches/${matchId}`);
      const matchSnap = await txn.get(matchRef);

      if (!matchSnap.exists) {
        throw new HttpsError("not-found", "Match not found.");
      }
      const match = matchSnap.data() ?? {};

      if (match.organizerId !== uid) {
        throw new HttpsError("permission-denied", "Only the organizer can cancel the recurrence.");
      }

      const seriesId = typeof match.seriesId === "string" ? match.seriesId : "";
      if (!seriesId) {
        throw new HttpsError("failed-precondition", "This match is not part of a recurring series.");
      }

      txn.set(
        db.doc(`matchSeries/${seriesId}`),
        {
          organizerId: match.organizerId,
          active: false,
          cancelledAt: FieldValue.serverTimestamp(),
          cancelledBy: uid,
        },
        {merge: true},
      );

      return {status: "recorded" as const, seriesId};
    });
  },
);

interface CancelMatchSeriesResponse {
  status: "recorded";
  seriesId: string;
}

interface CancelMatchSeriesPayload {
  matchId: string;
}

function parseCancelMatchSeriesPayload(value: unknown): CancelMatchSeriesPayload {
  const data = (value ?? {}) as Partial<CancelMatchSeriesPayload>;
  if (typeof data.matchId !== "string" || data.matchId.length === 0) {
    throw new HttpsError("invalid-argument", "matchId is required.");
  }
  return {matchId: data.matchId};
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

function nextOccurrenceSeconds(startsAtSeconds: number, recurrence: string): number {
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

export const generateRecurringMatches = onSchedule(
  {schedule: "every 24 hours", region: REGION, timeZone: "America/Sao_Paulo"},
  async () => {
    const nowSeconds = Math.floor(Date.now() / 1000);
    const leadThresholdSeconds = nowSeconds + RECURRENCE_LEAD_TIME_SECONDS;

    const snapshot = await db
      .collection("matches")
      .where("recurrence", "in", RECURRENCE_VALUES)
      .get();

    // Última ocorrência de cada série por data, não por status — uma ocorrência
    // cancelada precisa continuar sendo "a mais recente" para travar a série.
    const latestBySeries = new Map<string, FirebaseFirestore.QueryDocumentSnapshot>();
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

    const writes: Promise<unknown>[] = [];
    for (const doc of latestBySeries.values()) {
      const data = doc.data();
      if (data.status !== "OPEN") continue;

      const recurrence = data.recurrence;
      const startsAtSeconds = readEpochSeconds(data.startsAtSeconds);
      const organizerId = typeof data.organizerId === "string" ? data.organizerId : null;
      if (typeof recurrence !== "string" || startsAtSeconds === null || !organizerId) continue;

      const nextStartsAtSeconds = nextOccurrenceSeconds(startsAtSeconds, recurrence);
      if (nextStartsAtSeconds > leadThresholdSeconds) continue;

      const seriesId = typeof data.seriesId === "string" ? data.seriesId : doc.id;

      const seriesSnap = await db.doc(`matchSeries/${seriesId}`).get();
      if (seriesSnap.exists && seriesSnap.data()?.active === false) continue;

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
  },
);

// ---------------------------------------------------------------------------
// sendMatchReminders — Scheduled (a cada hora)
//
// Avisa quem participa de uma partida quando ela está chegando perto: um
// lembrete ~1 dia antes e outro ~2h antes. Cada marco tem uma janela de 2h de
// largura (o dobro da cadência do cron) para não deixar nenhuma partida
// escapar por causa de atraso/jitter do agendador; a flag em `matches/{id}`
// (`dayBeforeReminderSent`/`hoursBeforeReminderSent`) garante que a mesma
// partida não recebe o mesmo lembrete duas vezes mesmo com a janela sobrando.
// ---------------------------------------------------------------------------

const REMINDER_WINDOW_SECONDS = 60 * 60;

interface ReminderTier {
  field: "dayBeforeReminderSent" | "hoursBeforeReminderSent";
  leadSeconds: number;
  type: "match_reminder_day" | "match_reminder_hours";
  title: string;
}

const REMINDER_TIERS: ReminderTier[] = [
  {
    field: "dayBeforeReminderSent",
    leadSeconds: 24 * 60 * 60,
    type: "match_reminder_day",
    title: "Sua partida é amanhã",
  },
  {
    field: "hoursBeforeReminderSent",
    leadSeconds: 2 * 60 * 60,
    type: "match_reminder_hours",
    title: "Sua partida é daqui a pouco",
  },
];

export const sendMatchReminders = onSchedule(
  {schedule: "every 60 minutes", region: REGION, timeZone: "America/Sao_Paulo"},
  async () => {
    const nowSeconds = Math.floor(Date.now() / 1000);

    for (const tier of REMINDER_TIERS) {
      const windowCenter = nowSeconds + tier.leadSeconds;
      const windowStart = windowCenter - REMINDER_WINDOW_SECONDS;
      const windowEnd = windowCenter + REMINDER_WINDOW_SECONDS;

      const snapshot = await db
        .collection("matches")
        .where("status", "==", "OPEN")
        .where("startsAtSeconds", ">=", windowStart)
        .where("startsAtSeconds", "<=", windowEnd)
        .get();

      for (const doc of snapshot.docs) {
        const data = doc.data();
        if (data[tier.field] === true) continue;

        const participants: string[] = Array.isArray(data.participants)
          ? data.participants.filter((x): x is string => typeof x === "string")
          : [];

        if (participants.length > 0) {
          const body = [data.sport, data.venueName, data.neighborhood]
            .filter((part): part is string => typeof part === "string" && part.length > 0)
            .join(" · ");

          await writeNotifications(participants, {
            type: tier.type,
            title: tier.title,
            body,
            matchId: doc.id,
          });
        }

        await doc.ref.update({[tier.field]: true});
      }
    }
  },
);

/**
 * Jogadores dentro do raio máximo, lidos por faixa de geohash.
 *
 * Cada faixa é uma consulta; o `boundsForRadius` já devolve o menor conjunto de
 * faixas que cobre a caixa em volta do ponto.
 */
async function findNearbyCandidates(
  center: {lat: number; lng: number},
): Promise<NotificationCandidate[]> {
  const ranges = boundsForRadius(center, MAX_NOTIFY_RADIUS_KM);

  const snapshots = await Promise.all(
    ranges.map((range) =>
      db
        .collectionGroup("private")
        .orderBy("geohash")
        .startAt(range.start)
        .endAt(range.endInclusive)
        .get(),
    ),
  );

  const byUserId = new Map<string, NotificationCandidate>();

  for (const snapshot of snapshots) {
    for (const document of snapshot.docs) {
      // profiles/{uid}/private/data -> o uid é o avô do documento.
      const userId = document.ref.parent.parent?.id;
      if (!userId) continue;

      const candidate = parseCandidate(userId, document.data(), DEFAULT_RADIUS_KM);
      // Faixas de geohash podem se sobrepor; o Map deduplica.
      if (candidate) byUserId.set(userId, candidate);
    }
  }

  return [...byUserId.values()];
}

interface NotificationPayload {
  type: string;
  title: string;
  body: string;
  matchId: string;
}

/**
 * Grava o histórico de todos os destinatários e dispara o push.
 *
 * O histórico é a fonte da verdade: o push pode ser negado, o token pode estar
 * morto, o aparelho pode estar offline. Por isso a entrega falha em silêncio e
 * o item continua lá para o app mostrar quando abrir.
 */
async function writeNotifications(
  userIds: string[],
  payload: NotificationPayload,
): Promise<void> {
  const nowMs = Date.now();
  const batch = db.batch();

  for (const userId of userIds) {
    batch.set(db.collection(`users/${userId}/notificationHistory`).doc(), {
      type: payload.type,
      title: payload.title,
      body: payload.body,
      data: {matchId: payload.matchId},
      isRead: false,
      receivedAt: nowMs,
      createdAt: FieldValue.serverTimestamp(),
    });
  }

  await batch.commit();
  await sendPush(userIds, payload);
}

async function sendPush(userIds: string[], payload: NotificationPayload): Promise<void> {
  const tokenSnapshots = await Promise.all(
    userIds.map((userId) => db.collection(`users/${userId}/devices`).get()),
  );

  const tokens = tokenSnapshots
    .flatMap((snapshot) => snapshot.docs.map((document) => document.id))
    .filter((token) => token.length > 0);

  if (tokens.length === 0) return;

  try {
    await getMessaging().sendEachForMulticast({
      tokens,
      notification: {title: payload.title, body: payload.body},
      // Só strings: o FCM recusa qualquer outro tipo no data payload.
      data: {type: payload.type, matchId: payload.matchId},
    });
  } catch (error) {
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
async function removeParticipant(
  txn: FirebaseFirestore.Transaction,
  matchId: string,
  uid: string,
): Promise<string | undefined> {
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
    participants: FieldValue.arrayRemove(uid),
    [`teamAssignments.${uid}`]: FieldValue.delete(),
    updatedAt: FieldValue.serverTimestamp(),
  });

  if (!wasConfirmed) return undefined;

  if (firstWaitlist) {
    txn.update(firstWaitlist.ref, {
      isConfirmed: true,
      positionInWaitlist: null,
      promotedAt: FieldValue.serverTimestamp(),
    });
    // Sai um confirmado, entra um: o total não muda. Somar aqui inflava o
    // contador a cada promoção e a partida ficava "cheia" com vaga sobrando.
    txn.update(matchRef, {
      status: "OPEN",
      updatedAt: FieldValue.serverTimestamp(),
    });
    return firstWaitlist.id;
  }

  txn.update(matchRef, {
    confirmedCount: FieldValue.increment(-1),
    status: "OPEN",
    updatedAt: FieldValue.serverTimestamp(),
  });
  return undefined;
}

/** Primeiro da fila por ordem de entrada, ignorando quem está saindo. */
async function firstInWaitlist(
  txn: FirebaseFirestore.Transaction,
  matchId: string,
  leavingUid: string,
): Promise<FirebaseFirestore.QueryDocumentSnapshot | undefined> {
  const snapshot = await txn.get(
    db
      .collection(`matches/${matchId}/participants`)
      .where("isConfirmed", "==", false)
      .orderBy("positionInWaitlist", "asc")
      .limit(2),
  );
  return snapshot.docs.find((document) => document.id !== leavingUid);
}

/** Documentos lidos de uma vez em cada etapa da exclusão. */
const DELETION_PAGE_SIZE = 200;

/**
 * Percorre a consulta inteira, uma página por vez.
 *
 * O `.limit()` sozinho abandonava em silêncio tudo acima do teto: quem passasse
 * de [DELETION_PAGE_SIZE] partidas saía do app deixando o excedente no banco,
 * sem erro nenhum para denunciar. O cursor anda sobre o último documento lido,
 * e não reinicia a consulta, para que um documento que falhou ao ser apagado
 * não prenda o laço nele para sempre.
 */
async function forEachPage(
  query: FirebaseFirestore.Query,
  handle: (documents: FirebaseFirestore.QueryDocumentSnapshot[]) => Promise<void>,
): Promise<void> {
  let cursor: FirebaseFirestore.QueryDocumentSnapshot | undefined;

  for (;;) {
    const page = await (cursor ? query.startAfter(cursor) : query)
      .limit(DELETION_PAGE_SIZE)
      .get();
    if (page.empty) return;

    await handle(page.docs);

    if (page.size < DELETION_PAGE_SIZE) return;
    cursor = page.docs[page.size - 1];
  }
}

/**
 * Sai de toda partida em que a pessoa aparece.
 *
 * Uma transação por partida, e não uma só para todas: elas são independentes, e
 * uma transação gigante falharia inteira por causa de uma partida em conflito.
 * Reaproveita [removeParticipant], então a fila é promovida igual a uma saída
 * comum — a vaga não fica presa.
 */
async function leaveAllMatches(uid: string): Promise<void> {
  await forEachPage(
    db.collectionGroup("participants").where("userId", "==", uid),
    async (participations) => {
      for (const participation of participations) {
        const matchId = participation.ref.parent.parent?.id;
        if (!matchId) continue;

        await db
          .runTransaction(async (txn) => {
            const matchSnap = await txn.get(db.doc(`matches/${matchId}`));
            if (!matchSnap.exists) return;
            // Organizador não "sai" da própria partida — ela é tratada adiante.
            if (matchSnap.data()?.organizerId === uid) return;

            await removeParticipant(txn, matchId, uid);
          })
          // Uma partida problemática não pode impedir o resto da exclusão.
          .catch((error) => console.error(`leaveAllMatches ${matchId}`, error));

        await deleteSeriesVip(matchId, uid);
      }
    },
  );
}

/**
 * Tira o VIP da série a que a partida pertence.
 *
 * Existe ao lado da varredura por `userId` porque os documentos gravados antes
 * desse campo existir não aparecem em consulta nenhuma — o uid está só no id,
 * e o id não é consultável em collection group. Pelo caminho eles somem.
 */
async function deleteSeriesVip(matchId: string, uid: string): Promise<void> {
  const seriesId = (await db.doc(`matches/${matchId}`).get()).data()?.seriesId;
  if (typeof seriesId !== "string" || seriesId.length === 0) return;

  await db
    .doc(`matchSeries/${seriesId}/vipPlayers/${uid}`)
    .delete()
    .catch((error) => console.error(`deleteSeriesVip ${seriesId}`, error));
}

/**
 * Apaga as partidas que a pessoa organizou, com tudo que pendurava nelas.
 *
 * A partida que ainda vai acontecer avisa antes de sumir. Não existe push de
 * cancelamento no app: quem ia jogar descobria pelo próprio documento, que
 * passa a não existir — sem o aviso, a pessoa só descobriria na quadra.
 */
async function deleteOrganizedMatches(uid: string): Promise<void> {
  const nowMs = Date.now();

  await forEachPage(
    db.collection("matches").where("organizerId", "==", uid),
    async (organized) => {
      for (const match of organized) {
        const data = match.data();

        if (shouldCancelOnOrganizerDeletion(data, nowMs)) {
          await warnMatchIsGone(match.id, data, uid);
        }

        // recursiveDelete leva participants, bannedUsers, ratings,
        // matchRatings e organizerRatings junto — teamAssignments é campo do
        // próprio documento e vai com ele.
        await db
          .recursiveDelete(match.ref)
          .catch((error) => console.error(`deleteOrganizedMatches ${match.id}`, error));
      }
    },
  );

  await forEachPage(
    db.collection("matchSeries").where("organizerId", "==", uid),
    async (series) => {
      for (const document of series) {
        await db
          .recursiveDelete(document.ref)
          .catch((error) => console.error(`deleteOrganizedSeries ${document.id}`, error));
      }
    },
  );
}

/** Avisa quem estava escalado que a partida deixou de existir. */
async function warnMatchIsGone(
  matchId: string,
  match: FirebaseFirestore.DocumentData,
  organizerId: string,
): Promise<void> {
  const participants: string[] = Array.isArray(match.participants)
    ? match.participants.filter(
        (id): id is string => typeof id === "string" && id !== organizerId,
      )
    : [];
  if (participants.length === 0) return;

  const body = [match.sport, match.venueName, match.neighborhood]
    .filter((part): part is string => typeof part === "string" && part.length > 0)
    .join(" · ");

  await writeNotifications(participants, {
    type: "match_cancelled",
    title: "Partida cancelada",
    body,
    matchId,
  })
    // O aviso não pode travar a exclusão: a conta tem de sumir de qualquer jeito.
    .catch((error) => console.error(`warnMatchIsGone ${matchId}`, error));
}

/**
 * Partida futura sem organizador não tem como acontecer — cancela, para que
 * quem ia jogar descubra agora e não na quadra.
 */
export function shouldCancelOnOrganizerDeletion(
  match: {status?: unknown; startsAtSeconds?: unknown; startsAt?: unknown},
  nowMs: number,
): boolean {
  const status = String(match.status ?? "OPEN").toUpperCase();
  if (status === "CANCELLED" || status === "FINISHED") return false;

  const startsAt = readEpochSeconds(match.startsAtSeconds ?? match.startsAt);
  // Sem horário legível, cancelar é a escolha conservadora.
  if (startsAt === null) return true;
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
async function anonymizeAuthoredContent(uid: string): Promise<void> {
  for (const source of AUTHORED_CONTENT_SOURCES) {
    await forEachPage(
      db.collectionGroup(source.group).where(source.authorField, "==", uid),
      (documents) => anonymizePage(documents, source.authorField),
    );
  }

  await forEachPage(db.collection("reports").where("reporterId", "==", uid), (documents) =>
    anonymizePage(documents, "reporterId"),
  );
}

/**
 * Onde o uid aparece como autor. As três coleções de avaliação guardam a média
 * no perfil de quem foi avaliado: apagar o documento deixaria a nota de outra
 * pessoa errada para sempre, então o que sai é o autor, não o registro.
 */
const AUTHORED_CONTENT_SOURCES = [
  {group: "ratings", authorField: "raterUserId"},
  {group: "matchRatings", authorField: "raterUserId"},
  {group: "organizerRatings", authorField: "raterUserId"},
  {group: "skillRatings", authorField: "organizerId"},
] as const;

async function anonymizePage(
  documents: FirebaseFirestore.QueryDocumentSnapshot[],
  authorField: string,
): Promise<void> {
  const batch = db.batch();
  const anonymizedAtMs = Date.now();

  for (const document of documents) {
    // O uid está no id do documento nessas coleções, e id não se edita: o
    // único jeito de removê-lo é recriar o registro com id novo.
    batch.set(document.ref.parent.doc(), {
      ...document.data(),
      [authorField]: null,
      anonymizedAtMs,
    });
    batch.delete(document.ref);
  }

  await batch.commit();
}

/**
 * Apaga o que só existia por causa desta conta: o status de moderação dela e as
 * denúncias feitas contra ela.
 *
 * Guardar isso depois que o uid deixou de existir não protege ninguém — não há
 * mais conta para restringir — e seria dado pessoal sem finalidade.
 */
async function deleteModerationTrail(uid: string): Promise<void> {
  await forEachPage(
    db.collection("reports").where("reportedUserId", "==", uid),
    (documents) => deletePage(documents),
  );

  await db.doc(`moderation/${uid}`).delete();
}

/**
 * Apaga o que só faz sentido enquanto a conta existe: o VIP de uma série e o
 * banimento de uma partida. Sem conta para privilegiar ou barrar, os dois
 * registros viram dado pessoal sem finalidade.
 */
async function deleteUidKeyedRecords(uid: string): Promise<void> {
  for (const group of ["vipPlayers", "bannedUsers"] as const) {
    await forEachPage(db.collectionGroup(group).where("userId", "==", uid), (documents) =>
      deletePage(documents),
    );
  }
}

async function deletePage(
  documents: FirebaseFirestore.QueryDocumentSnapshot[],
): Promise<void> {
  const batch = db.batch();
  for (const document of documents) batch.delete(document.ref);
  await batch.commit();
}

function readEpochSeconds(value: unknown): number | null {
  if (typeof value === "number") return value;
  if (value && typeof value === "object" && "seconds" in value && typeof (value as {seconds: unknown}).seconds === "number") {
    return (value as {seconds: number}).seconds;
  }
  return null;
}
