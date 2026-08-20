import {readFile} from "node:fs/promises";
import {initializeTestEnvironment, type RulesTestEnvironment} from "@firebase/rules-unit-testing";
import {collection, doc, getDoc, getDocs, query, setDoc, updateDoc, where} from "firebase/firestore";
import {encodeGeohash} from "../../src/geo.js";
import {afterAll, afterEach, beforeAll, beforeEach, describe, expect, it} from "vitest";

const projectId = "match-ci";
const functionsBaseUrl = `http://127.0.0.1:5001/${projectId}/southamerica-east1`;
const authUrl = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key";

let testEnvironment: RulesTestEnvironment;
let idToken: string;
let uid: string;
// Guardado porque adminSetModeration exige reautenticar depois de ganhar a claim.
let userEmail: string;

beforeAll(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: await readFile("../firestore.rules", "utf8"),
    },
  });
});

beforeEach(async () => {
  userEmail = `callable-${Date.now()}-${Math.random()}@match.test`;
  const response = await fetch(authUrl, {
    method: "POST",
    headers: {"content-type": "application/json"},
    body: JSON.stringify({
      email: userEmail,
      password: "correct-horse-battery-staple",
      returnSecureToken: true,
    }),
  });
  const payload = await response.json() as {idToken: string; localId: string};
  expect(response.ok).toBe(true);
  idToken = payload.idToken;
  uid = payload.localId;
});

afterEach(async () => {
  await testEnvironment.clearFirestore();
  await fetch(`http://127.0.0.1:9099/emulator/v1/projects/${projectId}/accounts/${uid}`, {
    method: "DELETE",
  });
});

afterAll(async () => {
  await testEnvironment.cleanup();
});

describe("onUserCreate", () => {
  it("provisions the public profile, the private doc and a free subscription", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();

      const profile = await waitForDoc(() => getDoc(doc(database, "profiles", uid)));
      expect(profile.data()).toMatchObject({
        rating: 0,
        ratingCount: 0,
        matchesPlayed: 0,
        isBanned: false,
      });

      const privateData = await getDoc(doc(database, "profiles", uid, "private", "data"));
      expect(privateData.exists()).toBe(true);
      // Nasce disponível: o filtro de notificação (regra B5) consulta este
      // campo, e `false` no cadastro calaria o produto para quem nunca abre o
      // perfil. Ver o comentário em onUserCreate.
      expect(privateData.data()).toMatchObject({isAvailable: true, availableUntil: null});

      const subscription = await getDoc(doc(database, "users", uid, "subscription", "current"));
      expect(subscription.data()).toMatchObject({plan: "free", status: "active"});
    });
  });
});

describe("deleteAccount", () => {
  it("rejects unauthenticated requests", async () => {
    const response = await call("deleteAccount", {}, null);
    expect(response.status).toBe(401);
    expect(await response.text()).toContain("UNAUTHENTICATED");
  });

  it("rejects a non-empty payload", async () => {
    const response = await call("deleteAccount", {unexpected: true});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("deletes only the caller's data and stays idempotent", async () => {
    const otherUid = "other-user";
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "profiles", uid), {fullName: "Eu"});
      await setDoc(doc(database, "profiles", uid, "private", "data"), {phone: "+5511999999999"});
      await setDoc(doc(database, "users", uid, "notifications", "n1"), {type: "new_match"});
      await setDoc(doc(database, "profiles", otherUid), {fullName: "Outro"});
    });

    expect((await call("deleteAccount", {})).ok).toBe(true);
    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      expect((await getDoc(doc(database, "profiles", uid))).exists()).toBe(false);
      expect((await getDoc(doc(database, "profiles", uid, "private", "data"))).exists()).toBe(false);
      expect((await getDoc(doc(database, "users", uid, "notifications", "n1"))).exists()).toBe(false);
      expect((await getDoc(doc(database, "profiles", otherUid))).exists()).toBe(true);
    });
  });
});

async function waitForDoc<T>(read: () => Promise<T & {exists(): boolean}>): Promise<T> {
  for (let attempt = 0; attempt < 20; attempt++) {
    const snapshot = await read();
    if (snapshot.exists()) return snapshot;
    await new Promise((resolve) => setTimeout(resolve, 150));
  }
  throw new Error("Document was not provisioned within the timeout.");
}

