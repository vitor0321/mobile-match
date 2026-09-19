"use strict";
// Verificação de e-mail e telefone.
//
// Quem verifica é o Firebase Auth, não este código: o app dispara
// `sendEmailVerification()` ou o fluxo de SMS, e o resultado vira claim
// assinada no ID token. O servidor espelha essa claim no perfil e, quando
// `config/verification.enforced` está ligado, exige as duas nas callables
// que agem sobre outras pessoas.
Object.defineProperty(exports, "__esModule", { value: true });
exports.ADMIN_CALLABLES = exports.VERIFICATION_EXEMPT_CALLABLES = exports.VERIFIED_CALLABLES = exports.FULL_VERIFICATION = void 0;
exports.verificationFromClaims = verificationFromClaims;
exports.phoneNumberFromClaims = phoneNumberFromClaims;
exports.parseEnforcementFlag = parseEnforcementFlag;
exports.meetsRequirement = meetsRequirement;
exports.missingVerification = missingVerification;
/** Com a exigência ligada, toda ação protegida pede as duas verificações. */
exports.FULL_VERIFICATION = { email: true, phone: true };
/** Callables que agem sobre outras pessoas e por isso exigem conta verificada. */
exports.VERIFIED_CALLABLES = [
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
];
/**
 * Isentas de propósito. Excluir e exportar são garantias da LGPD; travar a
 * saída prende uma vaga que outra pessoa usaria; e sincronizar é o próprio
 * caminho para ficar verificado.
 */
exports.VERIFICATION_EXEMPT_CALLABLES = [
    "deleteAccount",
    "exportUserData",
    "leaveMatch",
    "syncVerificationStatus",
    "ensureUserProvisioned",
];
/**
 * Ferramenta da equipe, não ação entre usuários: quem barra é a custom claim
 * de admin, e exigir verificação aqui não protegeria ninguém.
 */
exports.ADMIN_CALLABLES = ["adminSetModeration"];
/**
 * Lê o estado de verificação das claims do ID token.
 *
 * O token é a única fonte que vale: o cliente pode mandar qualquer coisa no
 * payload, mas não forja uma claim assinada. `phone_number` só existe depois
 * do SMS, por isso a presença dele basta como prova.
 */
function verificationFromClaims(claims) {
    return {
        emailVerified: claims?.email_verified === true,
        phoneVerified: phoneNumberFromClaims(claims) !== null,
    };
}
/** O telefone assinado, ou null. É o único valor confiável para gravar. */
function phoneNumberFromClaims(claims) {
    const phone = claims?.phone_number;
    return typeof phone === "string" && phone.length > 0 ? phone : null;
}
/** `config/verification`: só o booleano `true` liga. Documento ausente é desligado. */
function parseEnforcementFlag(data) {
    return data?.enforced === true;
}
/** A conta atende ao exigido? */
function meetsRequirement(status, requirement) {
    if (requirement.email && !status.emailVerified)
        return false;
    if (requirement.phone && !status.phoneVerified)
        return false;
    return true;
}
/** O que falta, e-mail primeiro, para o app poder mapear sem depender de texto. */
function missingVerification(status, requirement) {
    if (requirement.email && !status.emailVerified)
        return "email";
    if (requirement.phone && !status.phoneVerified)
        return "phone";
    return null;
}
