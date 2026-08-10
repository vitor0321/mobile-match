import {initializeApp} from "firebase-admin/app";
import {getFirestore, Timestamp} from "firebase-admin/firestore";
import {defineSecret} from "firebase-functions/params";
import {HttpsError, onCall} from "firebase-functions/v2/https";

initializeApp();

const anthropicApiKey = defineSecret("ANTHROPIC_API_KEY");
const db = getFirestore();
const REGION = "southamerica-east1";
const MAX_REQUESTS_PER_HOUR = 20;
const MAX_TEXT_LENGTH = 4_000;
const RECENT_AUTH_WINDOW_MILLIS = 5 * 60 * 1_000;
const ANTHROPIC_VERSION = "2023-06-01";
const ANTHROPIC_MODEL = "claude-haiku-4-5-20251001";

type VerseExplanationRequest = {
  verseText: string;
  bookName: string;
  chapterNumber: number;
  verseNumber: number;
  translation: string;
};

type BookExplanationRequest = {
  bookId: number;
  bookName: string;
};

type TranslationRequest = {
  englishDefinition: string;
};

type VerseExplanationResponse = VerseExplanationRequest & {
  verseRef: string;
  explanation: string;
};

type BookExplanationResponse = BookExplanationRequest & {
  explanation: string;
};

export const getVerseExplanation = onCall(
  {region: REGION, secrets: [anthropicApiKey]},
  async (request): Promise<VerseExplanationResponse> => {
    requireAuthentication(request.auth?.uid);
    const input = parseVerseExplanationRequest(request.data);
    await enforceRateLimit(request.auth!.uid);

    const verseRef = `${input.bookName} ${input.chapterNumber}:${input.verseNumber}-${input.translation}`;
    const reference = db.collection("verse_explanations").doc(verseRef);
    const cached = await reference.get();
    if (cached.exists) return cached.data() as VerseExplanationResponse;

    const explanation = await generateText(buildVersePrompt(input));
    const response: VerseExplanationResponse = {...input, verseRef, explanation};
    await reference.set({...response, generatedAt: Timestamp.now()});
    return response;
  },
);

export const getBookExplanation = onCall(
  {region: REGION, secrets: [anthropicApiKey]},
  async (request): Promise<BookExplanationResponse> => {
    requireAuthentication(request.auth?.uid);
    const input = parseBookExplanationRequest(request.data);
    await enforceRateLimit(request.auth!.uid);

    const reference = db.collection("book_explanations").doc(String(input.bookId));
    const cached = await reference.get();
    if (cached.exists) return cached.data() as BookExplanationResponse;

    const explanation = await generateText(buildBookPrompt(input.bookName));
    const response: BookExplanationResponse = {...input, explanation};
    await reference.set({...response, generatedAt: Timestamp.now()});
    return response;
  },
);

export const translateStrongsDefinition = onCall(
  {region: REGION, secrets: [anthropicApiKey]},
  async (request): Promise<{translation: string}> => {
    requireAuthentication(request.auth?.uid);
    const input = parseTranslationRequest(request.data);
    await enforceRateLimit(request.auth!.uid);
    return {translation: await generateText(buildTranslationPrompt(input.englishDefinition), 512)};
  },
);

export const deleteAccount = onCall(
  {region: REGION},
  async (request): Promise<{deleted: true}> => {
    const uid = request.auth?.uid;
    requireAuthentication(uid);
    requireRecentAuthentication(request.auth?.token.auth_time);
    requireEmptyPayload(request.data);

    await Promise.all([
      db.recursiveDelete(db.collection("users").doc(uid)),
      db.collection("ai_rate_limits").doc(uid).delete(),
    ]);

    return {deleted: true};
  },
);

function requireAuthentication(uid: string | undefined): asserts uid is string {
  if (!uid) throw new HttpsError("unauthenticated", "Authentication is required.");
}

function requireRecentAuthentication(authTime: unknown): void {
  if (typeof authTime !== "number" || Date.now() - authTime * 1_000 > RECENT_AUTH_WINDOW_MILLIS) {
    throw new HttpsError("failed-precondition", "Recent authentication is required.");
  }
}

async function enforceRateLimit(uid: string): Promise<void> {
  const reference = db.collection("ai_rate_limits").doc(uid);
  await db.runTransaction(async (transaction) => {
    const now = Timestamp.now();
    const snapshot = await transaction.get(reference);
    const data = snapshot.data();
    const windowStartedAt = data?.windowStartedAt as Timestamp | undefined;
    const isCurrentWindow = windowStartedAt != null && now.toMillis() - windowStartedAt.toMillis() < 3_600_000;
    const count = isCurrentWindow ? (data?.count as number ?? 0) : 0;
    if (count >= MAX_REQUESTS_PER_HOUR) {
      throw new HttpsError("resource-exhausted", "AI request limit reached. Try again later.");
    }
    transaction.set(reference, {
      count: count + 1,
      windowStartedAt: isCurrentWindow ? windowStartedAt : now,
      updatedAt: now,
    });
  });
}

