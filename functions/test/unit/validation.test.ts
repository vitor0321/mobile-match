import {HttpsError} from "firebase-functions/v2/https";
import {describe, expect, it} from "vitest";
import {
  parseBookExplanationRequest,
  parseTranslationRequest,
  parseVerseExplanationRequest,
  requireEmptyPayload,
} from "../../src/index.js";

describe("callable request validation", () => {
  it("accepts and normalizes a valid verse explanation request", () => {
    expect(parseVerseExplanationRequest({
      verseText: " João 3:16 ",
      bookName: " João ",
      chapterNumber: 3,
      verseNumber: 16,
      translation: " NVI ",
    })).toEqual({
      verseText: "João 3:16",
      bookName: "João",
      chapterNumber: 3,
      verseNumber: 16,
      translation: "NVI",
    });
  });

  it("rejects malformed callable payloads", () => {
    expect(() => parseBookExplanationRequest({bookId: 0, bookName: "Gênesis"}))
      .toThrow(HttpsError);
    expect(() => parseTranslationRequest({englishDefinition: ""}))
      .toThrow(HttpsError);
    expect(() => requireEmptyPayload({unexpected: true}))
      .toThrow(HttpsError);
  });
});
