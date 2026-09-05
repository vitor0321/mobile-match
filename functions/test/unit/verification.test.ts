import {describe, expect, it} from "vitest";
import {
  VERIFICATION_POLICY,
  isEnforcementEnabled,
  meetsRequirement,
  missingVerification,
  verificationFromClaims,
} from "../../src/verification.js";

describe("verificationFromClaims", () => {
  it("lê o e-mail verificado da claim", () => {
    expect(verificationFromClaims({email_verified: true}).emailVerified).toBe(true);
    expect(verificationFromClaims({email_verified: false}).emailVerified).toBe(false);
  });

  it("exige o booleano, não um valor parecido", () => {
    // O token é assinado, mas ler frouxo aqui aceitaria "false" como verdadeiro.
    expect(verificationFromClaims({email_verified: "true"}).emailVerified).toBe(false);
    expect(verificationFromClaims({email_verified: 1}).emailVerified).toBe(false);
  });

  it("telefone presente é telefone verificado", () => {
    // Não existe telefone não verificado no Firebase Auth: a claim só aparece
    // depois do SMS.
    expect(verificationFromClaims({phone_number: "+5551999999999"}).phoneVerified).toBe(true);
    expect(verificationFromClaims({phone_number: ""}).phoneVerified).toBe(false);
    expect(verificationFromClaims({}).phoneVerified).toBe(false);
  });

  it("token ausente não verifica nada", () => {
    expect(verificationFromClaims(undefined)).toEqual({
      emailVerified: false,
      phoneVerified: false,
    });
  });
});

describe("política", () => {
  it("nasce toda desligada", () => {
    // Ligar tranca de uma hora para outra quem já usa o app e nunca verificou.
    // A capacidade existe; exigir é decisão de produto, com aviso antes.
    expect(isEnforcementEnabled(VERIFICATION_POLICY.joinMatch)).toBe(false);
    expect(isEnforcementEnabled(VERIFICATION_POLICY.createMatch)).toBe(false);
  });

  it("sem exigência, qualquer conta passa", () => {
    const nada = {emailVerified: false, phoneVerified: false};

    expect(meetsRequirement(nada, {email: false, phone: false})).toBe(true);
    expect(missingVerification(nada, {email: false, phone: false})).toBeNull();
  });

  it("exigência de e-mail barra quem não verificou", () => {
    const sóTelefone = {emailVerified: false, phoneVerified: true};

    expect(meetsRequirement(sóTelefone, {email: true, phone: false})).toBe(false);
    expect(missingVerification(sóTelefone, {email: true, phone: false})).toBe("email");
  });

  it("exigência de telefone barra quem não verificou", () => {
    const sóEmail = {emailVerified: true, phoneVerified: false};

    expect(meetsRequirement(sóEmail, {email: false, phone: true})).toBe(false);
    expect(missingVerification(sóEmail, {email: false, phone: true})).toBe("phone");
  });

  it("com as duas exigidas, aponta o e-mail primeiro", () => {
    const nada = {emailVerified: false, phoneVerified: false};

    // Uma pendência de cada vez: verificar e-mail é mais barato que SMS.
    expect(missingVerification(nada, {email: true, phone: true})).toBe("email");
  });

  it("conta completa passa em qualquer exigência", () => {
    const tudo = {emailVerified: true, phoneVerified: true};

    expect(meetsRequirement(tudo, {email: true, phone: true})).toBe(true);
    expect(missingVerification(tudo, {email: true, phone: true})).toBeNull();
  });
});
