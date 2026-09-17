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
   * A match with the caller as organizer and [RATED] confirmed — the only
   * shape in which the organizer-only rule allows a rating. No time
   * constraint: rating is allowed as soon as the player is confirmed, not
   * just after the match ends.
   */
  async function seedMatch(overrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: uid,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
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

  async function readProfile() {
    let snapshot: Awaited<ReturnType<typeof getDoc>> | undefined;
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      snapshot = await getDoc(doc(context.firestore(), "profiles", RATED));
    });
    return snapshot as Awaited<ReturnType<typeof getDoc>>;
  }

  it("rejects unauthenticated requests", async () => {
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5}, null);
    expect(response.status).toBe(401);
  });

  it("rejects rating yourself", async () => {
    await seedMatch();
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: uid, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a star count outside 1..5", async () => {
    await seedMatch();
    for (const rating of [0, 6, 4.5]) {
      const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating});
      expect(response.status).toBe(400);
      expect(await response.text()).toContain("INVALID_ARGUMENT");
    }
  });

  it("rejects a comment longer than the limit", async () => {
    await seedMatch();
    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 5,
      comment: "x".repeat(501),
    });
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("allows rating before the match has started", async () => {
    await seedMatch({startsAtSeconds: Math.floor(Date.now() / 1000) + 7200});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.ok).toBe(true);
  });

  it("rejects a cancelled match", async () => {
    await seedMatch({status: "CANCELLED"});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a caller who is not the organizer", async () => {
    await seedMatch({organizerId: "someone-else", participants: [uid, RATED]});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("rejects rating someone who did not play the match", async () => {
    await seedMatch({participants: [uid]});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("writes both copies and replaces the seed average on the first rating", async () => {
    await seedMatch();

    const response = await call("submitPlayerRating", {
      matchId: MATCH,
      ratedUserId: RATED,
      rating: 4,
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
    await seedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "profiles", RATED), {
        fullName: "Avaliado",
        rating: 5,
        ratingCount: 3,
      });
    });

    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 3});
    expect(response.ok).toBe(true);
    // (5*3 + 3) / 4 = 4.5
    expect(await response.json()).toMatchObject({result: {averageRating: 4.5, ratingCount: 4}});
  });

  it("resubmitting updates the average instead of ignoring the new value", async () => {
    await seedMatch();

    const first = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 4});
    expect(first.ok).toBe(true);
    expect(await first.json()).toMatchObject({result: {status: "recorded", averageRating: 4, ratingCount: 1}});

    const second = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 2});
    expect(second.ok).toBe(true);
    expect(await second.json()).toMatchObject({result: {status: "updated", averageRating: 2, ratingCount: 1}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const ratingId = `${uid}_${RATED}`;
      const canonical = await getDoc(doc(context.firestore(), "matches", MATCH, "ratings", ratingId));
      expect(canonical.data()).toMatchObject({rating: 2});
    });

    expect((await readProfile()).data()).toMatchObject({rating: 2, ratingCount: 1});
  });

  it("editing does not change the count when other players were also rated", async () => {
    await seedMatch({participants: [uid, RATED, "third-player"]});
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "profiles", RATED), {fullName: "Avaliado", rating: 3, ratingCount: 2});
    });

    // A média já tem 2 votos de outras pessoas; esta é a primeira vez que
    // este organizador avalia este jogador — deve incrementar a contagem.
    const first = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(await first.json()).toMatchObject({result: {status: "recorded", averageRating: 3.67, ratingCount: 3}});

    // Editar não deve incrementar de novo. A base agora é a média já
    // arredondada gravada pela primeira chamada (3.67), não a semente
    // original (3) — a recontagem sempre parte do que está persistido.
    const second = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 1});
    // (3.67*3 - 5 + 1) / 3 = 2.3366... arredonda para 2.34
    expect(await second.json()).toMatchObject({result: {status: "updated", averageRating: 2.34, ratingCount: 3}});
  });
});

