import {readFile} from "node:fs/promises";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import {Timestamp, deleteDoc, doc, getDoc, serverTimestamp, setDoc, updateDoc} from "firebase/firestore";
import {afterAll, afterEach, beforeAll, describe, expect, it} from "vitest";

const projectId = "match-ci";
const ORGANIZER = "organizer-uid";
const PLAYER = "player-uid";
const MATCH_ID = "match-1";

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

function asUser(uid: string) {
  return testEnvironment.authenticatedContext(uid).firestore();
}

function asAnonymous() {
  return testEnvironment.unauthenticatedContext().firestore();
}

function seed(work: (firestore: ReturnType<typeof asAnonymous>) => Promise<void>) {
  return testEnvironment.withSecurityRulesDisabled((context) =>
    work(context.firestore() as ReturnType<typeof asAnonymous>),
  );
}

function inHours(hours: number) {
  return Timestamp.fromDate(new Date(Date.now() + hours * 3_600_000));
}

function publicProfile(overrides: Record<string, unknown> = {}) {
  return {
    fullName: "Jogador Teste",
    nickname: null,
    avatarUrl: null,
    position: "Meia",
    level: "Livre",
    sports: ["futsal"],
    city: "São Paulo",
    neighborhood: "União dos Cegos",
    rating: 5,
    ratingCount: 0,
    matchesPlayed: 0,
    isBanned: false,
    // As regras exigem createdAt == request.time; o sentinel do servidor é o
    // único valor que satisfaz isso vindo do cliente.
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function matchPayload(overrides: Record<string, unknown> = {}) {
  return {
    organizerId: ORGANIZER,
    organizerName: "Organizador",
    sport: "futsal",
    title: "Futsal no Green Ball",
    venue: "Green Ball",
    address: "Rua das Quadras, 100",
    neighborhood: "União dos Cegos",
    lat: -23.5505,
    lng: -46.6333,
    geohash: "6gyf4bf8m",
    startsAt: inHours(6),
    durationMin: 60,
    totalSlots: 14,
    confirmedCount: 0,
    waitlistCount: 0,
    priceCents: 2000,
    platformFeeCents: 0,
    level: "Livre",
    rules: ["Chuteira society", "Chegar 10 min antes"],
    pixKey: null,
    status: "open",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

describe("profiles — parte pública", () => {
  it("qualquer autenticado lê o perfil público; anônimo não", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "profiles", ORGANIZER), publicProfile());
    });

    await assertSucceeds(getDoc(doc(asUser(PLAYER), "profiles", ORGANIZER)));
    await assertFails(getDoc(doc(asAnonymous(), "profiles", ORGANIZER)));
  });

  it("o dono cria o próprio perfil com os valores iniciais do servidor", async () => {
    await assertSucceeds(setDoc(doc(asUser(PLAYER), "profiles", PLAYER), publicProfile()));
  });

  it("nega criar perfil de outro usuário", async () => {
    await assertFails(setDoc(doc(asUser(PLAYER), "profiles", ORGANIZER), publicProfile()));
  });

  it("nega nascer com reputação inflada", async () => {
    await assertFails(
      setDoc(doc(asUser(PLAYER), "profiles", PLAYER), publicProfile({rating: 5, matchesPlayed: 120})),
    );
  });

  it("o dono edita nome e posição", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "profiles", PLAYER), publicProfile());
    });

    await assertSucceeds(
      updateDoc(doc(asUser(PLAYER), "profiles", PLAYER), {
        fullName: "Novo Nome",
        position: "Fixo",
        updatedAt: Timestamp.now(),
      }),
    );
  });

  it("nega o usuário mexer em reputação ou tirar o próprio banimento", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "profiles", PLAYER), publicProfile({isBanned: true}));
    });

    const profile = doc(asUser(PLAYER), "profiles", PLAYER);
    await assertFails(updateDoc(profile, {rating: 5}));
    await assertFails(updateDoc(profile, {matchesPlayed: 999}));
    await assertFails(updateDoc(profile, {isBanned: false}));
  });
});

describe("profiles/private — telefone, Pix, geo e disponibilidade", () => {
  it("só o dono lê e escreve os dados privados", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "profiles", PLAYER, "private", "data"), {
        phone: "+5511999999999",
        pixKey: "player@email.com",
        lat: -23.55,
        lng: -46.63,
        geohash: "6gyf4bf8m",
        radiusKm: 15,
        isAvailable: true,
      });
    });

    const ownPrivate = doc(asUser(PLAYER), "profiles", PLAYER, "private", "data");
    const otherPrivate = doc(asUser(ORGANIZER), "profiles", PLAYER, "private", "data");

    await assertSucceeds(getDoc(ownPrivate));
    await assertSucceeds(updateDoc(ownPrivate, {isAvailable: false}));

    // É este teste que garante que telefone e chave Pix não vazam com o
    // perfil público — no Postgres os dois campos ficavam na mesma linha.
    await assertFails(getDoc(otherPrivate));
    await assertFails(updateDoc(otherPrivate, {phone: "+5511000000000"}));
    await assertFails(getDoc(doc(asAnonymous(), "profiles", PLAYER, "private", "data")));
  });
});