function call(name: string, data: unknown, token: string | null = idToken): Promise<Response> {
  return fetch(`${functionsBaseUrl}/${name}`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      ...(token == null ? {} : {authorization: `Bearer ${token}`}),
    },
    body: JSON.stringify({data}),
  });
}

describe("submitPlayerRating", () => {
  const RATED = "rated-player";
  const MATCH = "match-rating";

  /**
   * A match that already ended, with the caller and [RATED] on the roster —
   * the only shape in which a rating is allowed.
   */
  async function seedFinishedMatch(overrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const startedTwoHoursAgo = Math.floor(Date.now() / 1000) - 2 * 60 * 60;
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: RATED,
        status: "OPEN",
        startsAtSeconds: startedTwoHoursAgo,
        durationMin: 60,
        totalSlots: 10,
        confirmedCount: 2,
        participants: [uid, RATED],
        ...overrides,
      });
      await setDoc(doc(database, "profiles", RATED), {
        fullName: "Avaliado",
        rating: 0,
        ratingCount: 0,
      });
    });
  }

  function readProfile() {
    return testEnvironment.withSecurityRulesDisabled((context) =>
      getDoc(doc(context.firestore(), "profiles", RATED)),
    );
  }

  it("rejects unauthenticated requests", async () => {
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5}, null);
    expect(response.status).toBe(401);
  });

  it("rejects rating yourself", async () => {
    await seedFinishedMatch();
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: uid, rating: 5, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a star count outside 1..5", async () => {
    await seedFinishedMatch();
    for (const rating of [0, 6, 4.5]) {
      // Com as dimensões válidas, um 400 aqui só pode ser da nota geral.
      const response = await call("submitPlayerRating", {
        matchId: MATCH,
        ratedUserId: RATED,
        rating,
        punctuality: 5,
        respect: 5,
        fairPlay: 5,
        behavior: 5,
      });
      expect(response.status).toBe(400);
      expect(await response.text()).toContain("INVALID_ARGUMENT");
    }
  });

  it("rejects a comment longer than the limit", async () => {
    await seedFinishedMatch();
    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 5,
      punctuality: 5, respect: 5, fairPlay: 5, behavior: 5,
      comment: "x".repeat(501),
    });
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("rejects a match that has not finished yet", async () => {
    await seedFinishedMatch({startsAtSeconds: Math.floor(Date.now() / 1000) + 3600});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a cancelled match", async () => {
    await seedFinishedMatch({status: "CANCELLED"});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a caller who did not play the match", async () => {
    await seedFinishedMatch({participants: [RATED]});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5});
    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("rejects rating someone who did not play the match", async () => {
    await seedFinishedMatch({participants: [uid]});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("writes both copies and replaces the seed average on the first rating", async () => {
    await seedFinishedMatch();

    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 4,
      punctuality: 5, respect: 5, fairPlay: 5, behavior: 5,
      comment: "  Jogou bem  ",
    });
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({
      result: {status: "recorded", averageRating: 4, ratingCount: 1},
    });

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const ratingId = `${uid}_${RATED}`;

      const canonical = await getDoc(doc(database, "matches", MATCH, "ratings", ratingId));
      expect(canonical.data()).toMatchObject({
        matchId: MATCH,
        ratedUserId: RATED,
        raterUserId: uid,
        rating: 4,
        comment: "Jogou bem",
      });
      expect(typeof canonical.data()?.createdAtMs).toBe("number");

      const readModel = await getDoc(doc(database, "profiles", RATED, "ratings", ratingId));
      expect(readModel.data()).toMatchObject({rating: 4, raterUserId: uid});
    });

    // Perfil nasce com rating 0 / ratingCount 0, então a primeira nota é a
    // própria média — sem semente para desviar.
    expect((await readProfile()).data()).toMatchObject({rating: 4, ratingCount: 1});
  });

  it("averages against the existing count", async () => {
    await seedFinishedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "profiles", RATED), {
        fullName: "Avaliado",
        rating: 5,
        ratingCount: 3,
      });
    });

    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 3, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5});
    expect(response.ok).toBe(true);
    // (5*3 + 3) / 4 = 4.5
    expect(await response.json()).toMatchObject({result: {averageRating: 4.5, ratingCount: 4}});
  });

  it("is idempotent — resending does not inflate the average", async () => {
    await seedFinishedMatch();

    expect((await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 4, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5})).ok).toBe(true);

    const second = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 1, punctuality: 5, respect: 5, fairPlay: 5, behavior: 5});
    expect(second.ok).toBe(true);
    expect(await second.json()).toMatchObject({
      result: {status: "already_rated", averageRating: 4, ratingCount: 1},
    });

    expect((await readProfile()).data()).toMatchObject({rating: 4, ratingCount: 1});
  });
});