describe("submitSkillRating", () => {
  const RATED = "rated-player-skill";
  const MATCH = "match-skill-rating";

  async function seedMatch(overrides: Record<string, unknown> = {}, ratedParticipantOverrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: uid,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 10,
        confirmedCount: 2,
        participants: [uid, RATED],
        ...overrides,
      });
      await setDoc(doc(database, "matches", MATCH, "participants", RATED), {
        userId: RATED,
        isConfirmed: true,
        ...ratedParticipantOverrides,
      });
      await setDoc(doc(database, "profiles", RATED), {
        fullName: "Avaliado",
        skillRating: 0,
        skillRatingCount: 0,
      });
    });
  }

  async function readProfile() {
    let snapshot: Awaited<ReturnType<typeof getDoc>> | undefined;
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      snapshot = await getDoc(doc(context.firestore(), "profiles", RATED));
    });
    return snapshot as Awaited<ReturnType<typeof getDoc>>;
  }

  it("rejects unauthenticated requests", async () => {
    const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating: 8}, null);
    expect(response.status).toBe(401);
  });

  it("allows the organizer to rate their own skill when they are also a confirmed participant", async () => {
    await seedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH, "participants", uid), {
        userId: uid,
        isConfirmed: true,
      });
      await setDoc(doc(database, "profiles", uid), {
        fullName: "Organizador",
        skillRating: 0,
        skillRatingCount: 0,
      });
    });

    const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: uid, rating: 8});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({
      result: {status: "recorded", averageRating: 8, ratingCount: 1},
    });
  });

  it("backfills a missing profile document instead of failing (legacy accounts without profiles/{uid})", async () => {
    await seedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH, "participants", uid), {
        userId: uid,
        isConfirmed: true,
      });
      // De propósito: NÃO cria profiles/{uid} — reproduz a conta legada sem
      // esse documento que causava "Rated player profile not found." em
      // produção mesmo com o organizador autenticado e confirmado na partida.
    });

    const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: uid, rating: 9});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({
      result: {status: "recorded", averageRating: 9, ratingCount: 1},
    });

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const backfilled = await getDoc(doc(context.firestore(), "profiles", uid));
      expect(backfilled.exists()).toBe(true);
      expect(backfilled.data()).toMatchObject({
        rating: 0,
        ratingCount: 0,
        matchesPlayed: 0,
        isBanned: false,
        skillRating: 9,
        skillRatingCount: 1,
      });
    });
  });

  it("rejects a rating outside 1..10", async () => {
    await seedMatch();
    for (const rating of [0, 11, 5.5]) {
      const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating});
      expect(response.status).toBe(400);
      expect(await response.text()).toContain("INVALID_ARGUMENT");
    }
  });

  it("rejects a caller who is not the organizer", async () => {
    await seedMatch({organizerId: "someone-else", participants: [uid, RATED]});
    const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating: 8});
    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("rejects rating someone who is not a confirmed participant", async () => {
    await seedMatch({}, {isConfirmed: false});
    const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating: 8});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects rating someone with no participant record for this match", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: uid,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 10,
        confirmedCount: 0,
        participants: [uid],
      });
      await setDoc(doc(database, "profiles", RATED), {fullName: "Avaliado", skillRating: 0, skillRatingCount: 0});
    });
    const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating: 8});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("writes the per-organizer document and the profile average on the first rating", async () => {
    await seedMatch();

    const response = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating: 8});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({
      result: {status: "recorded", averageRating: 8, ratingCount: 1},
    });

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const skillRating = await getDoc(doc(context.firestore(), "profiles", RATED, "skillRatings", uid));
      expect(skillRating.data()).toMatchObject({matchId: MATCH, ratedUserId: RATED, organizerId: uid, rating: 8});
      expect(typeof skillRating.data()?.createdAtMs).toBe("number");
    });

    expect((await readProfile()).data()).toMatchObject({skillRating: 8, skillRatingCount: 1});
  });

  it("resubmitting updates the average instead of duplicating the vote", async () => {
    await seedMatch();

    const first = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating: 6});
    expect(await first.json()).toMatchObject({result: {status: "recorded", averageRating: 6, ratingCount: 1}});

    const second = await call("submitSkillRating", {matchId: MATCH, ratedUserId: RATED, rating: 10});
    expect(await second.json()).toMatchObject({result: {status: "updated", averageRating: 10, ratingCount: 1}});

    expect((await readProfile()).data()).toMatchObject({skillRating: 10, skillRatingCount: 1});
  });
});