describe("matches", () => {
  it("o organizador cria a própria partida", async () => {
    await assertSucceeds(setDoc(doc(asUser(ORGANIZER), "matches", MATCH_ID), matchPayload()));
  });

  it("nega criar partida em nome de outro organizador", async () => {
    await assertFails(
      setDoc(doc(asUser(PLAYER), "matches", MATCH_ID), matchPayload()),
    );
  });

  it("nega partida no passado, lotação fora da faixa e contadores adiantados", async () => {
    const matchRef = doc(asUser(ORGANIZER), "matches", MATCH_ID);

    await assertFails(setDoc(matchRef, matchPayload({startsAt: inHours(-2)})));
    await assertFails(setDoc(matchRef, matchPayload({totalSlots: 1})));
    await assertFails(setDoc(matchRef, matchPayload({totalSlots: 500})));
    await assertFails(setDoc(matchRef, matchPayload({confirmedCount: 12})));
    await assertFails(setDoc(matchRef, matchPayload({status: "full"})));
  });

  it("nega usuário banido criar partida", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "profiles", ORGANIZER), publicProfile({isBanned: true}));
    });

    await assertFails(setDoc(doc(asUser(ORGANIZER), "matches", MATCH_ID), matchPayload()));
  });

  it("o organizador edita os dados da partida, mas não os contadores", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "matches", MATCH_ID), matchPayload({confirmedCount: 10}));
    });

    const matchRef = doc(asUser(ORGANIZER), "matches", MATCH_ID);

    await assertSucceeds(updateDoc(matchRef, {venue: "Arena Central", updatedAt: Timestamp.now()}));
    await assertSucceeds(updateDoc(matchRef, {status: "cancelled", updatedAt: Timestamp.now()}));

    // Contadores são a fonte da verdade da lotação: só as callables escrevem.
    await assertFails(updateDoc(matchRef, {confirmedCount: 0}));
    await assertFails(updateDoc(matchRef, {waitlistCount: 0}));
    await assertFails(updateDoc(matchRef, {status: "finished"}));
    // Encolher a partida abaixo de quem já está confirmado deixaria gente fora.
    await assertFails(updateDoc(matchRef, {totalSlots: 6}));
  });

  it("nega um terceiro editar a partida", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "matches", MATCH_ID), matchPayload());
    });

    await assertFails(updateDoc(doc(asUser(PLAYER), "matches", MATCH_ID), {venue: "Sequestrada"}));
  });

  it("apaga partida vazia, mas exige cancelamento quando já tem gente", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "matches", "vazia"), matchPayload());
      await setDoc(doc(database, "matches", "cheia"), matchPayload({confirmedCount: 4}));
    });

    await assertSucceeds(deleteDoc(doc(asUser(ORGANIZER), "matches", "vazia")));
    await assertFails(deleteDoc(doc(asUser(ORGANIZER), "matches", "cheia")));
  });
});

describe("matches/participants — a trava contra overbooking", () => {
  it("qualquer autenticado lê a lista, ninguém escreve pelo cliente", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "matches", MATCH_ID), matchPayload());
      await setDoc(doc(database, "matches", MATCH_ID, "participants", PLAYER), {
        userId: PLAYER,
        status: "confirmed",
        paymentStatus: "pending",
        order: 1,
      });
    });

    await assertSucceeds(getDoc(doc(asUser(ORGANIZER), "matches", MATCH_ID, "participants", PLAYER)));

    // Entrar, sair e pagar passam por joinMatch/leaveMatch/confirmPixPayment.
    await assertFails(
      setDoc(doc(asUser(PLAYER), "matches", MATCH_ID, "participants", PLAYER), {
        userId: PLAYER,
        status: "confirmed",
      }),
    );
    await assertFails(
      updateDoc(doc(asUser(PLAYER), "matches", MATCH_ID, "participants", PLAYER), {
        paymentStatus: "paid",
      }),
    );
    await assertFails(
      updateDoc(doc(asUser(ORGANIZER), "matches", MATCH_ID, "participants", PLAYER), {
        status: "confirmed",
      }),
    );
  });

  it("avaliação da partida não é gravada direto pelo cliente", async () => {
    await assertFails(
      setDoc(doc(asUser(PLAYER), "matches", MATCH_ID, "ratings", PLAYER), {
        ratedId: ORGANIZER,
        stars: 5,
      }),
    );
  });
});