describe("submitReport", () => {
  const REPORTED = "reported-player";
  const MATCH = "match-report";

  async function seedMatch(participants: string[]) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH), {
        organizerId: REPORTED,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        totalSlots: 10,
        participants,
      });
    });
  }

  /** Reports from other people, so the threshold logic has something to count. */
  async function seedOtherReports(count: number, atMs = Date.now()) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      for (let index = 0; index < count; index++) {
        await setDoc(doc(database, "reports", `other-${index}`), {
          reporterId: `reporter-${index}`,
          reportedUserId: REPORTED,
          matchId: "some-match",
          reason: "no_show",
          status: "open",
          createdAtMs: atMs,
        });
      }
    });
  }

  function readModeration() {
    return testEnvironment.withSecurityRulesDisabled((context) =>
      getDoc(doc(context.firestore(), "moderation", REPORTED)),
    );
  }

  it("rejects unauthenticated requests", async () => {
    const response = await call(
      "submitReport",
      {matchId: MATCH, reportedUserId: REPORTED, reason: "no_show"},
      null,
    );
    expect(response.status).toBe(401);
  });

  it("rejects an unknown reason", async () => {
    await seedMatch([uid, REPORTED]);
    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "i_dont_like_them",
    });
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("rejects reporting yourself", async () => {
    await seedMatch([uid, REPORTED]);
    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: uid,
      reason: "no_show",
    });
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a reporter who did not play the match", async () => {
    await seedMatch([REPORTED]);
    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "no_show",
    });
    // Not being able to report a stranger is the main anti-abuse anchor.
    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("rejects reporting someone who did not play the match", async () => {
    await seedMatch([uid]);
    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "no_show",
    });
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("stores the report and trims the details", async () => {
    await seedMatch([uid, REPORTED]);

    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "no_show",
      details: "  não apareceu e não avisou  ",
    });
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({
      result: {status: "recorded", moderationLevel: "none"},
    });

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const stored = await getDoc(
        doc(context.firestore(), "reports", `${MATCH}_${uid}_${REPORTED}`),
      );
      expect(stored.data()).toMatchObject({
        reporterId: uid,
        reportedUserId: REPORTED,
        matchId: MATCH,
        reason: "no_show",
        details: "não apareceu e não avisou",
        status: "open",
      });
    });
  });

  it("counts one reporter once per match", async () => {
    await seedMatch([uid, REPORTED]);

    expect(
      (await call("submitReport", {matchId: MATCH, reportedUserId: REPORTED, reason: "no_show"})).ok,
    ).toBe(true);

    const second = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "harassment",
    });
    expect(second.ok).toBe(true);
    expect(await second.json()).toMatchObject({result: {status: "already_reported"}});
  });

  it("warns once enough distinct people have reported", async () => {
    await seedMatch([uid, REPORTED]);
    await seedOtherReports(2);

    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "no_show",
    });

    // Two others plus this caller = three distinct reporters.
    expect(await response.json()).toMatchObject({result: {moderationLevel: "warning"}});
    expect((await readModeration()).data()).toMatchObject({
      level: "warning",
      distinctReporters: 3,
      requiresReview: false,
    });
  });

  it("suspends, with a deadline, at the higher threshold", async () => {
    await seedMatch([uid, REPORTED]);
    await seedOtherReports(5);

    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "aggressive_behavior",
    });

    expect(await response.json()).toMatchObject({result: {moderationLevel: "suspended"}});
    const moderation = (await readModeration()).data();
    expect(moderation).toMatchObject({level: "suspended", distinctReporters: 6});
    expect(moderation?.untilMs).toBeGreaterThan(Date.now());
  });

  it("never bans automatically, only flags for review", async () => {
    await seedMatch([uid, REPORTED]);
    await seedOtherReports(20);

    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "discrimination",
    });

    expect(await response.json()).toMatchObject({result: {moderationLevel: "suspended"}});
    expect((await readModeration()).data()).toMatchObject({
      level: "suspended",
      requiresReview: true,
    });
  });

  it("ignores reports older than the counting window", async () => {
    await seedMatch([uid, REPORTED]);
    // Old enough to fall outside REPORT_WINDOW_DAYS.
    await seedOtherReports(5, Date.now() - 400 * 24 * 60 * 60 * 1_000);

    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "no_show",
    });

    // Only the caller counts, so nothing escalates.
    expect(await response.json()).toMatchObject({result: {moderationLevel: "none"}});
  });

  it("does not downgrade a ban set by a human", async () => {
    await seedMatch([uid, REPORTED]);
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "moderation", REPORTED), {level: "banned"});
    });

    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "no_show",
    });

    expect(await response.json()).toMatchObject({result: {moderationLevel: "banned"}});
    expect((await readModeration()).data()).toMatchObject({level: "banned"});
  });
});

