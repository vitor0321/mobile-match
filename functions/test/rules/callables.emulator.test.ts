import {readFile} from "node:fs/promises";
import {initializeTestEnvironment, type RulesTestEnvironment} from "@firebase/rules-unit-testing";
import {doc, getDoc, setDoc} from "firebase/firestore";
import {afterAll, afterEach, beforeAll, beforeEach, describe, expect, it} from "vitest";

const projectId = "match-ci";
const functionsBaseUrl = `http://127.0.0.1:5001/${projectId}/southamerica-east1`;
const authUrl = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key";

let testEnvironment: RulesTestEnvironment;
let idToken: string;
let uid: string;

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
  const response = await fetch(authUrl, {
    method: "POST",
    headers: {"content-type": "application/json"},
    body: JSON.stringify({
      email: `callable-${Date.now()}-${Math.random()}@match.test`,
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
        rating: 5,
        ratingCount: 0,
        matchesPlayed: 0,
        isBanned: false,
      });

      const privateData = await getDoc(doc(database, "profiles", uid, "private", "data"));
      expect(privateData.exists()).toBe(true);
      expect(privateData.data()).toMatchObject({isAvailable: false});

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
        rating: 5,
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
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5}, null);
    expect(response.status).toBe(401);
  });

  it("rejects rating yourself", async () => {
    await seedFinishedMatch();
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: uid, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a star count outside 1..5", async () => {
    await seedFinishedMatch();
    for (const rating of [0, 6, 4.5]) {
      const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating});
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
      comment: "x".repeat(501),
    });
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("INVALID_ARGUMENT");
  });

  it("rejects a match that has not finished yet", async () => {
    await seedFinishedMatch({startsAtSeconds: Math.floor(Date.now() / 1000) + 3600});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a cancelled match", async () => {
    await seedFinishedMatch({status: "CANCELLED"});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("rejects a caller who did not play the match", async () => {
    await seedFinishedMatch({participants: [RATED]});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.status).toBe(403);
    expect(await response.text()).toContain("PERMISSION_DENIED");
  });

  it("rejects rating someone who did not play the match", async () => {
    await seedFinishedMatch({participants: [uid]});
    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 5});
    expect(response.status).toBe(400);
    expect(await response.text()).toContain("FAILED_PRECONDITION");
  });

  it("writes both copies and replaces the seed average on the first rating", async () => {
    await seedFinishedMatch();

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

    // The profile is seeded with rating 5 / ratingCount 0. That 5 is a display
    // placeholder, not a review, so it must not drag the first average up.
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

    const response = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 3});
    expect(response.ok).toBe(true);
    // (5*3 + 3) / 4 = 4.5
    expect(await response.json()).toMatchObject({result: {averageRating: 4.5, ratingCount: 4}});
  });

  it("is idempotent — resending does not inflate the average", async () => {
    await seedFinishedMatch();

    expect((await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 4})).ok).toBe(true);

    const second = await call("submitPlayerRating", {matchId: MATCH, ratedUserId: RATED, rating: 1});
    expect(second.ok).toBe(true);
    expect(await second.json()).toMatchObject({
      result: {status: "already_rated", averageRating: 4, ratingCount: 1},
    });

    expect((await readProfile()).data()).toMatchObject({rating: 4, ratingCount: 1});
  });
});