async function signUpUser(prefix: string): Promise<{uid: string; idToken: string}> {
  const email = `${prefix}-${Date.now()}-${Math.random()}@match.test`;
  const response = await fetch(authUrl, {
    method: "POST",
    headers: {"content-type": "application/json"},
    body: JSON.stringify({email, password: "correct-horse-battery-staple", returnSecureToken: true}),
  });
  const payload = await response.json() as {idToken: string; localId: string};
  expect(response.ok).toBe(true);
  return {uid: payload.localId, idToken: payload.idToken};
}

describe("joinMatch VIP behavior", () => {
  const MATCH = "match-join-vip";
  const SERIES = "series-join-vip";

  async function seedMatch(overrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const data: Record<string, unknown> = {
        organizerId: "someone-else",
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 1,
        confirmedCount: 0,
        participants: [],
        seriesId: SERIES,
        ...overrides,
      };
      for (const key of Object.keys(data)) {
        if (data[key] === undefined) delete data[key];
      }
      await setDoc(doc(context.firestore(), "matches", MATCH), data);
    });
  }

  it("a non-VIP joiner always waitlists, even with an open slot", async () => {
    await seedMatch();
    const response = await call("joinMatch", {matchId: MATCH});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({result: {status: "waitlist", matchId: MATCH, position: 1}});
  });

  it("a VIP-for-the-series joiner confirms directly when a slot is open", async () => {
    await seedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matchSeries", SERIES, "vipPlayers", uid), {
        addedAt: new Date(),
        addedBy: "someone-else",
      });
    });

    const response = await call("joinMatch", {matchId: MATCH});
    expect(await response.json()).toMatchObject({result: {status: "confirmed", matchId: MATCH}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const participant = await getDoc(doc(context.firestore(), "matches", MATCH, "participants", uid));
      expect(participant.data()).toMatchObject({isConfirmed: true, isVip: true});
    });
  });

  it("a VIP-for-the-series joiner still waitlists when the match is full", async () => {
    await seedMatch({totalSlots: 1, confirmedCount: 1, participants: ["already-confirmed"]});
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matchSeries", SERIES, "vipPlayers", uid), {
        addedAt: new Date(),
        addedBy: "someone-else",
      });
    });

    const response = await call("joinMatch", {matchId: MATCH});
    expect(await response.json()).toMatchObject({result: {status: "waitlist", matchId: MATCH}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const participant = await getDoc(doc(context.firestore(), "matches", MATCH, "participants", uid));
      expect(participant.data()?.isVip).toBe(false);
    });
  });

  it("a one-off match (no seriesId) never auto-confirms, even for someone who is VIP elsewhere", async () => {
    await seedMatch({seriesId: undefined, totalSlots: 10, confirmedCount: 0});
    const response = await call("joinMatch", {matchId: MATCH});
    expect(await response.json()).toMatchObject({result: {status: "waitlist", matchId: MATCH, position: 1}});
  });
});