async function generateText(prompt: string, maxTokens = 1024): Promise<string> {
  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": anthropicApiKey.value(),
      "anthropic-version": ANTHROPIC_VERSION,
    },
    body: JSON.stringify({
      model: ANTHROPIC_MODEL,
      max_tokens: maxTokens,
      messages: [{role: "user", content: prompt}],
    }),
  });
  if (!response.ok) throw new HttpsError("internal", "Unable to generate AI content.");

  const payload = await response.json() as {content?: Array<{type?: string; text?: string}>};
  const text = payload.content?.find((item) => item.type === "text")?.text?.trim();
  if (!text) throw new HttpsError("internal", "AI returned an empty response.");
  return text;
}

export function parseVerseExplanationRequest(value: unknown): VerseExplanationRequest {
  const input = requireRecord(value);
  return {
    verseText: requireText(input.verseText, "verseText"),
    bookName: requireText(input.bookName, "bookName"),
    chapterNumber: requirePositiveInteger(input.chapterNumber, "chapterNumber"),
    verseNumber: requirePositiveInteger(input.verseNumber, "verseNumber"),
    translation: requireText(input.translation, "translation"),
  };
}

export function parseBookExplanationRequest(value: unknown): BookExplanationRequest {
  const input = requireRecord(value);
  return {
    bookId: requirePositiveInteger(input.bookId, "bookId"),
    bookName: requireText(input.bookName, "bookName"),
  };
}

export function parseTranslationRequest(value: unknown): TranslationRequest {
  const input = requireRecord(value);
  return {englishDefinition: requireText(input.englishDefinition, "englishDefinition")};
}

export function requireEmptyPayload(value: unknown): void {
  const input = requireRecord(value);
  if (Object.keys(input).length > 0) {
    throw new HttpsError("invalid-argument", "Payload must be empty.");
  }
}

function requireRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== "object" || value == null || Array.isArray(value)) {
    throw new HttpsError("invalid-argument", "Payload must be an object.");
  }
  return value as Record<string, unknown>;
}

function requireText(value: unknown, field: string): string {
  if (typeof value !== "string" || value.trim().length === 0 || value.length > MAX_TEXT_LENGTH) {
    throw new HttpsError("invalid-argument", `${field} is invalid.`);
  }
  return value.trim();
}

function requirePositiveInteger(value: unknown, field: string): number {
  if (typeof value !== "number" || !Number.isInteger(value) || value <= 0) {
    throw new HttpsError("invalid-argument", `${field} is invalid.`);
  }
  return value;
}

function buildBookPrompt(bookName: string): string {
  return `Você é um teólogo especialista da Igreja Reformada Protestante, alinhado com a Igreja Presbiteriana do Brasil (IPB).

Forneça uma introdução ao livro bíblico abaixo, com rigor histórico-crítico dentro da tradição reformada.

Livro: ${bookName}

Estruture a resposta nas seguintes seções:

1. **Autoria**: Quem escreveu o livro, debates sobre autoria (se houver) e a posição predominante na tradição reformada.

2. **Período e Datação**: Quando o livro foi escrito, contexto histórico-político da época da composição.

3. **Contexto e Propósito**: Para quem foi escrito, qual problema ou necessidade motivou a obra, e como o livro se encaixa na história da redenção (revelação progressiva).

Responda em Português do Brasil. Use Markdown com **negritos** para termos-chave e nomes próprios. Sem introduções, saudações ou títulos além das seções acima. Alta profundidade teológica, linguagem extremamente direta, densa e concisa. Evite parágrafos longos e limite cada seção a no máximo um parágrafo objetivo.`;
}

function buildVersePrompt(input: VerseExplanationRequest): string {
  return `Você é um teólogo especialista da Igreja Reformada Protestante, alinhado com a Igreja Presbiteriana do Brasil (IPB).

Realize uma explicação exegética do versículo abaixo, mantendo-se estritamente dentro do contexto canônico do próprio livro e do corpus bíblico reformado.

Versículo: ${input.verseText}
Livro: ${input.bookName} | Capítulo: ${input.chapterNumber} | Versículo: ${input.verseNumber} | Tradução: ${input.translation}

Estruture a resposta nas seguintes seções:

1. **Contexto Literário e Histórico**
2. **Exegese do Texto**
3. **Referências Cruzadas**

Responda em Português do Brasil. Use Markdown com **negritos** para termos-chave, referências cruzadas e termos no idioma original. Sem introduções, saudações ou títulos além das seções acima. Alta profundidade teológica, linguagem extremamente direta, densa e concisa. Evite parágrafos longos e limite cada seção a no máximo um parágrafo objetivo.`;
}

function buildTranslationPrompt(definition: string): string {
  return `Traduza a definição abaixo do dicionário Strong's bíblico para o português brasileiro. Responda apenas com a tradução, sem introdução, sem explicação, sem aspas.\n\n${definition}`;
}
