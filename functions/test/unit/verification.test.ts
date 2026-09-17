import {readFileSync} from "node:fs";
import {describe, expect, it} from "vitest";
import {
  ADMIN_CALLABLES,
  FULL_VERIFICATION,
  VERIFICATION_EXEMPT_CALLABLES,
  VERIFIED_CALLABLES,
  meetsRequirement,
  missingVerification,
  parseEnforcementFlag,
  phoneNumberFromClaims,
  verificationFromClaims,
} from "../../src/verification.js";

describe("verificationFromClaims", () => {
  it("lê o e-mail verificado da claim", () => {
    expect(verificationFromClaims({email_verified: true}).emailVerified).toBe(true);
    expect(verificationFromClaims({email_verified: false}).emailVerified).toBe(false);
  });

  it("exige o booleano, não um valor parecido", () => {
    expect(verificationFromClaims({email_verified: "true"}).emailVerified).toBe(false);
    expect(verificationFromClaims({email_verified: 1}).emailVerified).toBe(false);
  });

  it("telefone presente é telefone verificado", () => {
    expect(verificationFromClaims({phone_number: "+5551999999999"}).phoneVerified).toBe(true);
    expect(verificationFromClaims({phone_number: ""}).phoneVerified).toBe(false);
    expect(verificationFromClaims({}).phoneVerified).toBe(false);
  });

  it("token ausente não verifica nada", () => {
    expect(verificationFromClaims(undefined)).toEqual({emailVerified: false, phoneVerified: false});
  });
});

describe("phoneNumberFromClaims", () => {
  it("devolve o telefone assinado", () => {
    expect(phoneNumberFromClaims({phone_number: "+5511912345678"})).toBe("+5511912345678");
  });

  it("sem telefone válido devolve null", () => {
    expect(phoneNumberFromClaims({phone_number: ""})).toBeNull();
    expect(phoneNumberFromClaims({phone_number: 5511912345678})).toBeNull();
    expect(phoneNumberFromClaims(undefined)).toBeNull();
  });
});

describe("parseEnforcementFlag", () => {
  it("só liga com o booleano true", () => {
    expect(parseEnforcementFlag({enforced: true})).toBe(true);
    expect(parseEnforcementFlag({enforced: "true"})).toBe(false);
    expect(parseEnforcementFlag({enforced: false})).toBe(false);
  });

  it("documento ausente é exigência desligada", () => {
    expect(parseEnforcementFlag(undefined)).toBe(false);
    expect(parseEnforcementFlag({})).toBe(false);
  });
});

describe("exigência completa", () => {
  it("exige e-mail e telefone", () => {
    expect(FULL_VERIFICATION).toEqual({email: true, phone: true});
  });

  it("aponta o e-mail primeiro quando falta tudo", () => {
    const nada = {emailVerified: false, phoneVerified: false};

    expect(meetsRequirement(nada, FULL_VERIFICATION)).toBe(false);
    expect(missingVerification(nada, FULL_VERIFICATION)).toBe("email");
  });

  it("com e-mail verificado, falta o telefone", () => {
    const sóEmail = {emailVerified: true, phoneVerified: false};

    expect(missingVerification(sóEmail, FULL_VERIFICATION)).toBe("phone");
  });

  it("conta completa passa", () => {
    const tudo = {emailVerified: true, phoneVerified: true};

    expect(meetsRequirement(tudo, FULL_VERIFICATION)).toBe(true);
    expect(missingVerification(tudo, FULL_VERIFICATION)).toBeNull();
  });
});

describe("callables exigidas e isentas", () => {
  const source = readFileSync(new URL("../../src/index.ts", import.meta.url), "utf8");

  function bodyOf(name: string): string {
    const start = source.indexOf(`export const ${name} = onCall(`);
    expect(start, `callable ${name} não encontrada em index.ts`).toBeGreaterThanOrEqual(0);
    const end = source.indexOf("\n);", start);
    expect(end, `fim da callable ${name} não encontrado`).toBeGreaterThan(start);
    return source.slice(start, end);
  }

  it("nenhuma callable está nas duas listas", () => {
    const exempt = new Set<string>(VERIFICATION_EXEMPT_CALLABLES);

    expect(VERIFIED_CALLABLES.filter((name) => exempt.has(name))).toEqual([]);
  });

  it("excluir, exportar, sair e sincronizar continuam isentas", () => {
    expect([...VERIFICATION_EXEMPT_CALLABLES].sort()).toEqual(
      ["deleteAccount", "exportUserData", "leaveMatch", "syncVerificationStatus"].sort(),
    );
  });

  it("toda callable exportada está em exatamente uma lista", () => {
    const exported = [...source.matchAll(/export const (\w+) = onCall\(/g)].map((match) => match[1]);
    const lists: readonly (readonly string[])[] = [VERIFIED_CALLABLES, VERIFICATION_EXEMPT_CALLABLES, ADMIN_CALLABLES];

    for (const name of exported) {
      const listsContaining = lists.filter((list) => list.includes(name)).length;
      expect(listsContaining, `callable ${name} precisa estar em exatamente uma lista`).toBe(1);
    }

    for (const name of lists.flat()) {
      expect(exported, `callable ${name} listada mas não exportada em index.ts`).toContain(name);
    }
  });

  it("toda callable exigida chama requireVerification", () => {
    for (const name of VERIFIED_CALLABLES) {
      const body = bodyOf(name);
      const auth = body.indexOf("requireAuthentication(uid);");
      const check = body.indexOf("await requireVerification(request.auth?.token)");
      const txn = body.indexOf("runTransaction(");

      expect(auth, `${name}: requireAuthentication ausente`).toBeGreaterThanOrEqual(0);
      expect(check, `${name}: requireVerification ausente ou antes da autenticação`).toBeGreaterThan(auth);
      expect(txn === -1 || check < txn, `${name}: requireVerification dentro da transação`).toBe(true);
    }
  });

  it("nenhuma callable isenta chama requireVerification", () => {
    for (const name of [...VERIFICATION_EXEMPT_CALLABLES, ...ADMIN_CALLABLES]) {
      expect(bodyOf(name), name).not.toContain("requireVerification(");
    }
  });
});