describe("setVipStatus", () => {
  const MATCH = "match-set-vip";
  const SERIES = "series-set-vip";
  const TARGET = "target-uid";

  async function seedMatch(overrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const data: Record<string, unknown> = {
        organizerId: uid,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 5,
        confirmedCount: 1,
        participants: [TARGET],
        seriesId: SERIES,
        ...overrides,
      };
      for (const key of Object.keys(data)) {
        if (data[key] === undefined) delete data[key];
      }
      await setDoc(doc(context.firestore(), "matches", MATCH), data);
    });
  }

  it("rejects unauthenticated requests", async () => {
    const response = await call("setVipStatus", {matchId: MATCH, targetUserId: TARGET, isVip: true}, null);
    expect(response.status).toBe(401);
  });

  it("rejects a caller who is not the organizer", async () => {
    await seedMatch({organizerId: "someone-else"});
    const response = await call("setVipStatus", {matchId: MATCH, targetUserId: TARGET, isVip: true});
    expect(response.status).toBe(403);
  });

  it("rejects a match with no seriesId", async () => {
    await seedMatch({seriesId: undefined});
    const response = await call("setVipStatus", {matchId: MATCH, targetUserId: TARGET, isVip: true});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a target who is not a participant in this match", async () => {
    await seedMatch();
    const response = await call("setVipStatus", {matchId: MATCH, targetUserId: "never-joined", isVip: true});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("marks a confirmed participant VIP without changing their placement", async () => {
    await seedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH, "participants", TARGET), {
        userId: TARGET,
        isConfirmed: true,
      });
    });

    const response = await call("setVipStatus", {matchId: MATCH, targetUserId: TARGET, isVip: true});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({result: {matchId: MATCH, isVip: true}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const participant = await getDoc(doc(database, "matches", MATCH, "participants", TARGET));
      expect(participant.data()).toMatchObject({isConfirmed: true, isVip: true});
      const vip = await getDoc(doc(database, "matchSeries", SERIES, "vipPlayers", TARGET));
      expect(vip.exists()).toBe(true);
      const match = await getDoc(doc(database, "matches", MATCH));
      expect(match.data()?.confirmedCount).toBe(1);
    });
  });

  it("marking a waitlisted participant VIP promotes them immediately, even past totalSlots", async () => {
    await seedMatch({totalSlots: 1, confirmedCount: 1, participants: ["already-confirmed", TARGET]});
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH, "participants", TARGET), {
        userId: TARGET,
        isConfirmed: false,
        positionInWaitlist: 1,
      });
    });

    const response = await call("setVipStatus", {matchId: MATCH, targetUserId: TARGET, isVip: true});
    expect(response.ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const participant = await getDoc(doc(database, "matches", MATCH, "participants", TARGET));
      expect(participant.data()).toMatchObject({isConfirmed: true, positionInWaitlist: null, isVip: true});
      const match = await getDoc(doc(database, "matches", MATCH));
      expect(match.data()?.confirmedCount).toBe(2);
      expect(match.data()?.status).toBe("FULL");
    });
  });

  it("unmarking VIP on a confirmed participant clears the flag without removing them", async () => {
    await seedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH, "participants", TARGET), {
        userId: TARGET,
        isConfirmed: true,
        isVip: true,
      });
      await setDoc(doc(database, "matchSeries", SERIES, "vipPlayers", TARGET), {
        addedAt: new Date(),
        addedBy: uid,
      });
    });

    const response = await call("setVipStatus", {matchId: MATCH, targetUserId: TARGET, isVip: false});
    expect(await response.json()).toMatchObject({result: {matchId: MATCH, isVip: false}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const participant = await getDoc(doc(database, "matches", MATCH, "participants", TARGET));
      expect(participant.data()).toMatchObject({isConfirmed: true, isVip: false});
      const vip = await getDoc(doc(database, "matchSeries", SERIES, "vipPlayers", TARGET));
      expect(vip.exists()).toBe(false);
    });
  });
});

describe("confirmWaitlistedPlayer", () => {
  const MATCH = "match-confirm-waitlisted";
  const TARGET = "target-uid";

  async function seedMatch(overrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: uid,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 1,
        confirmedCount: 1,
        participants: ["already-confirmed", TARGET],
        ...overrides,
      });
      await setDoc(doc(database, "matches", MATCH, "participants", TARGET), {
        userId: TARGET,
        isConfirmed: false,
        positionInWaitlist: 1,
      });
    });
  }

  it("rejects a caller who is not the organizer", async () => {
    await seedMatch({organizerId: "someone-else"});
    const response = await call("confirmWaitlistedPlayer", {matchId: MATCH, targetUserId: TARGET});
    expect(response.status).toBe(403);
  });

  it("rejects a target who is already confirmed", async () => {
    await seedMatch();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH, "participants", TARGET), {
        userId: TARGET,
        isConfirmed: true,
      });
    });
    const response = await call("confirmWaitlistedPlayer", {matchId: MATCH, targetUserId: TARGET});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("promotes the target to confirmed, even past totalSlots, without touching VIP", async () => {
    await seedMatch();
    const response = await call("confirmWaitlistedPlayer", {matchId: MATCH, targetUserId: TARGET});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({result: {matchId: MATCH}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const participant = await getDoc(doc(database, "matches", MATCH, "participants", TARGET));
      expect(participant.data()).toMatchObject({isConfirmed: true, positionInWaitlist: null});
      const match = await getDoc(doc(database, "matches", MATCH));
      expect(match.data()?.confirmedCount).toBe(2);
      expect(match.data()?.status).toBe("FULL");
    });
  });
});

