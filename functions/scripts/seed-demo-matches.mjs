// Cria partidas de demonstração (Futebol, recorrência semanal) em 22 cidades
// brasileiras, com uma conta real como organizadora, sem ocupar vaga
// (confirmedCount: 0, participants: []) — mesmo formato que o app grava em
// FirestoreGameSource.createMatch(). Cada cidade vira uma série de 24
// ocorrências semanais (seriesId compartilhado), igual ao que
// generateRecurringMatches geraria com o tempo, só que tudo de uma vez.
//
// Cada documento carrega um `seedBatchId` (não lido pelo app — GameMapper.kt
// só extrai os campos conhecidos) só para permitir localizar/apagar esse lote
// depois via Admin SDK, sem aparecer em lugar nenhum da UI.
//
//   GOOGLE_APPLICATION_CREDENTIALS=serviceAccount.json \
//     node scripts/seed-demo-matches.mjs <email-do-organizador> [--execute]
//
// Sem --execute, só imprime o resumo do que seria gravado (dry run).

import {initializeApp, applicationDefault} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {getFirestore} from "firebase-admin/firestore";

const [email, flag] = process.argv.slice(2);
if (!email) {
  console.error("uso: node scripts/seed-demo-matches.mjs <email-do-organizador> [--execute]");
  process.exit(1);
}
const execute = flag === "--execute";

const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
const BITS = [16, 8, 4, 2, 1];
const GEOHASH_PRECISION = 9;