describe("restricted accounts", () => {
  const MATCH = "match-blocked";

  async function suspendCaller(untilMs: number) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "moderation", uid), {
        level: "suspended",
        untilMs,
      });
    });
  }

  async function seedOpenMatch() {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH), {
        organizerId: "someone-else",
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        totalSlots: 10,
        confirmedCount: 1,
        participants: ["someone-else"],
      });
    });
  }

  it("cannot join a match while suspended", async () => {
    await seedOpenMatch();
    await suspendCaller(Date.now() + 24 * 60 * 60 * 1_000);

    const response = await call("joinMatch", {matchId: MATCH});

    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("can join again once the suspension has expired", async () => {
    await seedOpenMatch();
    await suspendCaller(Date.now() - 1_000);

    const response = await call("joinMatch", {matchId: MATCH});

    expect(response.ok).toBe(true);
  });

  it("cannot rate while suspended", async () => {
    await seedOpenMatch();
    await suspendCaller(Date.now() + 24 * 60 * 60 * 1_000);

    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: "someone-else",
      rating: 1,
    });

    expect(response.status).toBe(403);
  });

  it("cannot report while suspended", async () => {
    await seedOpenMatch();
    await suspendCaller(Date.now() + 24 * 60 * 60 * 1_000);

    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: "someone-else",
      reason: "no_show",
    });

    // Reports from a restricted account are usually retaliation.
    expect(response.status).toBe(403);
  });
});

describe("exportUserData", () => {
  it("rejects unauthenticated requests", async () => {
    const response = await call("exportUserData", {}, null);
    expect(response.status).toBe(401);
  });

  it("rejects a non-empty payload", async () => {
    const response = await call("exportUserData", {includeEverything: true});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("returns the caller's own data", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "profiles", uid), {fullName: "Eu", rating: 4});
      await setDoc(doc(database, "profiles", uid, "private", "data"), {phone: "+5511999999999"});
      await setDoc(doc(database, "users", uid, "notificationHistory", "n1"), {
        title: "Partida nova perto de você",
        isRead: false,
      });
    });

    const response = await call("exportUserData", {});
    expect(response.ok).toBe(true);

    const {result} = (await response.json()) as {result: Record<string, unknown>};
    expect(result).toMatchObject({
      userId: uid,
      profile: {fullName: "Eu"},
      private: {phone: "+5511999999999"},
    });
    expect(result.notificationHistory).toHaveLength(1);
  });

  it("hides who reported the caller", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "reports", "against"), {
        reporterId: "quem-denunciou",
        reportedUserId: uid,
        reason: "no_show",
        createdAtMs: Date.now(),
      });
      await setDoc(doc(database, "reports", "filed"), {
        reporterId: uid,
        reportedUserId: "outro",
        reason: "late",
        createdAtMs: Date.now(),
      });
    });

    const {result} = (await (await call("exportUserData", {})).json()) as {
      result: {
        reportsAgainst: Record<string, unknown>[];
        reportsFiled: Record<string, unknown>[];
      };
    };

    // Direito de acesso é sobre os dados da pessoa. A identidade de quem
    // denunciou é dado de terceiro e abriria caminho para retaliação.
    expect(result.reportsAgainst).toHaveLength(1);
    expect(result.reportsAgainst[0]).not.toHaveProperty("reporterId");
    expect(result.reportsAgainst[0]).toMatchObject({reason: "no_show"});

    // As que a própria pessoa fez saem inteiras.
    expect(result.reportsFiled[0]).toMatchObject({reporterId: uid, reason: "late"});
  });

  it("does not leak another user's data", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "profiles", "outra-pessoa"), {fullName: "Outra"});
    });

    const {result} = (await (await call("exportUserData", {})).json()) as {
      result: {profile: unknown};
    };

    expect(result.profile).toBeNull();
  });
});

