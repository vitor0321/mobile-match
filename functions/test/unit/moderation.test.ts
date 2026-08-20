import {describe, expect, it} from "vitest";
import {
  DAY_IN_MILLIS,
  MODERATION_LEVELS,
  REPORT_REASONS,
  SUSPENSION_DAYS,
  SUSPENSION_THRESHOLD,
  WARNING_THRESHOLD,
  REVIEW_THRESHOLD,
  isBlocked,
  isModerationLevel,
  isReportReason,
  manualModerationState,
  nextRatingAverage,
  parseRatingDimensions,
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

describe("manualModerationState", () => {
  const now = 1_700_000_000_000;

  it("suspende com prazo, usando o padrão quando não vem duração", () => {
    const state = manualModerationState("suspended", null, now);

    expect(state.untilMs).toBe(now + SUSPENSION_DAYS * DAY_IN_MILLIS);
    expect(state.isBanned).toBe(false);
  });

  it("respeita a duração informada", () => {
    expect(manualModerationState("suspended", 3, now).untilMs).toBe(now + 3 * DAY_IN_MILLIS);
  });

  it("ignora duração inválida em vez de gravar um prazo no passado", () => {
    expect(manualModerationState("suspended", -5, now).untilMs).toBe(
      now + SUSPENSION_DAYS * DAY_IN_MILLIS,
    );
    expect(manualModerationState("suspended", 0, now).untilMs).toBe(
      now + SUSPENSION_DAYS * DAY_IN_MILLIS,
    );
  });

  it("banimento não tem prazo e liga o espelho isBanned", () => {
    const state = manualModerationState("banned", 7, now);

    // profiles/{uid}.isBanned é o que a regra de criar partida e o filtro da
    // busca de jogadores leem.
    expect(state.isBanned).toBe(true);
    expect(state.untilMs).toBeNull();
  });

  it("none limpa punição e prazo", () => {
    const state = manualModerationState("none", 7, now);

    expect(state).toMatchObject({level: "none", untilMs: null, isBanned: false});
  });

  it("advertência não bloqueia nem marca prazo", () => {
    expect(manualModerationState("warning", null, now)).toMatchObject({
      untilMs: null,
      isBanned: false,
    });
  });

  it("decisão humana sempre tira a conta da fila de revisão", () => {
    // Se ficasse ligado, a conta voltaria ao painel para sempre.
    for (const level of MODERATION_LEVELS) {
      expect(manualModerationState(level, null, now).requiresReview).toBe(false);
    }
  });
});

describe("isModerationLevel", () => {
  it("aceita só os quatro níveis conhecidos", () => {
    expect(MODERATION_LEVELS.every(isModerationLevel)).toBe(true);
    expect(isModerationLevel("shadowban")).toBe(false);
    expect(isModerationLevel("BANNED")).toBe(false);
    expect(isModerationLevel(undefined)).toBe(false);
  });
});

describe("nextRatingAverage", () => {
  it("a primeira nota é a própria média", () => {
    // Perfis nascem com rating 0 e ratingCount 0, então a conta natural já dá
    // certo — não há caso especial de semente para desviar.
    expect(nextRatingAverage(0, 0, 4)).toBe(4);
    expect(nextRatingAverage(0, 0, 1)).toBe(1);
  });

  it("faz média corrente com o histórico", () => {
    // (5*3 + 3) / 4
    expect(nextRatingAverage(5, 3, 3)).toBe(4.5);
  });

  it("arredonda na casa pedida", () => {
    // (4*2 + 5) / 3 = 4.333...
    expect(nextRatingAverage(4, 2, 5)).toBe(4.33);
    expect(nextRatingAverage(4, 2, 5, 1)).toBe(4.3);
  });

  it("contador corrompido não propaga lixo para a média", () => {
    expect(nextRatingAverage(5, -1, 2)).toBe(2);
  });
});

describe("parseRatingDimensions", () => {
  const boom = (dimension: string) => {
    throw new Error(`invalid ${dimension}`);
  };
  const todas = {punctuality: 4, respect: 5, fairPlay: 3, behavior: 4};

  it("devolve as quatro dimensões", () => {
    expect(parseRatingDimensions({rating: 5, ...todas}, boom)).toEqual(todas);
  });

  it("exige todas — avaliação pela metade não existe", () => {
    // Aceitar parcial deixaria perfis com metade das médias agregadas e a outra
    // metade não, para sempre.
    expect(() => parseRatingDimensions({rating: 5}, boom)).toThrow("invalid punctuality");
    expect(() => parseRatingDimensions({...todas, respect: undefined}, boom)).toThrow(
      "invalid respect",
    );
    expect(() => parseRatingDimensions({...todas, behavior: null}, boom)).toThrow(
      "invalid behavior",
    );
  });

  it("recusa valor fora de 1..5", () => {
    expect(() => parseRatingDimensions({...todas, punctuality: 0}, boom)).toThrow(
      "invalid punctuality",
    );
    expect(() => parseRatingDimensions({...todas, respect: 6}, boom)).toThrow("invalid respect");
    expect(() => parseRatingDimensions({...todas, fairPlay: 4.5}, boom)).toThrow(
      "invalid fairPlay",
    );
    expect(() => parseRatingDimensions({...todas, behavior: "ótimo"}, boom)).toThrow(
      "invalid behavior",
    );
  });

  it("ignora chaves que não são dimensão", () => {
    expect(parseRatingDimensions({...todas, matchId: "m1", velocidade: 5}, boom)).toEqual(todas);
  });
});
