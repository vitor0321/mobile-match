// Verificação de e-mail e telefone (Phase 6).
//
// Quem verifica é o Firebase Auth, não este código: o app dispara
// `sendEmailVerification()` ou o fluxo de SMS, e o resultado vira claim no ID
// token, assinado. O papel do servidor é só espelhar essa claim no perfil, para
// que outras pessoas vejam o selo — e, se um dia for ligado, exigir.

/** O que o perfil publica como sinal de confiança. */
export type VerificationStatus = {
  emailVerified: boolean;
  phoneVerified: boolean;
};

/**
 * Lê o estado de verificação das claims do ID token.
 *
 * O token é a única fonte que vale: o cliente pode mandar qualquer coisa no
 * payload, mas não forja uma claim assinada pelo Firebase.
 *
 * `phone_number` só aparece quando existe credencial de telefone na conta, e é
 * por isso que a presença dele basta como prova — não há telefone não
 * verificado no Firebase Auth.
 */
export function verificationFromClaims(claims: Record<string, unknown> | undefined): VerificationStatus {
  return {
    emailVerified: claims?.email_verified === true,
    phoneVerified: typeof claims?.phone_number === "string" && claims.phone_number.length > 0,
  };
}

/**
 * O que cada ação exige.
 *
 * Tudo desligado de propósito. Ligar qualquer um destes tranca, de uma hora
 * para outra, todo mundo que já usa o app e nunca verificou nada — a capacidade
 * existe, a decisão de exigir é de produto e precisa de aviso antes.
 *
 * Ligar é trocar `false` por `true` e reimplantar.
 */
export const VERIFICATION_POLICY = {
  createMatch: {email: false, phone: false},
  joinMatch: {email: false, phone: false},
} as const;

export type VerificationRequirement = {email: boolean; phone: boolean};

/** A conta atende ao exigido para a ação? */
export function meetsRequirement(
  status: VerificationStatus,
  requirement: VerificationRequirement,
): boolean {
  if (requirement.email && !status.emailVerified) return false;
  if (requirement.phone && !status.phoneVerified) return false;
  return true;
}

/** Alguma ação exige alguma coisa? Serve para pular leitura desnecessária. */
export function isEnforcementEnabled(requirement: VerificationRequirement): boolean {
  return requirement.email || requirement.phone;
}

/** Mensagem única, para o app poder mapear sem depender de texto. */
export function missingVerification(
  status: VerificationStatus,
  requirement: VerificationRequirement,
): "email" | "phone" | null {
  if (requirement.email && !status.emailVerified) return "email";
  if (requirement.phone && !status.phoneVerified) return "phone";
  return null;
}