describe("onMatchCreated", () => {
  const NEARBY = "vizinho";
  const FAR = "distante";

  /** Porto Alegre, centro. */
  const CENTER = {lat: -30.0346, lng: -51.2177};

  async function seedPlayerAt(userId: string, kmNorth: number, radiusKm = 15) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const lat = CENTER.lat + kmNorth * 0.009;
      await setDoc(doc(database, "profiles", userId), {fullName: userId});
      await setDoc(doc(database, "profiles", userId, "private", "data"), {
        lat,
        lng: CENTER.lng,
        geohash: encodeGeohash({lat, lng: CENTER.lng}),
        radiusKm,
        availableSports: [],
      });
    });
  }

  function historyOf(userId: string) {
    return testEnvironment.withSecurityRulesDisabled((context) =>
      getDocs(collection(context.firestore(), "users", userId, "notificationHistory")),
    );
  }

  it("avisa quem está dentro do raio e ignora quem está fora", async () => {
    await seedPlayerAt(NEARBY, 5);
    // Além do raio efetivo (mínimo de 20 km da regra B4).
    await seedPlayerAt(FAR, 40, 10);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", "m-nova"), {
        organizerId: uid,
        sport: "futsal",
        venue: "Green Ball",
        neighborhood: "Centro",
        lat: CENTER.lat,
        lng: CENTER.lng,
        totalSlots: 10,
        participants: [uid],
        status: "OPEN",
      });
    });

    await waitFor(async () => (await historyOf(NEARBY)).size > 0);

    const nearby = await historyOf(NEARBY);
    expect(nearby.docs[0].data()).toMatchObject({
      type: "new_match",
      isRead: false,
      data: {matchId: "m-nova"},
    });

    expect((await historyOf(FAR)).size).toBe(0);
    // O organizador nunca é avisado da própria partida.
    expect((await historyOf(uid)).size).toBe(0);
  });
});

describe("onParticipantChanged", () => {
  const PROMOTED = "promovido";
  const MATCH = "m-fila";

  it("avisa quem sobe da fila, e só nessa transição", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: uid,
        sport: "futsal",
        venue: "Green Ball",
        status: "OPEN",
        participants: [uid, PROMOTED],
      });
      // Entra na fila: não é promoção, não deve notificar.
      await setDoc(doc(database, "matches", MATCH, "participants", PROMOTED), {
        userId: PROMOTED,
        isConfirmed: false,
        positionInWaitlist: 1,
      });
    });

    const history = () =>
      testEnvironment.withSecurityRulesDisabled((context) =>
        getDocs(collection(context.firestore(), "users", PROMOTED, "notificationHistory")),
      );

    await new Promise((resolve) => setTimeout(resolve, 2_000));
    expect((await history()).size).toBe(0);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await updateDoc(
        doc(context.firestore(), "matches", MATCH, "participants", PROMOTED),
        {isConfirmed: true, positionInWaitlist: null},
      );
    });

    await waitFor(async () => (await history()).size > 0);
    expect((await history()).docs[0].data()).toMatchObject({
      type: "promoted",
      data: {matchId: MATCH},
    });
  });
});

/** Triggers são assíncronos: espera a condição em vez de dormir um tempo fixo. */
async function waitFor(condition: () => Promise<boolean>, attempts = 30): Promise<void> {
  for (let attempt = 0; attempt < attempts; attempt++) {
    if (await condition()) return;
    await new Promise((resolve) => setTimeout(resolve, 200));
  }
  throw new Error("Condition was not met within the timeout.");
}