describe("users — notificações, pagamentos, assinatura e dispositivos", () => {
  it("o dono só marca a notificação como lida ou apaga", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "users", PLAYER, "notifications", "n1"), {
        type: "new_match",
        title: "Faltam 2 vagas: Green Ball",
        readAt: null,
      });
    });

    const own = doc(asUser(PLAYER), "users", PLAYER, "notifications", "n1");

    await assertSucceeds(getDoc(own));
    await assertSucceeds(updateDoc(own, {readAt: Timestamp.now()}));
    await assertFails(updateDoc(own, {title: "Título forjado"}));
    await assertFails(setDoc(doc(asUser(PLAYER), "users", PLAYER, "notifications", "n2"), {title: "Fake"}));
    await assertSucceeds(deleteDoc(own));

    await assertFails(getDoc(doc(asUser(ORGANIZER), "users", PLAYER, "notifications", "n1")));
  });

  it("o dono marca a notificação do histórico como lida (isRead) ou apaga", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "users", PLAYER, "notificationHistory", "h1"), {
        id: "h1",
        title: "Você foi promovido!",
        body: "Uma vaga abriu no Green Ball",
        receivedAt: Date.now(),
        isRead: false,
        data: {matchId: MATCH_ID},
      });
    });

    const own = doc(asUser(PLAYER), "users", PLAYER, "notificationHistory", "h1");

    await assertSucceeds(getDoc(own));
    await assertSucceeds(updateDoc(own, {isRead: true}));
    // Só o campo isRead pode mudar — forjar o título é negado.
    await assertFails(updateDoc(own, {title: "Título forjado"}));
    // Criar histórico direto é negado (vem das Functions ao receber o push).
    await assertFails(
      setDoc(doc(asUser(PLAYER), "users", PLAYER, "notificationHistory", "h2"), {title: "Fake"}),
    );
    await assertSucceeds(deleteDoc(own));

    // Ninguém lê o histórico de outro usuário.
    await assertFails(getDoc(doc(asUser(ORGANIZER), "users", PLAYER, "notificationHistory", "h1")));
  });

  it("pagamento e assinatura são só de leitura para o dono", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "users", PLAYER, "payments", "p1"), {
        matchId: MATCH_ID,
        amountCents: 2000,
        status: "pending",
      });
      await setDoc(doc(database, "users", PLAYER, "subscription", "current"), {
        plan: "free",
        status: "paid",
      });
    });

    await assertSucceeds(getDoc(doc(asUser(PLAYER), "users", PLAYER, "payments", "p1")));
    await assertFails(updateDoc(doc(asUser(PLAYER), "users", PLAYER, "payments", "p1"), {status: "paid"}));

    await assertSucceeds(getDoc(doc(asUser(PLAYER), "users", PLAYER, "subscription", "current")));
    await assertFails(
      updateDoc(doc(asUser(PLAYER), "users", PLAYER, "subscription", "current"), {plan: "business"}),
    );
  });

  it("o dono registra e remove o próprio dispositivo de push", async () => {
    const token = doc(asUser(PLAYER), "users", PLAYER, "devices", "fcm-token-1");

    await assertSucceeds(setDoc(token, {userId: PLAYER, platform: "android"}));
    await assertSucceeds(deleteDoc(token));

    await assertFails(
      setDoc(doc(asUser(ORGANIZER), "users", PLAYER, "devices", "fcm-token-2"), {
        userId: PLAYER,
        platform: "ios",
      }),
    );
  });
});

describe("moderação e denúncias", () => {
  it("denúncia não é criada direto e só o autor e o admin leem", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "reports", "r1"), {
        reporterId: PLAYER,
        reportedUserId: ORGANIZER,
        reason: "nao_comparecimento",
        status: "open",
      });
    });

    await assertFails(setDoc(doc(asUser(PLAYER), "reports", "r2"), {reporterId: PLAYER, reason: "fraude"}));
    await assertSucceeds(getDoc(doc(asUser(PLAYER), "reports", "r1")));
    await assertFails(getDoc(doc(asUser(ORGANIZER), "reports", "r1")));
  });

  it("o usuário lê o próprio status de moderação, mas não o altera", async () => {
    await seed(async (database) => {
      await setDoc(doc(database, "moderation", PLAYER), {level: "suspended", reason: "denúncias"});
    });

    await assertSucceeds(getDoc(doc(asUser(PLAYER), "moderation", PLAYER)));
    await assertFails(updateDoc(doc(asUser(PLAYER), "moderation", PLAYER), {level: "warning"}));
    await assertFails(getDoc(doc(asUser(ORGANIZER), "moderation", PLAYER)));
  });
});

describe("fechamento padrão", () => {
  it("nega qualquer caminho não previsto nas regras", async () => {
    const stray = doc(asUser(PLAYER), "colecao_desconhecida", "x");

    await assertFails(getDoc(stray));
    await assertFails(setDoc(stray, {qualquer: "coisa"}));
  });

  it("não sobrou nenhuma coleção do Lexis nas regras", async () => {
    const rules = await readFile("../firestore.rules", "utf8");

    expect(rules).not.toMatch(/verse|book_explanations|favorites/i);
  });
});
