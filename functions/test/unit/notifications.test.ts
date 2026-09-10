import {describe, expect, it} from "vitest";
import {
  MAX_NOTIFY_RADIUS_KM,
  MIN_NOTIFY_RADIUS_KM,
  type MatchInvite,
  type NotificationCandidate,
  effectiveRadiusKm,
  isWaitlistPromotion,
  parseCandidate,
  selectRecipients,
} from "../../src/notifications.js";

// Porto Alegre, mais ou menos no centro.
const MATCH: MatchInvite = {
  matchId: "m1",
  organizerId: "organizador",
  sport: "futsal",
  lat: -30.0346,
  lng: -51.2177,
};

/**
 * ~1 km por 0.009 grau de latitude nessa faixa.
 *
 * Nasce disponível porque os testes de raio, esporte e teto são sobre outra
 * coisa — a disponibilidade tem os testes dela mais abaixo.
 */
function candidateAtKm(userId: string, km: number, overrides: Partial<NotificationCandidate> = {}) {
  return {
    userId,
    lat: MATCH.lat + km * 0.009,
    lng: MATCH.lng,
    radiusKm: 15,
    availableSports: [],
    isAvailable: true,
    availableUntilMs: null,
    ...overrides,
  } satisfies NotificationCandidate;
}

describe("effectiveRadiusKm", () => {
  it("eleva um raio pequeno até o mínimo da regra B4", () => {
    // Procurar e ser avisado são intenções diferentes: quem busca em 5 km
    // ainda quer saber de partida a 15.
    expect(effectiveRadiusKm(5)).toBe(MIN_NOTIFY_RADIUS_KM);
    expect(effectiveRadiusKm(0)).toBe(MIN_NOTIFY_RADIUS_KM);
  });

  it("respeita um raio maior, até o teto", () => {
    expect(effectiveRadiusKm(35)).toBe(35);
    expect(effectiveRadiusKm(999)).toBe(MAX_NOTIFY_RADIUS_KM);
  });

  it("trata valor ausente ou inválido como zero", () => {
    expect(effectiveRadiusKm(undefined)).toBe(MIN_NOTIFY_RADIUS_KM);
    expect(effectiveRadiusKm("perto")).toBe(MIN_NOTIFY_RADIUS_KM);
    expect(effectiveRadiusKm(Number.NaN)).toBe(MIN_NOTIFY_RADIUS_KM);
  });
});

describe("selectRecipients", () => {
  it("nunca avisa o próprio organizador", () => {
    const recipients = selectRecipients(
      [candidateAtKm("organizador", 1), candidateAtKm("outro", 1)],
      MATCH,
    );

    expect(recipients.map((r) => r.userId)).toEqual(["outro"]);
  });

  it("corta quem está além do próprio raio efetivo", () => {
    const recipients = selectRecipients(
      [
        candidateAtKm("dentro", 18, {radiusKm: 5}),
        candidateAtKm("fora", 25, {radiusKm: 5}),
      ],
      MATCH,
    );

    // Os dois pediram raio de 5 km; a regra B4 eleva para 20.
    expect(recipients.map((r) => r.userId)).toEqual(["dentro"]);
  });

  it("honra um raio maior que o mínimo", () => {
    const recipients = selectRecipients([candidateAtKm("longe", 30, {radiusKm: 40})], MATCH);

    expect(recipients.map((r) => r.userId)).toEqual(["longe"]);
  });

  it("ignora quem está além do teto mesmo com raio absurdo", () => {
    const recipients = selectRecipients([candidateAtKm("distante", 80, {radiusKm: 500})], MATCH);

    expect(recipients).toHaveLength(0);
  });

  it("lista de esportes vazia significa tanto faz", () => {
    const recipients = selectRecipients([candidateAtKm("qualquer", 1)], MATCH);

    // É o padrão de todo perfil novo. Filtrar aqui seria não notificar ninguém.
    expect(recipients).toHaveLength(1);
  });

  it("respeita a preferência quando existe, sem diferenciar caixa", () => {
    const recipients = selectRecipients(
      [
        candidateAtKm("futsal", 1, {availableSports: ["FUTSAL"]}),
        candidateAtKm("volei", 1, {availableSports: ["volei"]}),
      ],
      MATCH,
    );

    expect(recipients.map((r) => r.userId)).toEqual(["futsal"]);
  });

  it("ordena do mais perto para o mais longe", () => {
    const recipients = selectRecipients(
      [candidateAtKm("longe", 15), candidateAtKm("perto", 2), candidateAtKm("medio", 8)],
      MATCH,
    );

    expect(recipients.map((r) => r.userId)).toEqual(["perto", "medio", "longe"]);
  });

  it("ao estourar o teto, quem fica de fora são os mais distantes", () => {
    const candidates = [
      candidateAtKm("c15", 15),
      candidateAtKm("c02", 2),
      candidateAtKm("c09", 9),
    ];

    const recipients = selectRecipients(candidates, MATCH, 2);

    expect(recipients.map((r) => r.userId)).toEqual(["c02", "c09"]);
  });

  it("devolve a distância calculada junto", () => {
    const [recipient] = selectRecipients([candidateAtKm("perto", 5)], MATCH);

    expect(recipient.distanceKm).toBeGreaterThan(4);
    expect(recipient.distanceKm).toBeLessThan(6);
  });
});