describe("adminSetModeration", () => {
  const TARGET = "alvo";

  /**
   * Promove o usuário do teste a admin.
   *
   * A claim entra no token só na próxima emissão, então é obrigatório pegar um
   * idToken novo depois — sem isso o teste falha por permissão e parece bug da
   * função.
   */
  async function becomeAdmin(): Promise<string> {
    await fetch(
      `http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:update?key=fake-api-key`,
      {
        method: "POST",
        headers: {"content-type": "application/json", authorization: "Bearer owner"},
        body: JSON.stringify({localId: uid, customAttributes: JSON.stringify({admin: true})}),
      },
    );

    const response = await fetch(
      "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key",
      {
        method: "POST",
        headers: {"content-type": "application/json"},
        body: JSON.stringify({
          email: userEmail,
          password: "correct-horse-battery-staple",
          returnSecureToken: true,
        }),
      },
    );
    return ((await response.json()) as {idToken: string}).idToken;
  }

  async function seedTarget() {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "profiles", TARGET), {
        fullName: "Alvo",
        isBanned: false,
      });
    });
  }

  function readState() {
    return testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const [moderation, profile] = await Promise.all([
        getDoc(doc(database, "moderation", TARGET)),
        getDoc(doc(database, "profiles", TARGET)),
      ]);
      return {moderation: moderation.data(), profile: profile.data()};
    });
  }

  it("recusa quem não é admin", async () => {
    await seedTarget();

    const response = await call("adminSetModeration", {
      userId: TARGET,
      level: "banned",
      reason: "teste",
    });

    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("exige motivo e nível conhecido", async () => {
    await seedTarget();
    const adminToken = await becomeAdmin();

    const semMotivo = await call(
      "adminSetModeration",
      {userId: TARGET, level: "banned", reason: "   "},
      adminToken,
    );
    expect(semMotivo.status).toBe(400);

    const nivelInvalido = await call(
      "adminSetModeration",
      {userId: TARGET, level: "shadowban", reason: "teste"},
      adminToken,
    );
    expect(nivelInvalido.status).toBe(400);
  });

  it("impede um admin de moderar a si mesmo", async () => {
    const adminToken = await becomeAdmin();

    const response = await call(
      "adminSetModeration",
      {userId: uid, level: "banned", reason: "engano"},
      adminToken,
    );

    // Banir a própria conta fecharia a porta do painel por dentro.
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("banir espelha isBanned no perfil e tira da fila de revisão", async () => {
    await seedTarget();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "moderation", TARGET), {
        level: "suspended",
        requiresReview: true,
        untilMs: Date.now() + 1_000,
      });
    });
    const adminToken = await becomeAdmin();

    const response = await call(
      "adminSetModeration",
      {userId: TARGET, level: "banned", reason: "reincidência"},
      adminToken,
    );
    expect(response.ok).toBe(true);

    const state = await readState();
    expect(state.moderation).toMatchObject({
      level: "banned",
      untilMs: null,
      requiresReview: false,
      decidedBy: uid,
      reason: "reincidência",
    });
    // profiles.isBanned é o que a regra de criar partida e a busca leem.
    expect(state.profile).toMatchObject({isBanned: true});
  });

  it("suspende com o prazo informado", async () => {
    await seedTarget();
    const adminToken = await becomeAdmin();

    await call(
      "adminSetModeration",
      {userId: TARGET, level: "suspended", days: 3, reason: "faltou"},
      adminToken,
    );

    const {moderation, profile} = await readState();
    expect(moderation?.untilMs).toBeGreaterThan(Date.now());
    expect(moderation?.untilMs).toBeLessThan(Date.now() + 4 * 24 * 60 * 60 * 1_000);
    expect(profile).toMatchObject({isBanned: false});
  });

  it("desfaz uma punição com level none", async () => {
    await seedTarget();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "moderation", TARGET), {level: "banned", requiresReview: true});
      await setDoc(doc(database, "profiles", TARGET), {fullName: "Alvo", isBanned: true});
    });
    const adminToken = await becomeAdmin();

    await call(
      "adminSetModeration",
      {userId: TARGET, level: "none", reason: "denúncias improcedentes"},
      adminToken,
    );

    const {moderation, profile} = await readState();
    expect(moderation).toMatchObject({level: "none", untilMs: null, requiresReview: false});
    expect(profile).toMatchObject({isBanned: false});
  });

  it("guarda quem decidiu no histórico", async () => {
    await seedTarget();
    const adminToken = await becomeAdmin();

    await call(
      "adminSetModeration",
      {userId: TARGET, level: "warning", reason: "primeira vez"},
      adminToken,
    );

    const {moderation} = await readState();
    expect(moderation?.history).toHaveLength(1);
    expect(moderation?.history[0]).toMatchObject({level: "warning", decidedBy: uid});
  });

  it("recusa alvo sem perfil", async () => {
    const adminToken = await becomeAdmin();

    const response = await call(
      "adminSetModeration",
      {userId: "nao-existe", level: "banned", reason: "teste"},
      adminToken,
    );

    expect(response.status).toBe(404);
  });
});

