import {afterAll, describe, expect, it} from "vitest";

const authEmulatorUrl = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key";
const createdUserIds: string[] = [];

afterAll(async () => {
  await Promise.all(createdUserIds.map((localId) =>
    fetch(`http://127.0.0.1:9099/emulator/v1/projects/match-ci/accounts/${localId}`, {
      method: "DELETE",
    }),
  ));
});

describe("Authentication Emulator", () => {
  it("creates a deterministic user for callable integration scenarios", async () => {
    const response = await fetch(authEmulatorUrl, {
      method: "POST",
      headers: {"content-type": "application/json"},
      body: JSON.stringify({
        email: `emulator-${Date.now()}@match.test`,
        password: "correct-horse-battery-staple",
        returnSecureToken: true,
      }),
    });

    const payload = await response.json() as {localId?: string; idToken?: string};
    expect(response.ok).toBe(true);
    expect(payload.localId).toBeTruthy();
    expect(payload.idToken).toBeTruthy();
    createdUserIds.push(payload.localId!);
  });
});