describe("selectRecipients — disponibilidade (regra B5)", () => {
  const NOW = 1_700_000_000_000;

  it("não avisa quem está com o toggle desligado", () => {
    const recipients = selectRecipients(
      [candidateAtKm("indisponivel", 1, {isAvailable: false})],
      MATCH,
      undefined,
      NOW,
    );

    expect(recipients).toEqual([]);
  });

  it("avisa quem está disponível sem vencimento", () => {
    // `availableUntilMs` nulo é "até eu desligar", que é o que o toggle grava.
    const recipients = selectRecipients(
      [candidateAtKm("disponivel", 1, {availableUntilMs: null})],
      MATCH,
      undefined,
      NOW,
    );

    expect(recipients.map((r) => r.userId)).toEqual(["disponivel"]);
  });

  it("não avisa quem tem a janela vencida, mesmo com o toggle ligado", () => {
    // Vencer vale como desligado sem ninguém precisar varrer a base.
    const recipients = selectRecipients(
      [candidateAtKm("vencido", 1, {isAvailable: true, availableUntilMs: NOW - 1})],
      MATCH,
      undefined,
      NOW,
    );

    expect(recipients).toEqual([]);
  });

  it("a janela vale até o último instante", () => {
    const aberta = selectRecipients(
      [candidateAtKm("u", 1, {availableUntilMs: NOW + 1})],
      MATCH,
      undefined,
      NOW,
    );
    const fechada = selectRecipients(
      [candidateAtKm("u", 1, {availableUntilMs: NOW})],
      MATCH,
      undefined,
      NOW,
    );

    expect(aberta).toHaveLength(1);
    expect(fechada).toHaveLength(0);
  });

  it("o filtro convive com raio e esporte, sem atropelar nenhum", () => {
    const recipients = selectRecipients(
      [
        candidateAtKm("perto-disponivel", 1),
        candidateAtKm("perto-indisponivel", 2, {isAvailable: false}),
        candidateAtKm("longe-disponivel", 999),
        candidateAtKm("outro-esporte", 3, {availableSports: ["VOLEI"]}),
      ],
      MATCH,
      undefined,
      NOW,
    );

    expect(recipients.map((r) => r.userId)).toEqual(["perto-disponivel"]);
  });
});