describe("banPlayerFromMatch", () => {
  const MATCH = "match-ban";

  async function seedMatch(overrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH), {
        organizerId: uid,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 10,
        confirmedCount: 0,
        participants: [],
        ...overrides,
      });
    });
  }

  it("rejects unauthenticated requests", async () => {
    const response = await call("banPlayerFromMatch", {matchId: MATCH, targetUserId: "someone"}, null);
    expect(response.status).toBe(401);
  });

  it("rejects a caller who is not the organizer", async () => {
    await seedMatch({organizerId: "someone-else"});
    const response = await call("banPlayerFromMatch", {matchId: MATCH, targetUserId: "someone"});
    expect(response.status).toBe(403);
  });

  it("rejects the organizer trying to ban themselves", async () => {
    await seedMatch();
    const response = await call("banPlayerFromMatch", {matchId: MATCH, targetUserId: uid});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("removes a confirmed participant, decrements confirmedCount, and records the ban", async () => {
    await seedMatch({totalSlots: 10, confirmedCount: 1, participants: ["target-uid"]});
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH, "participants", "target-uid"), {
        userId: "target-uid",
        isConfirmed: true,
      });
    });

    const response = await call("banPlayerFromMatch", {matchId: MATCH, targetUserId: "target-uid"});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({result: {matchId: MATCH}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const participant = await getDoc(doc(database, "matches", MATCH, "participants", "target-uid"));
      expect(participant.exists()).toBe(false);
      const match = await getDoc(doc(database, "matches", MATCH));
      expect(match.data()?.confirmedCount).toBe(0);
      const ban = await getDoc(doc(database, "matches", MATCH, "bannedUsers", "target-uid"));
      expect(ban.exists()).toBe(true);
      expect(ban.data()?.bannedBy).toBe(uid);
    });
  });

  it("promotes the first waitlisted player when it bans a confirmed player", async () => {
    await seedMatch({totalSlots: 1, confirmedCount: 1, participants: ["target-uid", "waiting-uid"]});
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH, "participants", "target-uid"), {
        userId: "target-uid",
        isConfirmed: true,
      });
      await setDoc(doc(database, "matches", MATCH, "participants", "waiting-uid"), {
        userId: "waiting-uid",
        isConfirmed: false,
        positionInWaitlist: 1,
      });
    });

    const response = await call("banPlayerFromMatch", {matchId: MATCH, targetUserId: "target-uid"});
    expect(await response.json()).toMatchObject({result: {matchId: MATCH, promotedUserId: "waiting-uid"}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const promoted = await getDoc(doc(context.firestore(), "matches", MATCH, "participants", "waiting-uid"));
      expect(promoted.data()).toMatchObject({isConfirmed: true, positionInWaitlist: null});
    });
  });

  it("is a no-op removal (still records the ban) when the target is not in the match", async () => {
    await seedMatch();
    const response = await call("banPlayerFromMatch", {matchId: MATCH, targetUserId: "never-joined"});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({result: {matchId: MATCH}});

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const ban = await getDoc(doc(context.firestore(), "matches", MATCH, "bannedUsers", "never-joined"));
      expect(ban.exists()).toBe(true);
    });
  });

  it("blocks the banned player from rejoining via joinMatch", async () => {
    await seedMatch({totalSlots: 10, confirmedCount: 0});
    const target = await signUpUser("ban-rejoin");
    await call("banPlayerFromMatch", {matchId: MATCH, targetUserId: target.uid});

    const response = await call("joinMatch", {matchId: MATCH}, target.idToken);
    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });
});

