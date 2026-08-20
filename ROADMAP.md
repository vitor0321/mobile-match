# Mobile Match — Roadmap Consolidado

**Último atualizado:** 2026-08-18 · **Status:** Phase 6 em andamento — denúncia, moderação, restrição, notificações, export LGPD e painel de moderação prontos

---

## Índice

1. [Visão Geral do Projeto](#visão-geral-do-projeto)
2. [Status Atual](#status-atual)
3. [O que foi feito (Phase 1-4)](#o-que-foi-feito-phase-1-4)
4. [O que falta (Phase 5+)](#o-que-falta-phase-5)
5. [Arquitetura](#arquitetura)
6. [Modelo de Dados](#modelo-de-dados)
7. [Cronograma Detalhado](#cronograma-detalhado)
8. [Riscos e Mitigações](#riscos-e-mitigações)

---

## Visão Geral do Projeto

**Mobile Match** é um marketplace que conecta **vagas em partidas** a **jogadores disponíveis**. A partida já existe e a quadra já está reservada; o app resolve o espaço vazio.

> **TEM UMA VAGA. TEM ALGUÉM QUERENDO JOGAR. A GENTE FAZ O MATCH.**

### Stack

| Aspecto | Tecnologia |
|---|---|
| **Plataformas** | Android + iOS (KMP) |
| **UI** | Compose Multiplatform |
| **Backend** | Firebase (Firestore + Functions + FCM) |
| **Autenticação** | Firebase Auth |
| **Navegação** | Voyager |
| **DI** | Koin |
| **Cache Local** | Room KMP + DataStore |
| **Monetização** | RevenueCat (assinatura) + Pix P2P (partidas) |

### Módulos

```
app/                        Entry point, Koin, shell de navegação
core/                       DI, analytics, crash, dispatchers, retry
  └ geo/                    Geohash, haversine, formatação de distância
  └ payments/               PixPayloadBuilder (TLV + CRC16)
  └ datetime/               Formatadores ("Hoje · 20:00")
  └ location/               expect/actual para permissão + GPS
firestore/                  Encapsula o SDK Firestore (cocoapods + framework iOS)
navigator/                  Contratos entre produtos
cedarDS/                    Design system (MatchCard, SportChip, BottomBar, etc)
products/identity/          ✓ Autenticação, perfil, paywall, RevenueCat
products/games/             ✓ Partidas: home, busca, criar, detalhe, mymatches
products/player/            ⊕ Perfil do jogador, disponibilidade, avaliações
products/notifications/     ⊕ Feed, FCM, WhatsApp broadcast
functions/                  Cloud Functions (triggers, callables)
```

---

## Status Atual

### Phase 1: GameList + Search ✅ COMPLETA

**Objetivo:** O jogador vê e busca jogos perto dele.

**Entregáveis:**
- ✅ HomeStep (lista de partidas por proximidade)
- ✅ SearchStep (busca textual + filtros de esporte, raio, preço)
- ✅ MatchCard (card de partida com informações completas)
- ✅ Integração com cache local (Room + DataStore)
- ✅ Filtros de esporte, raio (5-50 km), só-com-vaga
- ✅ Localização e cálculo de distância (haversine)
- ✅ Strings localizadas (pt-BR/en-US)

**Commit:** `0d1ebbe` — fase 1 complete

---

### Phase 2: Create Match, MyMatches, Profile ✅ COMPLETA

**Objetivo:** Jogador cria partidas, gerencia participações e edita perfil.

**Entregáveis:**
- ✅ CreateMatchStep (formulário com validação)
- ✅ MyMatchesStep (abas: Ativas/Passadas)
- ✅ ProfileStep (perfil do usuário logado)
- ✅ DateTimePicker, SportSelector
- ✅ Validação de campos (venues, datas no futuro, 2-20 players)
- ✅ Integração com Firestore

**Commit:** `b359a24` — phase 2 complete

---

### Phase 3: Real-time Status Updates ✅ COMPLETA

**Objetivo:** Participações em tempo real, join/leave com validação.

**Entregáveis:**
- ✅ JoinMatch flow (confirmed vs waitlist)
- ✅ LeaveMatch com notificações
- ✅ Realtime snapshots de participantes
- ✅ Transações para evitar overbooking
- ✅ Promoção de waitlist ao deixar partida
- ✅ Status badges (OPEN, FULL, CANCELLED, FINISHED)

**Commit:** `a15e030` — phase 3 complete

---

### Phase 4: Leave/Cancel Match, UI Dialogs ✅ COMPLETA

**Objetivo:** Fluxos completos de saída e cancelamento com confirmação.

**Entregáveis:**
- ✅ LeaveMatch dialog com confirmação
- ✅ CancelMatch dialog (só organizador)
- ✅ Notificações de mudança de status
- ✅ Atualização de contadores em tempo real
- ✅ Tratamento de erros com snackbars
- ✅ Rating display (estrelas + contagem)

**Commit:** `5488551` — phase 4 complete

---

## O que Falta (Phase 5+)

### Phase 5: Player Search & Filters ✅ COMPLETA (15-20h)

**Objetivo:** Jogadores encontram e filtram outros jogadores. Veem ratings e histórico.

**Escopo:**
- 🔍 **Player Search:** Buscar jogadores por nome, esporte, localização
- ⭐ **Filtros:** Rating mínimo (0-5 ⭐), esportes favoritos, mín. partidas
- 📊 **Player Details:** Perfil expandido com ratings, histórico, estatísticas
- 📝 **Ratings:** Exibir avaliações de outras pessoas, média, distribuição
- 🎯 **Leaderboard:** Top players (opcional, bonus)

**Sprints:**
1. **Player Search + Filters** (6-8h) ✅
   - `SearchPlayersUseCase`, `GetPlayerDetailsUseCase`
   - `PlayerSearchStep` + `PlayerFiltersPanel`
   - Queries otimizadas por raio, esporte, rating

2. **Player Details & Ratings** (6-8h) ✅
   - `PlayerDetailsStep` com header, stats, histograma e preview de avaliações
   - `PlayerRatingsListStep` (paginado, 20 por página, ordenação servidor-side)
   - `RatingStars` e `RatingSummary` no cedarDS
   - `GetPlayerRatingsUseCase` + cursor opaco (`RatingCursor`)
   - Strings pt-BR/en-US e testes de StepModel

3. **Polish & Integration** (2-4h) ✅
   - Mapeamento de campos corrigido: `fullName`/`avatarUrl`/`createdAt`
   - Cache em memória com TTL de 5 min (busca + perfil), invalidado ao avaliar
   - Debounce de 300ms e cancelamento da busca anterior
   - Aviso de "refine a busca" quando o teto de leitura é atingido
   - Jogadores banidos fora da busca (`where isBanned == false`)
   - Caching em memória (TTL 5 min)
   - Pagination nos resultados de busca
   - Pre-loading de jogadores próximos
   - Testes E2E

---

### Phase 6: Confiança e Segurança 🚧 EM ANDAMENTO (10-15h)

**Objetivo:** O produto sobrevive ao primeiro usuário mal-intencionado.

**Escopo:**
- 🚨 **Denúncias** com 10 motivos ✅
- 🛡️ **Moderação** (advertência → suspensão → revisão humana) ✅
- 🔒 **Restrição efetiva** — joinMatch, submitPlayerRating, submitReport e a
  regra de criar partida recusam conta banida ou suspensa ✅
- 🔔 **Notificações** — `onMatchCreated` e `onParticipantChanged` ✅
- 🖥️ **Painel de moderação** — fila de revisão e decisão humana ✅
- ⏱️ **Partida encerrada pelo relógio** (D34) — servidor ✅, UI ⏳
- 📋 **Avaliações pós-partida** multidimensionais (pontualidade, respeito,
  fair play, comportamento) — servidor ✅ (obrigatórias), UI ⏳
- ✉️ **Verificação** de e-mail e telefone — servidor ✅ (`syncVerificationStatus`,
  selo espelhado no perfil, política de exigência **desligada**), UI ⏳
- 🔐 **LGPD** — exportação e exclusão estendida ✅
- ❌ **Contadores de experiência** — fora do produto (D26)
- ❌ **Assinatura / limite de plano** — fora do MVP (D27)

**O que existia antes:** só a fachada. As coleções `reports` e `moderation`
tinham regras, comentários e testes, mas ninguém escrevia nelas. `isBanned` era
gravado uma única vez, como `false`, no cadastro. E o ROADMAP afirmava que
`joinMatch` validava ban — não validava; o único lugar que consultava banimento
era a regra de criar partida.

**Anti-abuso, por construção:**
- Só dá para denunciar quem jogou a mesma partida que você
- O id `{matchId}_{reporter}_{reported}` limita a uma denúncia por par por partida
- O escalonamento conta **denunciantes distintos**, não denúncias
- Janela de 180 dias: sem ela a punição seria permanente na prática
- Banimento **não** é automático (ver D22)

---

### Phase 7: Mapa e Polimento ⏳ PENDENTE (8-12h)

**Objetivo:** Experiência visual completa e identidade do produto.

**Escopo:**
- 🗺️ **Mapa interativo** (Google Maps / MapKit via `expect`/`actual`)
- 🎨 **Tema Cedar** (identidade esportiva, remover Lexis/Bible leftovers)
- 🏆 **Ícones** Android/iOS personalizados
- 📊 **Analytics** de funil (view → join → confirm → play)
- 📱 **Painel Admin** — moderação ✅ (`admin/index.html`, Firebase Hosting); demais áreas ⏳

---

## Arquitetura

### Camadas (Data → Domain → UI)

```kotlin
// Exemplo: MatchRepository

domain/model/
  ├── Match
  ├── Sport
  ├── MatchStatus
  ├── MatchFilter
  └── Participant

domain/error/
  └── GamesError (Network, NotFound, PermissionDenied, Unknown)

domain/repository/
  └── MatchRepository interface

domain/usecase/
  └── GetNearbyMatchesUseCase
  └── SearchMatchesUseCase
  └── ObserveMatchUseCase

data/remote/
  └── MatchSource interface + expect/actual FirestoreMatchSource

data/local/
  ├── GamesDatabase (Room KMP)
  ├── MatchEntity
  ├── MatchDao
  └── GamesPreferences (DataStore)

data/mapper/
  ├── MatchDto ↔ MatchEntity
  └── MatchEntity ↔ Match

data/repository/
  └── MatchRepositoryImpl (offline-first com withRetry)

ui/home/
  ├── HomeStep (Screen do Voyager)
  ├── HomeStepModel (StateFlow + Channel)
  ├── HomeState
  └── HomeEvents
```

### Padrão Voyager + MVI

```kotlin
// Step implementa Screen do Voyager
internal class HomeStep : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<HomeStepModel>()
        val state by model.state.collectAsState()
        
        // UI reage a estado e efeitos
    }
}

// StepModel gerencia estado e efeitos
internal class HomeStepModel : ScreenModel {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()
    
    fun onEvent(event: HomeEvents) { /* ... */ }
}

// State é imutável com campos val
internal data class HomeState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: ImmutableList<Game> = persistentListOf(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

// Events + Effects para comunicação
internal sealed interface HomeEvents
internal sealed interface HomeEffect
```

### Firestore Security Rules

```javascript
// Princípios:
// - Escrita em participants, payments, subscription: negada ao cliente
// - Só Cloud Functions escrevem dados sensíveis
// - profiles/{uid}/private/*: request.auth.uid == uid
// - Admin via custom claim

match /{document=**} {
  allow read, write: if request.auth != null;
}

match /participants/{document=**} {
  allow read: if request.auth != null;
  allow write: if false; // Só Functions
}

match /profiles/{uid}/private/{document=**} {
  allow read, write: if request.auth.uid == uid;
}
```

### Cloud Functions (TypeScript)

```typescript
// Regra: Decisões críticas no servidor, nunca no cliente
// joinMatch: transação, valida ban, lotação, duplicidade
// leaveMatch: transação, promove fila, notifica
// onMatchCreated: faixas de geohash sobre collection group `private`, seleção
//   por raio efetivo (B4) com teto de destinatários, histórico + push
// onParticipantChanged: notifica quem subiu da fila (a promoção em si é do leaveMatch)
// submitReport: transação — exige partida em comum, id composto trava duplicidade,
//   reconta denunciantes distintos na janela e reescreve moderation/{uid}
// submitPlayerRating: transação — valida partida encerrada, participação de
//   quem avalia e de quem é avaliado, bloqueia autoavaliação, id composto
//   {rater}_{rated} garante unicidade, recalcula rating/ratingCount do perfil
// exportUserData: direito de acesso; denúncia contra a pessoa sai sem o denunciante
// deleteAccount: sai das partidas (promovendo a fila), cancela as futuras que
//   organizava, despersonaliza as passadas, anonimiza avaliações e denúncias que
//   escreveu, apaga a trilha de moderação e o usuário do Firebase Auth
```

---

## Modelo de Dados

### Firestore Collections

```
matches/{matchId}
  ├── id, sport, title, venue, address, neighborhood, city
  ├── lat, lng, geohash (para busca por raio)
  ├── startsAt (Timestamp), durationMin
  ├── totalSlots, confirmedCount, waitlistCount (denormalizado, só Functions)
  ├── priceCents, platformFeeCents, level, rules[]
  ├── status: open | full | cancelled | finished
  ├── organizerId, organizerName, organizerAvatarUrl, organizerRating
  ├── createdAt, updatedAt
  └── /participants/{uid} (subcollection)
      ├── userId, displayName, avatarUrl
      ├── status: confirmed | waitlist | cancelled
      ├── paymentStatus: pending | paid | expired | refunded
      └── joinedAt, order

profiles/{uid}
  ├── fullName, nickname, avatarUrl, position, level
  ├── emailVerified, phoneVerified (só Functions — sinal de confiança)
  ├── sports[], city, neighborhood
  ├── rating, ratingCount, matchesPlayed
  ├── isBanned, createdAt, updatedAt
  └── /private/data (subdocumento privado)
      ├── phone, pixKey, email
      ├── lat, lng, geohash, radiusKm
      ├── isAvailable, availableUntil, availableSports[]

users/{uid}/notifications/{notifId}
  ├── type: new_match | vacancy | promoted | payment | rating
  ├── title, body, matchId
  └── readAt, createdAt

matches/{matchId}/ratings/{raterUid}_{ratedUid}   ← registro canônico
  ├── matchId, ratedUserId, raterUserId
  ├── rating (1-5), comment
  ├── punctuality, respect, fairPlay, behavior (1-5, obrigatórias)
  └── createdAtMs (número), createdAt (serverTimestamp, auditoria)

profiles/{uid}/ratings/{mesmo id}                 ← modelo de leitura
  └── cópia escrita na mesma transação de submitPlayerRating
      (a tela de perfil precisa das avaliações RECEBIDAS por alguém)

  As quatro dimensões são obrigatórias — não existe cliente antigo para
  acomodar. Cada uma agrega em `<dim>Average` no perfil, compartilhando o mesmo
  `ratingCount`: toda avaliação traz as quatro, então as contagens não divergem.

reports/{reportId} (admin only)
  ├── reporterId, reportedUserId, matchId
  ├── reason, details, status
  └── createdAt

moderation/{uid} (admin only)
  ├── level: warning | suspended | banned
  ├── until, reason, history[]
```

### Índices Firestore

```json
{
  "indexes": [
    {
      "collectionGroup": "matches",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "geohash", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "matches",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "sport", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "startsAt", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "participants",
      "queryScope": "COLLECTION_GROUP",
      "fields": [
        { "fieldPath": "userId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" }
      ]
    }
  ]
}
```

---

## Cronograma Detalhado

### Ordem de Execução

```
Fase 0 (infra)
   ↓
Fase 1 ←── Fase 2 (paralelo)
   ↓           ↓
Fase 3 ←──────
   ↓
Fase 4 ←── Fase 5 (paralelo)
   ↓           ↓
Fase 6 ←──────
   ↓
Fase 7
```

### Timeline Realista

| Fase | Escopo | Horas | Status |
|---|---|---|---|
| **0** | Firebase, firestore/, core/* | 20-25h | ✅ DONE |
| **1** | GameList + Search | 15-20h | ✅ DONE |
| **2** | Create + MyMatches + Profile | 20-25h | ✅ DONE |
| **3** | Real-time + Join/Leave | 15-20h | ✅ DONE |
| **4** | Cancel + UI Dialogs + Ratings | 10-15h | ✅ DONE |
| **5** | Player Search & Filters | 15-20h | ✅ DONE |
| **6** | Trust & Safety | 10-15h | 🚧 EM ANDAMENTO |
| **7** | Map + Polish | 8-12h | ⏳ |
| **TOTAL** | Completo | **113-152h** | **~50% done** |

---

## Riscos e Mitigações

| # | Risco | Impacto | Mitigação | Status |
|---|---|---|---|---|
| R1 | firestore.rules do Lexis nega tudo | Bloqueia Phase 1 | Reescrita completa | ✅ RESOLVIDO |
| R2 | Busca por raio (Firestore) | Sem matchmaking | Geohash + testes | ✅ IMPLEMENTADO |
| R3 | Overbooking na última vaga | Quebra de confiança | joinMatch callable + transação; contador corrigido na promoção (2026-08-18) | ✅ IMPLEMENTADO |
| R4 | Mapa em CMP | Sem solução madura | expect/actual isolado; Fase 7 | ⏳ DEFER |
| R5 | Assinatura por Pix → rejeitada | App rejeitado na App Store | RevenueCat (entitlements) | ✅ DECIDIDO |
| R6 | Room KMP primeira adoção | Atrito de build, KSP iOS | Spike Fase 0 | ✅ FEITO |
| R7 | Custo Firestore (notificações) | Conta cresce | Teto de 50 km e 60 destinatários por partida | ✅ IMPLEMENTADO |
| R8 | Múltiplos Firebase pods (iOS) | Erros de linkagem | Módulo firestore/ único | ✅ IMPLEMENTADO |
| R9 | QR Pix via API terceiro | Vazamento de chave | Gerar QR no dispositivo | ✅ IMPLEMENTADO |
| R10 | AnalyticsTracker com métodos Bible | Confusão, erros | Limpar na Fase 0 | ✅ FEITO |
| R11 | CI apontava para `:products:bible` | Nada era compilado nem testado | Workflow reescrito para `:products:games` + gatilho de push | ✅ RESOLVIDO |

---

## Próximas Ações

### Concluído (Sprint 1 e 2)

1. **Phase 5 - Player Search** ✅
   - [x] `SearchPlayersUseCase`, `GetPlayerDetailsUseCase`
   - [x] Firestore queries (rating, sports, matches count)
   - [x] `PlayerSearchStep` + `PlayerSearchResultCard`

2. **`PlayerFiltersPanel`** ✅
   - [x] Rating range slider (0-5 ⭐)
   - [x] Sport multi-select
   - [x] Min matches (organized, participated)
   - [x] Sort dropdown (rating, recent, matches, name)

3. **`PlayerDetailsStep`** ✅
   - [x] Ratings section com estrelas e comentários
   - [x] Distribution histogram (1-5 ⭐) via `RatingSummary`
   - [x] Latest 5 reviews
   - [x] [Ver todas as avaliações] → lista paginada

4. **Ratings Display & List** ✅
   - [x] `PlayerRatingsListStep` (paginado, 20/página, prefetch + botão)
   - [x] `RatingStars`, `RatingSummary` no cedarDS
   - [x] Sort filters (Recentes, Melhores, Piores) — ordenação no servidor

6. **Testes & Strings** ✅
   - [x] Unit tests (`RatingDistribution`, `RatingCursor`, formatters de `core`)
   - [x] Turbine tests (`PlayerDetailsStepModel`, `PlayerRatingsListStepModel`)
   - [x] Strings pt-BR/en-US (`PlayerDetailsStrings`, `PlayerRatingsStrings`)

### Backend de avaliação ✅

7. **`submitPlayerRating` (Cloud Function)**
   - [x] Transação com validação de partida encerrada e de participação
   - [x] Unicidade por id composto `{rater}_{rated}`, reenvio idempotente
   - [x] Recalcula `rating`/`ratingCount` do perfil (cliente não escreve reputação)
   - [x] Regras do Firestore para `profiles/{uid}/ratings`
   - [x] Testes de emulador (callable) e de regras
   - [ ] **Falta:** `firebase deploy --only functions,firestore:rules,firestore:indexes`

### Sprint 3 — Polish ✅

5. **Caching & Performance**
   - [x] Cache em memória com TTL de 5 min (`InMemoryPlayerCache`)
   - [x] Debounce + cancelamento no lugar de paginação (ver D19)
   - [ ] ~~Pre-loading de jogadores próximos~~ — o perfil não tem geo pública;
         depende de `profiles/{uid}/private/data`, que só o dono lê

8. **Correções encontradas no Sprint 3**
   - [x] `FirestorePlayerSource` lia `displayName`/`photoUrl`/`createdAtSeconds`;
         o documento tem `fullName`/`avatarUrl`/`createdAt`. Toda a busca voltava
         vazia e o Player Details lançava "Player data incomplete"
   - [x] Ordenações por `lastActivitySeconds` e `totalMatches` removidas — o
         Firestore descarta documentos sem o campo do `orderBy`, então elas
         devolviam lista vazia
   - [x] Seção Experiência e filtros de mín. partidas removidos (sem escritor)

### Semana 3

7. **Polish**
   - [ ] E2E testing
   - [ ] Bug fixes
   - [ ] Code review
   - [ ] Merge to main

---

## Regras de Negócio Críticas

### B1: Taxa de Plataforma = 7%

```kotlin
platformFeeCents = (priceCents * 0.07).roundToInt()
```

**Status:** ⏳ Desligada no MVP (Fase 5)

### B2: Slot Logic

```
openSlots = max(totalSlots - confirmedCount, 0)
joining com 0 vagas → waitlist
```

**Status:** ✅ Implementada (Fase 3)

### B3: Waitlist Auto-promoção FIFO

```
Ao cancelar: promove o primeiro da fila por created_at
Notifica: "Você subiu da fila!"
```

**Status:** ✅ Implementada (Fase 3)

### B4: Raio Mínimo de Notificação = 20km

```
notifyRadius = min(max(userRadiusKm, 20), 50)
```

Teto de 50 km além do mínimo: sem ele um perfil com raio absurdo faria toda
criação de partida varrer a base inteira. Ver `functions/src/notifications.ts`.

**Status:** ✅ Implementada (Phase 6, `selectRecipients`)

### B5: Janela de Disponibilidade = now + 6h

```
Toggle "Estou disponível" abre dialog
availableUntil = now + 6 horas
```

**Status:** ⏳ PENDENTE — nada liga `isAvailable`, que nasce `false`. Enquanto
não existir, o filtro de disponibilidade em `selectRecipients` fica desligado e
todo mundo no raio recebe aviso de qualquer partida (D25).

### B6: Organizador Auto-entra como Confirmed+Paid

```
Creating a match → auto-join como confirmed+paid
Na própria partida que criou
```

**Status:** ✅ Implementada (Fase 3)

### B7: Pix 100% Local

```
BR Code EMV gerado no cliente (core/payments/PixPayloadBuilder)
Nunca chamamos API de terceiro (qrserver.com etc)
```

**Status:** ✅ Implementada (Fase 0)

### B8: Broadcast WhatsApp

```
nearbyAvailablePlayers(): Callable
  - raio 30km
  - top 60 jogadores
  - service-role (só organizador)
openUrl("wa.me/...?text=...") — API oficial
```

**Status:** ⏳ Phase 4

### B9: Contagem de Vagas = Participant Counting (Client-side)

```
confirmedCount = len(participants.filter { status == CONFIRMED })
totalSlots = denormalizado em match doc
openSlots = max(totalSlots - confirmedCount, 0)

Updates via snapshots() realtime
```

**Status:** ✅ Implementada (Fase 3)

### B10: Plataforma Taxa via Pix Direto

```
Sem escrow, sem gateway
Direto na chave Pix do organizador
```

**Status:** ⏳ Desligada no MVP

---

## Decisões Arquiteturais

| # | Decisão | Escolha | Motivo |
|---|---|---|---|
| **D1** | Acesso Firestore no KMP | `expect`/`actual` SDK nativo | Padrão identity, controle total |
| **D2** | Organização módulos | `products/games|player|notifications` | Separação de responsabilidades |
| **D3** | Cache local | Room KMP (fonte verdade) + DataStore | Offline-first, performance |
| **D4** | Fase 1 scope | Paridade total com Lovable | Validação de regras de negócio |
| **D5** | Padrão UI | StateFlow + Channel (identity) | Consistência no projeto |
| **D6** | Módulo Firestore | Único, compartilhado | Evita duplicação de pods iOS |
| **D7** | Assinatura | RevenueCat (IAP) | Apple/Google requerem para funcionalidades digitais |
| **D8** | Pagamentos | Pix P2P (sem gateway) | Rateio direto, sem escrow |
| **D9** | Search raio | Geohash (GeoFire padrão) | Firestore não tem query por distância |
| **D10** | Ratings | Não agregados no server | Client-side reduce, recalcula ao submeter |
| **D11** | Timestamp de rating | Campo numérico `createdAtMs` | Sobrevive ao interop Android/iOS e serve de cursor `startAfter` |
| **D12** | Paginação de ratings | Cursor opaco + `orderBy` no servidor | Ordenar página parcial no cliente reembaralha a lista ao carregar mais |
| **D13** | Formatação numérica | `core/format` próprio | `String.format` é JVM-only e quebra o build iOS |
| **D14** | Avaliações recebidas | Cópia em `profiles/{uid}/ratings` | `users/` é a árvore privada do dono; perfil é público pra quem está logado |
| **D15** | Fim da partida | `startsAt + durationMin` no passado | Nada marca `status: FINISHED` ainda — exigir esse status travaria toda avaliação |
| **D16** | Reenvio de avaliação | Idempotente (`already_rated`) | Mesmo padrão de `joinMatch`/`cancelMatch`; reenviar não infla a média |
| **D17** | Estatísticas de experiência | Fora do perfil de terceiros | Nada escreve os contadores; o perfil próprio deriva das próprias partidas, o de terceiros exigiria collection group + permissão. Volta na Phase 6 |
| **D18** | Cache de jogador | Memória, TTL 5 min | Busca refaz query a cada filtro e a cada volta do perfil; TTL curto evita o repeteco sem segurar nome/nota velhos |
| **D19** | Busca de jogadores | Debounce + teto, sem paginação | Filtro de nota/esporte roda no cliente: paginar em cima disso dá páginas de tamanho aleatório e um "tem mais" que mente. A UI avisa quando o teto é atingido |
| **D20** | Escalonamento | Conta denunciantes distintos | Contar denúncias cruas deixaria uma pessoa sozinha derrubar outra abrindo dez |
| **D21** | Fim da suspensão | `untilMs` comparado na hora da leitura | Nada roda para limpar o documento quando o prazo vence; a regra e o guard comparam a data |
| **D22** | Banimento | Nunca automático | Banir por contagem é vetor de brigading: um grupo coordenado elimina qualquer jogador. No limiar mais alto a conta é suspensa e marcada para revisão humana |
| **D23** | Sair e cancelar partida | Liberados para conta restrita | Bloquear a saída prenderia a pessoa segurando uma vaga — o oposto do que se quer |
| **D24** | Partida encerrada | Derivado do relógio, não persistido | Nada marcava `status: FINISHED`, então o botão de avaliar nunca aparecia. `startsAt + durationMin` no passado é a mesma verdade que o servidor já usa |
| **D25** | Filtro de disponibilidade na notificação | Desligado por ora | `isAvailable` nasce `false` e o toggle (B5) não existe; filtrar por ele hoje seria não notificar ninguém, nunca |
| **D26** | Contadores de experiência | Fora do produto | Decisão do Vitor: nem trigger, nem seção. Sai do escopo em vez de virar dívida |
| **D27** | Assinatura e limite de plano | Fora do MVP | Sem RevenueCat ativo e sem limite de partidas por plano até haver decisão de monetização |
| **D28** | Export LGPD de denúncia | Denunciante redigido | Direito de acesso é sobre os dados da pessoa; revelar quem denunciou entrega dado de terceiro e abre caminho para retaliação |
| **D29** | Quem vira admin | Script fora do produto | A claim `admin` só é concedida por `scripts/grant-admin.mjs` com credencial de serviço. Não há caminho pelo app: quem pode banir se decide fora do produto |
| **D30** | Decisão de moderação | Callable, nunca escrita direta | `moderation/{uid}` e o espelho `profiles.isBanned` têm de mudar juntos; duas escritas acopladas não saem do cliente, nem do admin |
| **D31** | Conteúdo autoral na exclusão | Anonimizar, não apagar | A avaliação também é dado de quem foi avaliado, e a denúncia é prova contra outro. Apagar deixaria qualquer um limpar o próprio rastro excluindo a conta |
| **D32** | Partidas do organizador excluído | Futura cancela, passada despersonaliza | Apagar levaria junto o histórico de todo mundo que jogou; o que precisa sumir é o nome, não o registro |
| **D33** | CI | Roda também em `push` para `main`/`developer` | O workflow só tinha gatilho de `pull_request` e o trabalho vai direto para `developer` — nunca rodou uma vez |
| **D34** | Partida encerrada | Calculada pelo relógio na UI, `status` nunca vira `FINISHED` | Nada escrevia esse status, então `canRate` nunca ligava e todo o fluxo de avaliação ficou inalcançável. `startsAt + durationMin` no passado é a mesma verdade que o servidor já usa em `submitPlayerRating` — uma fonte só, sem cron |
| **D35** | Exigir verificação | Capacidade pronta, política desligada | Ligar tranca de uma hora para outra todo mundo que já usa o app e nunca verificou. Construir e exigir são decisões separadas; a segunda precisa de aviso antes |
| **D36** | Fonte do selo | Claim do ID token, não campo do perfil | O espelho no perfil serve para os outros verem; a decisão de barrar lê o token, que é assinado e não fica desatualizado |
| **D37** | Semente da reputação | `rating: 0`, não `5` | O 5 era placeholder de exibição e obrigava um caso especial na média para não transformar a primeira nota 1 em 3. Com 0 a conta natural já dá certo. Quem exibe decide o que mostrar com `ratingCount == 0` |
| **D38** | Dimensões da avaliação | Obrigatórias | O ramo opcional existia para um cliente anterior que nunca existiu em produção. Mantê-lo significaria duas formas de avaliação para sempre e perfis com metade das médias agregadas |

---

## Checklist de Verificação

### Phase 1 Acceptance

- [ ] Partidas reais do Firestore na Home
- [ ] Ordenadas por distância
- [ ] Filtros (esporte, raio) persistem
- [ ] App offline mostra cache
- [ ] Testes verdes
- [ ] Screenshot tests passam
- [ ] Android + iOS compilando

### Phase 2 Acceptance

- [ ] Create Match: form valida, salva, navega
- [ ] MyMatches: abas Ativas/Passadas
- [ ] Profile: mostra stats + prefs
- [ ] Strings localizadas
- [ ] Sem crashes/ANRs

### Phase 3 Acceptance

- [ ] Join Match: dois dispositivos na última vaga
- [ ] Um confirma, outro vai para waitlist
- [ ] Ao sair: fila é promovida
- [ ] Realtime com snapshots()
- [x] ~~Limites por plano (free = 1 ativa)~~ — fora do MVP (D27); nunca existiu no código

### Phase 4 Acceptance

- [ ] Leave/Cancel dialogs com confirmação
- [ ] Atualização realtime de status
- [ ] Ratings mostram corretamente
- [ ] Sem overbooking / race conditions
- [ ] Notificações funcionam

### Phase 6 Acceptance

- [ ] Denunciar exige partida em comum; denunciar estranho é recusado
- [ ] Mesma pessoa não conta duas vezes na mesma partida
- [ ] 3 denunciantes distintos → advertência; 6 → suspensão com prazo
- [ ] Banimento só sai do painel, nunca da contagem
- [ ] Conta suspensa não entra em partida, não avalia, não denuncia, não cria
- [ ] Conta suspensa AINDA consegue sair de partida (não pode ficar presa)
- [ ] Suspensão vencida destrava sem ninguém rodar nada
- [ ] Painel: fila de revisão carrega, decisão aplica, `isBanned` espelha no perfil
- [ ] `exportUserData` devolve os dados da pessoa sem revelar quem a denunciou
- [ ] `deleteAccount` libera as vagas, promove a fila e apaga o usuário do Auth
- [ ] Avaliação nas quatro dimensões grava e agrega
- [ ] App dispara verificação de e-mail e de telefone
- [ ] Selo de verificado aparece no perfil de outra pessoa
- [ ] Decidir SE exigir verificação para entrar/criar partida (hoje desligado, D35)

### Phase 7 Acceptance

- [ ] Mapa renderiza pins reais no Android e no iOS
- [ ] Tema Cedar aplicado; nenhum resquício visual do Lexis
- [ ] Ícones próprios nas duas lojas
- [ ] Funil view → join → confirm → play chega no analytics
- [ ] Job de CI para iOS (hoje nada compila o alvo iOS — ver [CI](#))

### Phase 5 Acceptance

- [ ] Search jogadores por nome, esporte, rating
- [ ] Filtros avançados funcionam
- [ ] Player details mostra stats + reviews
- [ ] Rating distribution visível
- [ ] Avaliações do jogador paginam de 20 em 20
- [x] ~~Pagination nos resultados de busca~~ — decidido contra (D19): o filtro de
      nota e esporte roda no cliente, então paginar daria páginas de tamanho
      aleatório. A UI avisa quando o teto de leitura é atingido
- [ ] Testes E2E passam

---

## Referências

- `README.md` — Overview do projeto
- `E2E_VERIFICATION.md` — Testes de Phase 1
- `PHASE2_PLAN.md` — Detalhes Phase 2
- `.claude/migration_plan.md` — Plano original (Lovable → Match)
- `docs/PLANO_MIGRACAO_MATCH.md` — Plano atualizado com status real

---

**Mantido por:** Vitor Walcker  
**Última atualização:** 2026-08-18  
**Próxima revisão:** Após Phase 5 completa