describe("leaveMatch — contador de confirmados", () => {
  const MATCH = "m-contador";
  const OTHER = "outro-confirmado";
  const WAITING = "na-fila";

  it("promoção não infla o contador", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: OTHER,
        status: "FULL",
        startsAtSeconds: Math.floor(Date.now() / 1_000) + 3_600,
        totalSlots: 2,
        confirmedCount: 2,
        participants: [uid, OTHER, WAITING],
      });
      await setDoc(doc(database, "matches", MATCH, "participants", uid), {
        userId: uid,
        isConfirmed: true,
      });
      await setDoc(doc(database, "matches", MATCH, "participants", OTHER), {
        userId: OTHER,
        isConfirmed: true,
      });
      await setDoc(doc(database, "matches", MATCH, "participants", WAITING), {
        userId: WAITING,
        isConfirmed: false,
        positionInWaitlist: 1,
      });
    });

    const response = await call("leaveMatch", {matchId: MATCH});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({result: {promotedUserId: WAITING}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const match = await getDoc(doc(database, "matches", MATCH));

      // Sai um confirmado, entra um da fila: o total não muda. Somar aqui
      // inflava o contador a cada promoção e a partida ficava "cheia" com vaga
      // sobrando — e depois passava de totalSlots.
      expect(match.data()?.confirmedCount).toBe(2);

      const promoted = await getDoc(doc(database, "matches", MATCH, "participants", WAITING));
      expect(promoted.data()).toMatchObject({isConfirmed: true, positionInWaitlist: null});
    });
  });

  it("sem ninguém na fila, o contador cai", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: OTHER,
        status: "FULL",
        startsAtSeconds: Math.floor(Date.now() / 1_000) + 3_600,
        totalSlots: 2,
        confirmedCount: 2,
        participants: [uid, OTHER],
      });
      await setDoc(doc(database, "matches", MATCH, "participants", uid), {
        userId: uid,
        isConfirmed: true,
      });
    });

    expect((await call("leaveMatch", {matchId: MATCH})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const match = await getDoc(doc(context.firestore(), "matches", MATCH));
      expect(match.data()?.confirmedCount).toBe(1);
    });
  });
});

describe("deleteAccount — limpeza completa", () => {
  const FUTURE = "m-futura";
  const PAST = "m-passada";
  const OTHER = "outra-pessoa";

  async function seedEverything() {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const nowSeconds = Math.floor(Date.now() / 1_000);

      await setDoc(doc(database, "profiles", uid), {fullName: "Eu", isBanned: false});

      // Organiza uma futura e uma que já aconteceu.
      await setDoc(doc(database, "matches", FUTURE), {
        organizerId: uid,
        organizerName: "Eu",
        status: "OPEN",
        startsAtSeconds: nowSeconds + 7_200,
        participants: [uid],
      });
      await setDoc(doc(database, "matches", PAST), {
        organizerId: uid,
        organizerName: "Eu",
        status: "OPEN",
        startsAtSeconds: nowSeconds - 7_200,
        participants: [uid],
      });

      // Avaliação que escreveu sobre outra pessoa.
      await setDoc(doc(database, "profiles", OTHER, "ratings", `${uid}_${OTHER}`), {
        raterUserId: uid,
        ratedUserId: OTHER,
        rating: 5,
        createdAtMs: Date.now(),
      });

      // Denúncia que fez, e denúncia que recebeu.
      await setDoc(doc(database, "reports", `${PAST}_${uid}_${OTHER}`), {
        reporterId: uid,
        reportedUserId: OTHER,
        reason: "no_show",
        createdAtMs: Date.now(),
      });
      await setDoc(doc(database, "reports", `${PAST}_${OTHER}_${uid}`), {
        reporterId: OTHER,
        reportedUserId: uid,
        reason: "late",
        createdAtMs: Date.now(),
      });
      await setDoc(doc(database, "moderation", uid), {level: "warning"});
    });
  }

  it("cancela a partida futura e despersonaliza a passada", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const future = await getDoc(doc(database, "matches", FUTURE));
      const past = await getDoc(doc(database, "matches", PAST));

      // Partida futura sem organizador não tem como acontecer.
      expect(future.data()).toMatchObject({status: "CANCELLED", organizerName: "Jogador removido"});
      // A passada é histórico de quem jogou: fica, sem o nome.
      expect(past.data()).toMatchObject({status: "OPEN", organizerName: "Jogador removido"});
    });
  });

  it("mantém a avaliação sobre outra pessoa, sem o autor", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const ratings = await getDocs(
        collection(context.firestore(), "profiles", OTHER, "ratings"),
      );

      // A nota também é dado de quem foi avaliado — apagar mexeria na média
      // dele. Some o autor, não o conteúdo.
      expect(ratings.size).toBe(1);
      expect(ratings.docs[0].data().raterUserId).toBeNull();
      expect(ratings.docs[0].id).not.toContain(uid);
    });
  });

  it("mantém a denúncia que fez e apaga a que recebeu", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const filed = await getDocs(
        query(collection(database, "reports"), where("reportedUserId", "==", OTHER)),
      );
      const against = await getDocs(
        query(collection(database, "reports"), where("reportedUserId", "==", uid)),
      );

      // A denúncia é prova contra outra pessoa: apagá-la deixaria qualquer um
      // limpar o próprio rastro excluindo a conta.
      expect(filed.size).toBe(1);
      expect(filed.docs[0].data().reporterId).toBeNull();
      expect(filed.docs[0].id).not.toContain(uid);

      // Já a denúncia contra quem não existe mais não protege ninguém.
      expect(against.size).toBe(0);
      expect((await getDoc(doc(database, "moderation", uid))).exists()).toBe(false);
    });
  });

  it("apaga o usuário do Firebase Auth", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    const lookup = await fetch(
      "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:lookup?key=fake-api-key",
      {
        method: "POST",
        headers: {"content-type": "application/json", authorization: "Bearer owner"},
        body: JSON.stringify({localId: [uid]}),
      },
    );

    // Sem isto a conta continuava entrando e caía num estado sem perfil.
    expect(((await lookup.json()) as {users?: unknown[]}).users ?? []).toHaveLength(0);
  });

  it("continua idempotente", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);
    // O token ainda é válido por um tempo; repetir não pode explodir.
    expect((await call("deleteAccount", {})).ok).toBe(true);
  });
});

