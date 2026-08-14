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