describe("parseCandidate", () => {
  const DEFAULT_RADIUS = 15;

  it("lê os campos do documento privado", () => {
    const candidate = parseCandidate(
      "u1",
      {
        lat: -30,
        lng: -51,
        radiusKm: 30,
        availableSports: ["futsal", "volei"],
        isAvailable: true,
        availableUntil: 1_700_000_000_000,
      },
      DEFAULT_RADIUS,
    );

    expect(candidate).toEqual({
      userId: "u1",
      lat: -30,
      lng: -51,
      radiusKm: 30,
      availableSports: ["futsal", "volei"],
      isAvailable: true,
      availableUntilMs: 1_700_000_000_000,
    });
  });

  it("trata disponibilidade ausente ou de tipo errado como indisponível", () => {
    // Na dúvida, não incomodar: é o lado seguro do erro para notificação.
    expect(parseCandidate("u1", {lat: -30, lng: -51}, DEFAULT_RADIUS)?.isAvailable).toBe(false);
    expect(
      parseCandidate("u1", {lat: -30, lng: -51, isAvailable: "true"}, DEFAULT_RADIUS)?.isAvailable,
    ).toBe(false);
    expect(
      parseCandidate("u1", {lat: -30, lng: -51, isAvailable: 1}, DEFAULT_RADIUS)?.isAvailable,
    ).toBe(false);
  });

  it("aceita availableUntil como número ou Timestamp, e ignora o resto", () => {
    const read = (availableUntil: unknown) =>
      parseCandidate("u1", {lat: -30, lng: -51, availableUntil}, DEFAULT_RADIUS)?.availableUntilMs;

    expect(read(1_700_000_000_000)).toBe(1_700_000_000_000);
    // É o que o SDK devolve quando o campo foi gravado como data.
    expect(read({toMillis: () => 1_700_000_000_000})).toBe(1_700_000_000_000);
    // Sem vencimento é null, e não zero — zero seria "venceu em 1970".
    expect(read(undefined)).toBeNull();
    expect(read(null)).toBeNull();
    expect(read("amanhã")).toBeNull();
    expect(read(Number.NaN)).toBeNull();
  });

  it("descarta quem não tem coordenada", () => {
    // Sem posição não dá para medir distância, e chutar o centro da cidade
    // notificaria a pessoa errada.
    expect(parseCandidate("u1", {lng: -51}, DEFAULT_RADIUS)).toBeNull();
    expect(parseCandidate("u1", {lat: null, lng: -51}, DEFAULT_RADIUS)).toBeNull();
    expect(parseCandidate("u1", {lat: "-30", lng: "-51"}, DEFAULT_RADIUS)).toBeNull();
    expect(parseCandidate("u1", {lat: Number.NaN, lng: -51}, DEFAULT_RADIUS)).toBeNull();
  });

  it("cai no raio padrão quando o campo não está lá", () => {
    const candidate = parseCandidate("u1", {lat: -30, lng: -51}, DEFAULT_RADIUS);

    expect(candidate?.radiusKm).toBe(DEFAULT_RADIUS);
  });

  it("limpa lixo da lista de esportes em vez de quebrar", () => {
    const candidate = parseCandidate(
      "u1",
      {lat: -30, lng: -51, availableSports: ["futsal", 7, null, "volei"]},
      DEFAULT_RADIUS,
    );

    expect(candidate?.availableSports).toEqual(["futsal", "volei"]);
  });

  it("trata availableSports ausente ou de tipo errado como lista vazia", () => {
    expect(parseCandidate("u1", {lat: -30, lng: -51}, DEFAULT_RADIUS)?.availableSports).toEqual([]);
    expect(
      parseCandidate("u1", {lat: -30, lng: -51, availableSports: "futsal"}, DEFAULT_RADIUS)
        ?.availableSports,
    ).toEqual([]);
  });
});

describe("isWaitlistPromotion", () => {
  it("reconhece fila -> confirmado", () => {
    expect(isWaitlistPromotion({isConfirmed: false}, {isConfirmed: true})).toBe(true);
  });

  it("entrar já confirmado não é promoção", () => {
    // Sem essa checagem o trigger diria "você subiu da fila" a cada escrita.
    expect(isWaitlistPromotion(undefined, {isConfirmed: true})).toBe(false);
  });

  it("sair não é promoção", () => {
    expect(isWaitlistPromotion({isConfirmed: true}, undefined)).toBe(false);
  });

  it("continuar no mesmo estado não é promoção", () => {
    expect(isWaitlistPromotion({isConfirmed: true}, {isConfirmed: true})).toBe(false);
    expect(isWaitlistPromotion({isConfirmed: false}, {isConfirmed: false})).toBe(false);
  });

  it("campo ausente ou de outro tipo não conta", () => {
    expect(isWaitlistPromotion({}, {isConfirmed: true})).toBe(false);
    expect(isWaitlistPromotion({isConfirmed: 0}, {isConfirmed: 1})).toBe(false);
  });
});