describe("submitPlayerRating — dimensões", () => {
  const RATED = "avaliado-dim";
  const MATCH = "m-dimensoes";
  const TODAS = {punctuality: 5, respect: 5, fairPlay: 3, behavior: 4};

  async function seedFinished() {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: RATED,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1_000) - 7_200,
        durationMin: 60,
        totalSlots: 10,
        participants: [uid, RATED],
      });
      await setDoc(doc(database, "profiles", RATED), {
        fullName: "Avaliado",
        rating: 0,
        ratingCount: 0,
      });
    });
  }

  function readProfile() {
    return testEnvironment.withSecurityRulesDisabled((context) =>
      getDoc(doc(context.firestore(), "profiles", RATED)),
    );
  }

  it("grava as quatro dimensões e agrega cada uma", async () => {
    await seedFinished();

    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 4,
      ...TODAS,
    });
    expect(response.ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const stored = await getDoc(
        doc(context.firestore(), "matches", MATCH, "ratings", `${uid}_${RATED}`),
      );
      expect(stored.data()).toMatchObject({rating: 4, ...TODAS});
    });

    // Uma contagem só: as dimensões caminham com ratingCount, porque toda
    // avaliação traz as quatro.
    expect((await readProfile()).data()).toMatchObject({
      rating: 4,
      ratingCount: 1,
      punctualityAverage: 5,
      fairPlayAverage: 3,
      behaviorAverage: 4,
    });
  });

  it("a primeira nota vira a média, sem semente atrapalhando", async () => {
    await seedFinished();

    await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 1,
      punctuality: 1,
      respect: 1,
      fairPlay: 1,
      behavior: 1,
    });

    // Perfil nasce com rating 0: a primeira nota 1 fica 1, não 3.
    expect((await readProfile()).data()).toMatchObject({rating: 1, punctualityAverage: 1});
  });

  it("recusa avaliação sem as dimensões", async () => {
    await seedFinished();

    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 4,
    });

    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("recusa avaliação pela metade", async () => {
    await seedFinished();

    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 4,
      punctuality: 5,
    });

    expect(response.status).toBe(400);
  });

  it("recusa dimensão fora de 1..5", async () => {
    await seedFinished();

    for (const invalida of [{punctuality: 0}, {respect: 6}, {fairPlay: 4.5}, {behavior: "bom"}]) {
      const response = await call("submitPlayerRating", {
        matchId: MATCH,
        ratedUserId: RATED,
        rating: 4,
        ...TODAS,
        ...invalida,
      });
      expect(response.status).toBe(400);
      expect(await response.text()).toContain("INVALID_ARGUMENT");
    }
  });
});
