// Verificação de e-mail e telefone.
//
// Quem verifica é o Firebase Auth, não este código: o app dispara
// `sendEmailVerification()` ou o fluxo de SMS, e o resultado vira claim
// assinada no ID token. O servidor espelha essa claim no perfil e, quando
// `config/verification.enforced` está ligado, exige as duas nas callables
// que agem sobre outras pessoas.

/** O que o perfil publica como sinal de confiança. */
export type VerificationStatus = {
  emailVerified: boolean;
  phoneVerified: boolean;
};

export type VerificationRequirement = {email: boolean; phone: boolean};

/** Com a exigência ligada, toda ação protegida pede as duas verificações. */
export const FULL_VERIFICATION: VerificationRequirement = {email: true, phone: true};

/** Callables que agem sobre outras pessoas e por isso exigem conta verificada. */
export const VERIFIED_CALLABLES = [
  "joinMatch",
  "cancelMatch",
  "cancelMatchSeries",
  "submitPlayerRating",
  "submitOrganizerRating",
  "submitSkillRating",
  "submitMatchRating",
  "submitReport",
  "setVipStatus",
  "confirmWaitlistedPlayer",
  "banPlayerFromMatch",
] as const;

/**
 * Isentas de propósito. Excluir e exportar são garantias da LGPD; travar a
 * saída prende uma vaga que outra pessoa usaria; e sincronizar é o próprio
 * caminho para ficar verificado.
 */
export const VERIFICATION_EXEMPT_CALLABLES = [
  "deleteAccount",
  "exportUserData",
  "leaveMatch",
  "syncVerificationStatus",
] as const;

/**
 * Ferramenta da equipe, não ação entre usuários: quem barra é a custom claim
 * de admin, e exigir verificação aqui não protegeria ninguém.
 */
export const ADMIN_CALLABLES = ["adminSetModeration"] as const;

/**
 * Lê o estado de verificação das claims do ID token.
 *
 * O token é a única fonte que vale: o cliente pode mandar qualquer coisa no
 * payload, mas não forja uma claim assinada. `phone_number` só existe depois
 * do SMS, por isso a presença dele basta como prova.
 */
export function verificationFromClaims(claims: Record<string, unknown> | undefined): VerificationStatus {
  return {
    emailVerified: claims?.email_verified === true,
    phoneVerified: phoneNumberFromClaims(claims) !== null,
  };
}

/** O telefone assinado, ou null. É o único valor confiável para gravar. */
export function phoneNumberFromClaims(claims: Record<string, unknown> | undefined): string | null {
  const phone = claims?.phone_number;
  return typeof phone === "string" && phone.length > 0 ? phone : null;
}

/** `config/verification`: só o booleano `true` liga. Documento ausente é desligado. */
export function parseEnforcementFlag(data: Record<string, unknown> | undefined): boolean {
  return data?.enforced === true;
}

/** A conta atende ao exigido? */
export function meetsRequirement(
  status: VerificationStatus,
  requirement: VerificationRequirement,
): boolean {
  if (requirement.email && !status.emailVerified) return false;
  if (requirement.phone && !status.phoneVerified) return false;
  return true;
}

/** O que falta, e-mail primeiro, para o app poder mapear sem depender de texto. */
export function missingVerification(
  status: VerificationStatus,
  requirement: VerificationRequirement,
): "email" | "phone" | null {
  if (requirement.email && !status.emailVerified) return "email";
  if (requirement.phone && !status.phoneVerified) return "phone";
  return null;
}
