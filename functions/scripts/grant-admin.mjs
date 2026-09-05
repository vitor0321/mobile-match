// Concede (ou remove) a custom claim `admin`, que é o que o painel de moderação
// e a callable adminSetModeration exigem.
//
// Não existe caminho pelo app de propósito: quem pode banir tem de ser decidido
// fora do produto.
//
//   GOOGLE_APPLICATION_CREDENTIALS=serviceAccount.json \
//     node scripts/grant-admin.mjs <email> [--revoke]
//
// A pessoa precisa sair e entrar de novo para o token carregar a claim.

import {initializeApp, applicationDefault} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";

const [email, flag] = process.argv.slice(2);
if (!email) {
  console.error("uso: node scripts/grant-admin.mjs <email> [--revoke]");
  process.exit(1);
}

const grant = flag !== "--revoke";
initializeApp({credential: applicationDefault()});

const auth = getAuth();
const user = await auth.getUserByEmail(email);

// Preserva as outras claims (role, plan) — sobrescrever apagaria as duas.
await auth.setCustomUserClaims(user.uid, {...(user.customClaims ?? {}), admin: grant});

console.log(`${grant ? "admin concedido a" : "admin removido de"} ${email} (${user.uid})`);
console.log("peça para a pessoa sair e entrar de novo.");