describe("submitOrganizerRating", () => {
  const ORGANIZER = "organizer-to-rate";
  const MATCH = "match-organizer-rating";

  async function seedMatch(overrides: Record<string, unknown> = {}) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "matches", MATCH), {
        organizerId: ORGANIZER,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 10,
        confirmedCount: 2,
        participants: [ORGANIZER, uid],
        ...overrides,
      });
      await setDoc(doc(database, "profiles", ORGANIZER), {
        fullName: "Organizador",
        rating: 0,
        ratingCount: 0,
        asOrganizerRating: 0,
        asOrganizerRatingCount: 0,
      });
    });
  }

  async function readOrganizerProfile() {
    let snapshot: Awaited<ReturnType<typeof getDoc>> | undefined;
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      snapshot = await getDoc(doc(context.firestore(), "profiles", ORGANIZER));
    });
    return snapshot as Awaited<ReturnType<typeof getDoc>>;
  }

  it("rejects unauthenticated requests", async () => {
    const response = await call("submitOrganizerRating", {matchId: MATCH, rating: 5}, null);
    expect(response.status).toBe(401);
  });

  it("rejects the organizer rating themselves", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH), {
        organizerId: uid,
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        durationMin: 60,
        totalSlots: 10,
        confirmedCount: 1,
        participants: [uid],
      });
    });
    const response = await call("submitOrganizerRating", {matchId: MATCH, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("rejects a caller who is not a confirmed participant", async () => {
    await seedMatch({participants: [ORGANIZER]});
    const response = await call("submitOrganizerRating", {matchId: MATCH, rating: 5});
    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("rejects a cancelled match", async () => {
    await seedMatch({status: "CANCELLED"});
    const response = await call("submitOrganizerRating", {matchId: MATCH, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a star count outside 1..5", async () => {
    await seedMatch();
    for (const rating of [0, 6, 4.5]) {
      const response = await call("submitOrganizerRating", {matchId: MATCH, rating});
      expect(response.status).toBe(400);
    }
  });

  it("records a rating and updates the organizer's aggregate", async () => {
    await seedMatch();
    const response = await call("submitOrganizerRating", {matchId: MATCH, rating: 4});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({
      result: {status: "recorded", averageRating: 4, ratingCount: 1},
    });

    const profile = await readOrganizerProfile();
    expect(profile.data()).toMatchObject({asOrganizerRating: 4, asOrganizerRatingCount: 1});
  });

  it("resending edits the rating instead of inflating the count", async () => {
    await seedMatch();
    await call("submitOrganizerRating", {matchId: MATCH, rating: 4});
    const response = await call("submitOrganizerRating", {matchId: MATCH, rating: 2});
    expect(response.ok).toBe(true);
    expect(await response.json()).toMatchObject({
      result: {status: "updated", averageRating: 2, ratingCount: 1},
    });

    const profile = await readOrganizerProfile();
    expect(profile.data()).toMatchObject({asOrganizerRating: 2, asOrganizerRatingCount: 1});
  });

  it("does not touch the player-skill rating fields", async () => {
    await seedMatch();
    await call("submitOrganizerRating", {matchId: MATCH, rating: 5});
    const profile = await readOrganizerProfile();
    expect(profile.data()).toMatchObject({rating: 0, ratingCount: 0});
  });
});

describe("submitReport", () => {
  const REPORTED = "reported-player";
  const MATCH = "match-report";

  async function seedMatch(participants: string[]) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH), {
        organizerId: uid,
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

  it("rejects a caller who is not the organizer", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "matches", MATCH), {
        organizerId: "someone-else",
        status: "OPEN",
        startsAtSeconds: Math.floor(Date.now() / 1000) + 3600,
        totalSlots: 10,
        participants: [uid, REPORTED],
      });
    });
    const response = await call("submitReport", {
      matchId: MATCH,
      reportedUserId: REPORTED,
      reason: "no_show",
    });
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
  const ALHEIA = "m-de-outro";
  const SERIES = "serie-1";
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
        participants: [uid, OTHER],
      });
      await setDoc(doc(database, "matches", PAST), {
        organizerId: uid,
        organizerName: "Eu",
        status: "OPEN",
        startsAtSeconds: nowSeconds - 7_200,
        participants: [uid],
      });
      await setDoc(doc(database, "matches", FUTURE, "participants", OTHER), {
        userId: OTHER,
        isConfirmed: true,
      });
      await setDoc(doc(database, "matchSeries", SERIES), {organizerId: uid, active: true});

      // Joga na partida de outra pessoa: escalado num time, VIP da série dela,
      // banido, e tendo avaliado o jogo, o organizador e um jogador.
      await setDoc(doc(database, "matches", ALHEIA), {
        organizerId: OTHER,
        organizerName: "Outro",
        status: "OPEN",
        startsAtSeconds: nowSeconds + 7_200,
        participants: [uid],
        confirmedCount: 1,
        totalSlots: 10,
        seriesId: "serie-alheia",
        teamAssignments: {[uid]: 0, [OTHER]: 1},
      });
      await setDoc(doc(database, "matches", ALHEIA, "participants", uid), {
        userId: uid,
        isConfirmed: true,
        isVip: true,
      });
      await setDoc(doc(database, "matchSeries", "serie-alheia", "vipPlayers", uid), {
        userId: uid,
        addedBy: OTHER,
      });
      await setDoc(doc(database, "matches", ALHEIA, "bannedUsers", uid), {
        userId: uid,
        bannedBy: OTHER,
      });
      await setDoc(doc(database, "matches", ALHEIA, "matchRatings", uid), {
        matchId: ALHEIA,
        raterUserId: uid,
        rating: 4,
        createdAtMs: Date.now(),
      });
      await setDoc(doc(database, "matches", ALHEIA, "organizerRatings", uid), {
        matchId: ALHEIA,
        organizerId: OTHER,
        raterUserId: uid,
        rating: 5,
        createdAtMs: Date.now(),
      });
      await setDoc(doc(database, "profiles", OTHER, "skillRatings", uid), {
        matchId: PAST,
        ratedUserId: OTHER,
        organizerId: uid,
        rating: 3,
        createdAtMs: Date.now(),
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

  it("apaga toda partida que organizou, futura ou passada", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();

      expect((await getDoc(doc(database, "matches", FUTURE))).exists()).toBe(false);
      expect((await getDoc(doc(database, "matches", PAST))).exists()).toBe(false);
      // recursiveDelete tem de levar as subcoleções junto, senão sobram órfãs
      // sem pai — invisíveis no console e vivas na consulta de collection group.
      expect(
        (await getDocs(collection(database, "matches", FUTURE, "participants"))).size,
      ).toBe(0);
      // A série é dele: sem partidas e sem organizador, não gera mais nada.
      expect((await getDoc(doc(database, "matchSeries", SERIES))).exists()).toBe(false);
    });
  });

  it("avisa quem ia jogar antes de a partida futura sumir", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const history = await getDocs(
        collection(context.firestore(), "users", OTHER, "notificationHistory"),
      );

      // Não existe push de cancelamento no app: sem este aviso, o jogador
      // descobriria que a partida sumiu só na quadra.
      expect(history.size).toBe(1);
      expect(history.docs[0].data()).toMatchObject({
        type: "match_cancelled",
        data: {matchId: FUTURE},
      });
    });
  });

  it("não avisa sobre a partida que já aconteceu", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const history = await getDocs(
        collection(context.firestore(), "users", OTHER, "notificationHistory"),
      );

      expect(history.docs.every((entry) => entry.data().data?.matchId !== PAST)).toBe(true);
    });
  });

  it("sai da partida alheia, some do time e libera a vaga", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const match = await getDoc(doc(database, "matches", ALHEIA));

      // A partida é de outra pessoa: continua de pé, sem ele.
      expect(match.exists()).toBe(true);
      expect(match.data()?.participants ?? []).not.toContain(uid);
      expect((await getDoc(doc(database, "matches", ALHEIA, "participants", uid))).exists())
        .toBe(false);
      // A chave no mapa de times sobrevivia à saída e escalava um fantasma.
      expect(Object.keys(match.data()?.teamAssignments ?? {})).not.toContain(uid);
    });
  });

  it("tira o VIP da série e o banimento da partida", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();

      expect(
        (await getDoc(doc(database, "matchSeries", "serie-alheia", "vipPlayers", uid))).exists(),
      ).toBe(false);
      expect(
        (await getDoc(doc(database, "matches", ALHEIA, "bannedUsers", uid))).exists(),
      ).toBe(false);
    });
  });

  it("tira o autor das avaliações em que o uid era o id do documento", async () => {
    await seedEverything();

    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const matchRatings = await getDocs(
        collection(database, "matches", ALHEIA, "matchRatings"),
      );
      const organizerRatings = await getDocs(
        collection(database, "matches", ALHEIA, "organizerRatings"),
      );
      const skillRatings = await getDocs(collection(database, "profiles", OTHER, "skillRatings"));

      // A média está guardada no perfil de quem foi avaliado: apagar o
      // documento deixaria a nota de outra pessoa errada para sempre.
      expect(matchRatings.size).toBe(1);
      expect(matchRatings.docs[0].id).not.toContain(uid);
      expect(matchRatings.docs[0].data().raterUserId).toBeNull();

      expect(organizerRatings.size).toBe(1);
      expect(organizerRatings.docs[0].id).not.toContain(uid);
      expect(organizerRatings.docs[0].data().raterUserId).toBeNull();

      expect(skillRatings.size).toBe(1);
      expect(skillRatings.docs[0].id).not.toContain(uid);
      expect(skillRatings.docs[0].data().organizerId).toBeNull();
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

describe("verificação de conta", () => {
  let verifiedPhone: string;
  let phoneSequence = 0;

  // O emulador de Auth recusa o mesmo telefone em duas contas, e as contas dos
  // casos anteriores continuam lá: cada caso usa um número próprio.
  beforeEach(() => {
    phoneSequence += 1;
    verifiedPhone = `+55119123${45678 + phoneSequence}`;
  });

  async function setEnforcement(enforced: boolean | null) {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const ref = doc(context.firestore(), "config", "verification");
      if (enforced === null) {
        await setDoc(ref, {});
      } else {
        await setDoc(ref, {enforced});
      }
    });
  }

  /**
   * O emulador de Auth grava e-mail verificado e telefone pelo endpoint de
   * admin; as claims só entram no próximo token, por isso o login de novo.
   */
  async function verifyAccount(changes: {emailVerified?: boolean; phoneNumber?: string}): Promise<string> {
    const update = await fetch("http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:update?key=fake-api-key", {
      method: "POST",
      headers: {"content-type": "application/json", authorization: "Bearer owner"},
      body: JSON.stringify({localId: uid, ...changes}),
    });
    expect(update.ok, await update.clone().text()).toBe(true);
    const response = await fetch(
      "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key",
      {
        method: "POST",
        headers: {"content-type": "application/json"},
        body: JSON.stringify({email: userEmail, password: "correct-horse-battery-staple", returnSecureToken: true}),
      },
    );
    expect(response.ok, await response.clone().text()).toBe(true);
    return ((await response.json()) as {idToken: string}).idToken;
  }

  async function errorOf(response: Response): Promise<{status?: string; message?: string}> {
    return ((await response.json()) as {error?: {status?: string; message?: string}}).error ?? {};
  }

  it("com a exigência ligada, conta sem verificação não entra em partida", async () => {
    await setEnforcement(true);

    const response = await call("joinMatch", {matchId: "partida-inexistente"});
    const error = await errorOf(response);

    expect(error.status).toBe("FAILED_PRECONDITION");
    expect(error.message).toBe("Verification required: email");
  });

  it("com só o e-mail verificado, o que falta é o telefone", async () => {
    await setEnforcement(true);
    const token = await verifyAccount({emailVerified: true});

    const error = await errorOf(await call("joinMatch", {matchId: "partida-inexistente"}, token));

    expect(error.message).toBe("Verification required: phone");
  });

  it("conta verificada passa pela exigência e segue para a validação normal", async () => {
    await setEnforcement(true);
    const token = await verifyAccount({emailVerified: true, phoneNumber: verifiedPhone});

    const error = await errorOf(await call("joinMatch", {matchId: "partida-inexistente"}, token));

    expect(error.status).toBe("NOT_FOUND");
  });

  it("com a exigência desligada ou ausente, ninguém é barrado", async () => {
    await setEnforcement(false);
    expect((await errorOf(await call("joinMatch", {matchId: "partida-inexistente"}))).status).toBe("NOT_FOUND");

    await setEnforcement(null);
    expect((await errorOf(await call("joinMatch", {matchId: "partida-inexistente"}))).status).toBe("NOT_FOUND");
  });

  it("isentas continuam funcionando para conta sem verificação", async () => {
    await setEnforcement(true);

    const exported = await call("exportUserData", {});
    expect(exported.ok).toBe(true);

    const left = await errorOf(await call("leaveMatch", {matchId: "partida-inexistente"}));
    expect(left.status).toBe("NOT_FOUND");
    expect(left.message ?? "").not.toContain("Verification required");
  });

  it("sincronizar copia o telefone assinado para os dados privados", async () => {
    const token = await verifyAccount({emailVerified: true, phoneNumber: verifiedPhone});

    const response = await call("syncVerificationStatus", {}, token);
    expect(response.ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      const privateData = await getDoc(doc(database, "profiles", uid, "private", "data"));
      const profile = await getDoc(doc(database, "profiles", uid));

      expect(privateData.data()?.phone).toBe(verifiedPhone);
      expect(profile.data()).toMatchObject({emailVerified: true, phoneVerified: true});
    });
  });

  it("sem telefone na conta, sincronizar não grava telefone", async () => {
    const token = await verifyAccount({emailVerified: true});

    expect((await call("syncVerificationStatus", {}, token)).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const privateData = await getDoc(doc(context.firestore(), "profiles", uid, "private", "data"));
      expect(privateData.data()?.phone ?? null).toBeNull();
    });
  });
});
