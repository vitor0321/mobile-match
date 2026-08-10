import {readFile} from "node:fs/promises";
import {initializeTestEnvironment, type RulesTestEnvironment} from "@firebase/rules-unit-testing";
import {doc, getDoc, setDoc} from "firebase/firestore";
import {afterAll, afterEach, beforeAll, describe, expect, it} from "vitest";

const projectId = "match-ci";
let testEnvironment: RulesTestEnvironment;

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

afterEach(async () => {
  await testEnvironment.clearFirestore();
});

afterAll(async () => {
  await testEnvironment.cleanup();
});

describe("Firestore rules", () => {
  it("allows an owner to read and write their own favorites", async () => {
    const database = testEnvironment.authenticatedContext("owner").firestore();
    const favorite = doc(database, "users/owner/favorites/john-3-16");

    await expect(setDoc(favorite, {verseRef: "João 3:16"})).resolves.toBeUndefined();
    await expect(getDoc(favorite)).resolves.toMatchObject({exists: expect.any(Function)});
  });

  it("denies a different user and anonymous users from private data", async () => {
    const otherUserFavorite = doc(
      testEnvironment.authenticatedContext("other").firestore(),
      "users/owner/favorites/john-3-16",
    );
    const anonymousComment = doc(
      testEnvironment.unauthenticatedContext().firestore(),
      "users/owner/verse_comments/john-3-16",
    );

    await expect(getDoc(otherUserFavorite)).rejects.toThrow();
    await expect(setDoc(anonymousComment, {content: "No access"})).rejects.toThrow();
  });

  it("allows authenticated cache reads but denies every client cache write", async () => {
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "verse_explanations/john-3-16-NVI"), {
        explanation: "Cached explanation",
      });
    });

    const authenticatedDatabase = testEnvironment.authenticatedContext("reader").firestore();
    const cachedExplanation = doc(authenticatedDatabase, "verse_explanations/john-3-16-NVI");

    await expect(getDoc(cachedExplanation)).resolves.toMatchObject({exists: expect.any(Function)});
    await expect(setDoc(cachedExplanation, {explanation: "Tampered"})).rejects.toThrow();
  });
});
