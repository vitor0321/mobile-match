import {describe, expect, it} from "vitest";
import {shouldCancelOnOrganizerDeletion} from "../../src/index.js";

const now = 1_700_000_000_000;
const inTwoHours = Math.floor(now / 1_000) + 2 * 60 * 60;
const twoHoursAgo = Math.floor(now / 1_000) - 2 * 60 * 60;

describe("shouldCancelOnOrganizerDeletion", () => {
  it("cancela partida futura", () => {
    // Partida futura sem organizador não tem como acontecer; melhor que quem ia
    // jogar descubra agora e não na quadra.
    expect(shouldCancelOnOrganizerDeletion({status: "OPEN", startsAtSeconds: inTwoHours}, now))
      .toBe(true);
    expect(shouldCancelOnOrganizerDeletion({status: "FULL", startsAtSeconds: inTwoHours}, now))
      .toBe(true);
  });

  it("não mexe em partida que já aconteceu", () => {
    // Só o nome do organizador sai; o registro do jogo é histórico dos outros.
    expect(shouldCancelOnOrganizerDeletion({status: "OPEN", startsAtSeconds: twoHoursAgo}, now))
      .toBe(false);
  });

  it("não recancela nem mexe em partida encerrada", () => {
    expect(shouldCancelOnOrganizerDeletion({status: "CANCELLED", startsAtSeconds: inTwoHours}, now))
      .toBe(false);
    expect(shouldCancelOnOrganizerDeletion({status: "FINISHED", startsAtSeconds: inTwoHours}, now))
      .toBe(false);
    expect(shouldCancelOnOrganizerDeletion({status: "cancelled", startsAtSeconds: inTwoHours}, now))
      .toBe(false);
  });

  it("aceita o horário no formato Timestamp do Firestore", () => {
    expect(shouldCancelOnOrganizerDeletion({status: "OPEN", startsAt: {seconds: inTwoHours}}, now))
      .toBe(true);
  });

  it("sem horário legível, cancela — é a escolha conservadora", () => {
    expect(shouldCancelOnOrganizerDeletion({status: "OPEN"}, now)).toBe(true);
    expect(shouldCancelOnOrganizerDeletion({status: "OPEN", startsAtSeconds: "amanhã"}, now))
      .toBe(true);
  });

  it("status ausente é tratado como aberta", () => {
    expect(shouldCancelOnOrganizerDeletion({startsAtSeconds: inTwoHours}, now)).toBe(true);
    expect(shouldCancelOnOrganizerDeletion({startsAtSeconds: twoHoursAgo}, now)).toBe(false);
  });
});
