import {describe, expect, it} from "vitest";
import {
  defaultPrivateData,
  defaultProfile,
  defaultSubscription,
  missingFields,
  provisionedClaims,
} from "../../src/provisioning.js";

const NOW = "server-timestamp";

describe("default documents", () => {
  it("builds the public profile from the Auth record", () => {
    expect(defaultProfile({displayName: "Ana", photoURL: "https://foto"}, NOW)).toMatchObject({
      fullName: "Ana",
      avatarUrl: "https://foto",
      rating: 0,
      ratingCount: 0,
      matchesPlayed: 0,
      isBanned: false,
      createdAt: NOW,
    });
  });

  it("fills blanks when the Auth record has no name or photo", () => {
    expect(defaultProfile({}, NOW)).toMatchObject({fullName: "", avatarUrl: null});
  });

  it("starts the private doc available so the person hears about matches", () => {
    expect(defaultPrivateData({email: "a@b.c", phoneNumber: "+5511999999999"}, NOW)).toMatchObject({
      email: "a@b.c",
      phone: "+5511999999999",
      isAvailable: true,
      availableUntil: null,
    });
  });

  it("starts on the free plan", () => {
    expect(defaultSubscription(NOW)).toMatchObject({plan: "free", status: "active"});
  });
});

describe("missingFields", () => {
  const defaults = {rating: 0, isBanned: false, fullName: ""};

  it("writes every default when the document does not exist", () => {
    expect(missingFields(undefined, defaults)).toEqual(defaults);
  });

  it("only fills what is missing and never overwrites what is there", () => {
    expect(missingFields({rating: 4.5, emailVerified: true}, defaults)).toEqual({isBanned: false, fullName: ""});
  });

  it("keeps a field that exists with a null value", () => {
    expect(missingFields({rating: 3, isBanned: true, fullName: null}, defaults)).toBeNull();
  });

  it("writes nothing when the document is complete", () => {
    expect(missingFields({rating: 3, isBanned: true, fullName: "Ana"}, defaults)).toBeNull();
  });
});

describe("provisionedClaims", () => {
  it("gives a new account the default claims", () => {
    expect(provisionedClaims(undefined)).toEqual({role: "user", plan: "free"});
  });

  it("keeps the claims the account already has", () => {
    expect(provisionedClaims({admin: true})).toEqual({admin: true, role: "user", plan: "free"});
  });

  it("does not downgrade a paying account", () => {
    expect(provisionedClaims({plan: "pro"})).toEqual({plan: "pro", role: "user"});
  });

  it("changes nothing when the account is already provisioned", () => {
    expect(provisionedClaims({role: "user", plan: "free"})).toBeNull();
  });
});
