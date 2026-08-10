import {readFile} from "node:fs/promises";
import {initializeTestEnvironment, type RulesTestEnvironment} from "@firebase/rules-unit-testing";
import {doc, getDoc, setDoc} from "firebase/firestore";
import {afterAll, afterEach, beforeAll, beforeEach, describe, expect, it} from "vitest";

const projectId = "match-ci";
const functionsBaseUrl = `http://127.0.0.1:5001/${projectId}/southamerica-east1`;
const authUrl = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key";
const secretMarker = "SHOULD_NEVER_APPEAR_IN_A_FUNCTION_RESPONSE";

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

describe("callable functions", () => {
  it("rejects unauthenticated requests without leaking sensitive data", async () => {
    const response = await call("deleteAccount", {}, null);
    const body = await response.text();

    expect(response.status).toBe(401);
    expect(body).toContain("UNAUTHENTICATED");
    expect(body).not.toContain(secretMarker);
  });

  it("rejects malformed authenticated AI payloads before generation", async () => {
    const response = await call("getVerseExplanation", {verseText: secretMarker});
    const body = await response.text();

    expect(response.status).toBe(400);
    expect(body).toContain("INVALID_ARGUMENT");
    expect(body).not.toContain(secretMarker);
  });

  it("returns a cached AI explanation without invoking an external generator", async () => {
    const verseRef = "João 3:16-NVI";
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "verse_explanations", verseRef), {
        ...versePayload(),
        verseRef,
        explanation: "Cached explanation",
      });
    });

    const response = await call("getVerseExplanation", versePayload());
    const body = await response.json() as {result: {explanation: string; verseRef: string}};

    expect(response.ok).toBe(true);
    expect(body.result).toEqual({
      ...versePayload(),
      verseRef,
      explanation: "Cached explanation",
    });
  });

  it("enforces the AI rate limit before a cache miss can reach the generator", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "ai_rate_limits", uid), {
        count: 20,
        windowStartedAt: new Date(),
      });
    });

    const response = await call("getVerseExplanation", versePayload());
    const body = await response.text();

    expect(response.status).toBe(429);
    expect(body).toContain("RESOURCE_EXHAUSTED");
    expect(body).not.toContain(secretMarker);
  });

  it("deletes only the authenticated users data and remains idempotent", async () => {
    const otherUid = "other-user";
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "users", uid), {profile: true});
      await setDoc(doc(database, "users", uid, "favorites", "john-3-16"), {verseRef: "João 3:16"});
      await setDoc(doc(database, "users", otherUid), {profile: true});
      await setDoc(doc(database, "ai_rate_limits", uid), {count: 1});
    });

    expect((await call("deleteAccount", {})).ok).toBe(true);
    expect((await call("deleteAccount", {})).ok).toBe(true);

    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      expect((await getDoc(doc(database, "users", uid))).exists()).toBe(false);
      expect((await getDoc(doc(database, "users", otherUid))).exists()).toBe(true);
      expect((await getDoc(doc(database, "ai_rate_limits", uid))).exists()).toBe(false);
    });
  });
});

function versePayload() {
  return {
    verseText: "Porque Deus amou o mundo",
    bookName: "João",
    chapterNumber: 3,
    verseNumber: 16,
    translation: "NVI",
  };
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
