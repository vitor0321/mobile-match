import {describe, expect, it} from "vitest";
import {
  DAY_IN_MILLIS,
  REPORT_REASONS,
  SUSPENSION_THRESHOLD,
  WARNING_THRESHOLD,
  REVIEW_THRESHOLD,
  isBlocked,
  isReportReason,
  levelForReporterCount,
  requiresHumanReview,
} from "../../src/moderation.js";

describe("report reasons", () => {
  it("is the exact wire contract shared with the Kotlin client", () => {
    // ReportReason.kt mirrors this list. Renaming an id orphans reports that
    // were already stored, so this test exists to make a rename loud.
    expect([...REPORT_REASONS]).toEqual([
      "no_show",
      "late",
      "no_payment",
      "aggressive_behavior",
      "verbal_abuse",
      "discrimination",
      "harassment",
      "dangerous_play",
      "fake_profile",
      "other",
    ]);
  });

  it("rejects anything not in the list", () => {
    expect(isReportReason("no_show")).toBe(true);
    expect(isReportReason("NO_SHOW")).toBe(false);
    expect(isReportReason("whatever")).toBe(false);
    expect(isReportReason(undefined)).toBe(false);
    expect(isReportReason(3)).toBe(false);
  });
});

describe("escalation", () => {
  it("does nothing below the warning threshold", () => {
    expect(levelForReporterCount(0)).toBe("none");
    expect(levelForReporterCount(WARNING_THRESHOLD - 1)).toBe("none");
  });

  it("warns, then suspends", () => {
    expect(levelForReporterCount(WARNING_THRESHOLD)).toBe("warning");
    expect(levelForReporterCount(SUSPENSION_THRESHOLD - 1)).toBe("warning");
    expect(levelForReporterCount(SUSPENSION_THRESHOLD)).toBe("suspended");
  });

  it("never bans automatically", () => {
    // Banning on report count alone is a brigading vector: a coordinated group
    // could remove any player. Past the review threshold the account is
    // suspended and flagged — a human decides the ban.
    expect(levelForReporterCount(REVIEW_THRESHOLD)).toBe("suspended");
    expect(levelForReporterCount(1_000)).toBe("suspended");
    expect(requiresHumanReview(REVIEW_THRESHOLD - 1)).toBe(false);
    expect(requiresHumanReview(REVIEW_THRESHOLD)).toBe(true);
  });
});

describe("isBlocked", () => {
  const now = 1_700_000_000_000;

  it("lets an unmoderated account through", () => {
    expect(isBlocked(undefined, now)).toBe(false);
    expect(isBlocked({level: "none"}, now)).toBe(false);
  });

  it("does not block on a warning", () => {
    // A warning is a heads-up, not a punishment.
    expect(isBlocked({level: "warning"}, now)).toBe(false);
  });

  it("blocks a suspension until its deadline", () => {
    const until = now + 3 * DAY_IN_MILLIS;
    expect(isBlocked({level: "suspended", untilMs: until}, now)).toBe(true);
    expect(isBlocked({level: "suspended", untilMs: until}, until)).toBe(false);
    expect(isBlocked({level: "suspended", untilMs: until}, until + 1)).toBe(false);
  });

  it("treats a suspension with no deadline as active", () => {
    // The missing field is a write bug; unblocking because of it would be the
    // more expensive mistake.
    expect(isBlocked({level: "suspended"}, now)).toBe(true);
    expect(isBlocked({level: "suspended", untilMs: "soon"}, now)).toBe(true);
  });

  it("blocks a ban forever", () => {
    expect(isBlocked({level: "banned"}, now)).toBe(true);
    expect(isBlocked({level: "banned", untilMs: now - DAY_IN_MILLIS}, now)).toBe(true);
  });
});