function encodeGeoHash(lat, lng, precision = GEOHASH_PRECISION) {
  let hash = "";
  let latMin = -90;
  let latMax = 90;
  let lngMin = -180;
  let lngMax = 180;
  let even = true;
  let bit = 0;
  let ch = 0;
  while (hash.length < precision) {
    if (even) {
      const mid = (lngMin + lngMax) / 2;
      if (lng >= mid) {
        ch |= BITS[bit];
        lngMin = mid;
      } else {
        lngMax = mid;
      }
    } else {
      const mid = (latMin + latMax) / 2;
      if (lat >= mid) {
        ch |= BITS[bit];
        latMin = mid;
      } else {
        latMax = mid;
      }
    }
    even = !even;
    if (bit < 4) {
      bit++;
    } else {
      hash += BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }
  return hash;
}

const CITIES = [
  {city: "Manaus", state: "AM", neighborhood: "Adrianópolis", lat: -3.1190, lng: -60.0217, weekday: 1, hour: 20},
  {city: "Belém", state: "PA", neighborhood: "Umarizal", lat: -1.4558, lng: -48.4902, weekday: 2, hour: 19},
  {city: "São Luís", state: "MA", neighborhood: "Renascença", lat: -2.5307, lng: -44.3068, weekday: 3, hour: 19},
  {city: "Fortaleza", state: "CE", neighborhood: "Aldeota", lat: -3.7172, lng: -38.5433, weekday: 4, hour: 20},
  {city: "Natal", state: "RN", neighborhood: "Petrópolis", lat: -5.7945, lng: -35.2110, weekday: 5, hour: 19},
  {city: "João Pessoa", state: "PB", neighborhood: "Tambaú", lat: -7.1195, lng: -34.8450, weekday: 6, hour: 18},
  {city: "Recife", state: "PE", neighborhood: "Boa Viagem", lat: -8.0476, lng: -34.8770, weekday: 0, hour: 18},
  {city: "Maceió", state: "AL", neighborhood: "Ponta Verde", lat: -9.6498, lng: -35.7089, weekday: 1, hour: 19},
  {city: "Salvador", state: "BA", neighborhood: "Barra", lat: -12.9714, lng: -38.5014, weekday: 2, hour: 20},
  {city: "Brasília", state: "DF", neighborhood: "Asa Sul", lat: -15.7939, lng: -47.8828, weekday: 3, hour: 20},
  {city: "Cuiabá", state: "MT", neighborhood: "Centro Sul", lat: -15.6014, lng: -56.0979, weekday: 4, hour: 19},
  {city: "Goiânia", state: "GO", neighborhood: "Setor Bueno", lat: -16.6869, lng: -49.2648, weekday: 5, hour: 20},
  {city: "Campo Grande", state: "MS", neighborhood: "Jardim dos Estados", lat: -20.4697, lng: -54.6201, weekday: 6, hour: 19},
  {city: "Vitória", state: "ES", neighborhood: "Praia do Canto", lat: -20.3155, lng: -40.3128, weekday: 0, hour: 19},
  {city: "Belo Horizonte", state: "MG", neighborhood: "Savassi", lat: -19.9167, lng: -43.9345, weekday: 1, hour: 20},
  {city: "Rio de Janeiro", state: "RJ", neighborhood: "Tijuca", lat: -22.9068, lng: -43.1729, weekday: 2, hour: 19},
  {city: "São Paulo", state: "SP", neighborhood: "Vila Madalena", lat: -23.5505, lng: -46.6333, weekday: 3, hour: 20},
  {city: "Campinas", state: "SP", neighborhood: "Cambuí", lat: -22.9099, lng: -47.0626, weekday: 4, hour: 19},
  {city: "Curitiba", state: "PR", neighborhood: "Batel", lat: -25.4284, lng: -49.2733, weekday: 5, hour: 19},
  {city: "Londrina", state: "PR", neighborhood: "Gleba Palhano", lat: -23.3103, lng: -51.1628, weekday: 6, hour: 20},
  {city: "Florianópolis", state: "SC", neighborhood: "Trindade", lat: -27.5954, lng: -48.5480, weekday: 0, hour: 20},
  {city: "Porto Alegre", state: "RS", neighborhood: "Moinhos de Vento", lat: -30.0346, lng: -51.2177, weekday: 1, hour: 19},
];

const OCCURRENCES_PER_CITY = 24;
const TOTAL_SLOTS = 10;
const PRICE_CENTS = 2000;
const DURATION_MIN = 60;
const SEED_BATCH_ID = `demo-futebol-${new Date().toISOString().slice(0, 10)}`;

function firstOccurrence(weekday, hour) {
  const now = new Date();
  const date = new Date(now);
  date.setUTCHours(hour, 0, 0, 0);
  let daysUntil = (weekday - date.getUTCDay() + 7) % 7;
  if (daysUntil === 0 && date.getTime() <= now.getTime()) daysUntil = 7;
  date.setUTCDate(date.getUTCDate() + daysUntil);
  return date;
}

initializeApp({credential: applicationDefault()});
const auth = getAuth();
const db = getFirestore();

const organizer = await auth.getUserByEmail(email);
const profileSnap = await db.doc(`profiles/${organizer.uid}`).get();
const profile = profileSnap.exists ? profileSnap.data() : {};
const organizerName = organizer.displayName ?? email.split("@")[0];
const organizerRating = typeof profile.rating === "number" ? profile.rating : 0;
const organizerRatingCount = typeof profile.ratingCount === "number" ? profile.ratingCount : 0;

console.log(`organizador: ${organizerName} (${organizer.uid})`);
console.log(`cidades: ${CITIES.length} · ocorrências por cidade: ${OCCURRENCES_PER_CITY}`);
console.log(`total de partidas: ${CITIES.length * OCCURRENCES_PER_CITY}`);
console.log(`seedBatchId: ${SEED_BATCH_ID}`);
console.log(execute ? "\nMODO: gravando em produção\n" : "\nMODO: dry run (nada será gravado — use --execute para gravar)\n");

const docs = [];
for (const place of CITIES) {
  const geohash = encodeGeoHash(place.lat, place.lng);
  const seriesRef = db.collection("matches").doc();
  let startsAt = firstOccurrence(place.weekday, place.hour);

  for (let i = 0; i < OCCURRENCES_PER_CITY; i++) {
    const ref = i === 0 ? seriesRef : db.collection("matches").doc();
    const startsAtSeconds = Math.floor(startsAt.getTime() / 1000);

    docs.push({
      ref,
      data: {
        sport: "FUTEBOL",
        venueName: `Arena Society ${place.city}`,
        neighborhood: place.neighborhood,
        city: `${place.city} - ${place.state}`,
        address: `Quadra Society Municipal, ${place.neighborhood}`,
        lat: place.lat,
        lng: place.lng,
        geohash,
        startsAtSeconds,
        durationMin: DURATION_MIN,
        recurrence: "WEEKLY",
        seriesId: seriesRef.id,
        confirmedCount: 0,
        totalSlots: TOTAL_SLOTS,
        priceCents: PRICE_CENTS,
        status: "OPEN",
        organizerName,
        organizerId: organizer.uid,
        organizerRating,
        organizerRatingCount,
        matchRating: 0,
        matchRatingCount: 0,
        currencyCode: "BRL",
        participants: [],
        seedBatchId: SEED_BATCH_ID,
      },
    });

    startsAt = new Date(startsAt.getTime() + 7 * 24 * 60 * 60 * 1000);
  }
}

console.log(`amostra (${docs[0].data.venueName}):`);
console.log(JSON.stringify({...docs[0].data, startsAtDate: new Date(docs[0].data.startsAtSeconds * 1000).toISOString()}, null, 2));

if (!execute) {
  console.log(`\n${docs.length} documentos seriam criados. Rode de novo com --execute para gravar.`);
  process.exit(0);
}

const BATCH_SIZE = 400;
for (let i = 0; i < docs.length; i += BATCH_SIZE) {
  const batch = db.batch();
  for (const doc of docs.slice(i, i + BATCH_SIZE)) {
    batch.set(doc.ref, doc.data);
  }
  await batch.commit();
  console.log(`gravados ${Math.min(i + BATCH_SIZE, docs.length)}/${docs.length}`);
}

console.log("\npronto.");
