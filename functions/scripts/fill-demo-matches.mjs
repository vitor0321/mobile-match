// Marca como lotadas (0 vagas) as partidas demo criadas por
// seed-demo-matches.mjs — sem isso a tela de Detalhes continuaria mostrando
// vagas disponíveis, porque ela lê ao vivo a subcoleção
// matches/{id}/participants em vez do confirmedCount do documento principal.
//
// Reduz totalSlots para 2 (era 10) e cria 2 documentos de participante
// fake por partida (userId sintético, sem conta real por trás) com
// isConfirmed: true, deixando confirmedCount == totalSlots em toda a tela.
//
//   GOOGLE_APPLICATION_CREDENTIALS=serviceAccount.json \
//     node scripts/fill-demo-matches.mjs <seedBatchId> [--execute]
//
// Sem --execute, só imprime quantos documentos seriam afetados (dry run).

import {initializeApp, applicationDefault} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";

const [seedBatchId, flag] = process.argv.slice(2);
if (!seedBatchId) {
  console.error("uso: node scripts/fill-demo-matches.mjs <seedBatchId> [--execute]");
  process.exit(1);
}
const execute = flag === "--execute";

const NEW_TOTAL_SLOTS = 2;
const FAKE_PLAYER_NAMES = ["Jogador 1", "Jogador 2"];

initializeApp({credential: applicationDefault()});
const db = getFirestore();

const snapshot = await db.collection("matches").where("seedBatchId", "==", seedBatchId).get();
console.log(`partidas encontradas: ${snapshot.size}`);
console.log(execute ? "\nMODO: gravando em produção\n" : "\nMODO: dry run (nada será gravado — use --execute para gravar)\n");

if (snapshot.empty) {
  console.log("nenhuma partida com esse seedBatchId. nada a fazer.");
  process.exit(0);
}

const now = Date.now();
const writes = [];
for (const doc of snapshot.docs) {
  writes.push({
    ref: doc.ref,
    data: {totalSlots: NEW_TOTAL_SLOTS, confirmedCount: NEW_TOTAL_SLOTS, participants: FAKE_PLAYER_NAMES.map((_, i) => `demo-player-${i + 1}`)},
    isMatchUpdate: true,
  });

  FAKE_PLAYER_NAMES.forEach((name, i) => {
    const participantId = `demo-player-${i + 1}`;
    writes.push({
      ref: doc.ref.collection("participants").doc(participantId),
      data: {
        userId: participantId,
        displayName: name,
        photoUrl: null,
        joinedAt: now,
        isConfirmed: true,
        positionInWaitlist: null,
        hasPaid: false,
      },
      isMatchUpdate: false,
    });
  });
}

console.log(`${snapshot.size} partidas × (1 atualização + ${FAKE_PLAYER_NAMES.length} participantes) = ${writes.length} operações`);
console.log("amostra (atualização de partida):", JSON.stringify(writes[0].data, null, 2));
console.log("amostra (participante fake):", JSON.stringify(writes[1].data, null, 2));

if (!execute) {
  console.log("\nRode de novo com --execute para gravar.");
  process.exit(0);
}

const BATCH_SIZE = 450;
for (let i = 0; i < writes.length; i += BATCH_SIZE) {
  const batch = db.batch();
  for (const write of writes.slice(i, i + BATCH_SIZE)) {
    if (write.isMatchUpdate) {
      batch.update(write.ref, write.data);
    } else {
      batch.set(write.ref, write.data);
    }
  }
  await batch.commit();
  console.log(`gravados ${Math.min(i + BATCH_SIZE, writes.length)}/${writes.length}`);
}

console.log("\npronto.");
