# Mobile Match — Roadmap Consolidado

**Último atualizado:** 2026-08-18 · **Status:** Phase 5 Sprint 1-2 complete, Sprint 3 (polish) next

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

### Phase 5: Player Search & Filters ⏳ PENDENTE (15-20h)

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

3. **Polish & Integration** (2-4h) ⏳
   - Caching em memória (TTL 5 min)
   - Pagination nos resultados de busca
   - Pre-loading de jogadores próximos
   - Testes E2E

---

### Phase 6: Confiança e Segurança ⏳ PENDENTE (10-15h)

**Objetivo:** O produto sobrevive ao primeiro usuário mal-intencionado.

**Escopo:**
- 📋 **Avaliações pós-partida** (pontualidade, respeito, fair play, comportamento)
- 🚨 **Denúncias** com 10 motivos customizados
- 🛡️ **Moderação** (advertência → suspensão → banimento)
- ✉️ **Verificação** de e-mail e telefone (SMS)
- 🔐 **LGPD** (exclusão de conta, exportação, minimização de dados)

---

### Phase 7: Mapa e Polimento ⏳ PENDENTE (8-12h)

**Objetivo:** Experiência visual completa e identidade do produto.

**Escopo:**
- 🗺️ **Mapa interativo** (Google Maps / MapKit via `expect`/`actual`)
- 🎨 **Tema Cedar** (identidade esportiva, remover Lexis/Bible leftovers)
- 🏆 **Ícones** Android/iOS personalizados
- 📊 **Analytics** de funil (view → join → confirm → play)
- 📱 **Painel Admin** (fora do app, web separado)

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
// onMatchCreated: busca por geohash, notifica, push
// onParticipantChanged: notifica vaga, promove fila
// submitRating: recalcula média em transação
// deleteAccount: estendido do identity, limpa match data
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

matches/{matchId}/ratings/{raterUid}
  ├── ratedId, stars, punctuality, respect, fairPlay, behavior
  └── comment, createdAt

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
| **5** | Player Search & Filters | 15-20h | ⏳ NEXT |
| **6** | Trust & Safety | 10-15h | ⏳ |
| **7** | Map + Polish | 8-12h | ⏳ |
| **TOTAL** | Completo | **113-152h** | **~50% done** |

---

## Riscos e Mitigações

| # | Risco | Impacto | Mitigação | Status |
|---|---|---|---|---|
| R1 | firestore.rules do Lexis nega tudo | Bloqueia Phase 1 | Reescrita completa | ✅ RESOLVIDO |
| R2 | Busca por raio (Firestore) | Sem matchmaking | Geohash + testes | ✅ IMPLEMENTADO |
| R3 | Overbooking na última vaga | Quebra de confiança | joinMatch callable + transação | ✅ IMPLEMENTADO |
| R4 | Mapa em CMP | Sem solução madura | expect/actual isolado; Fase 7 | ⏳ DEFER |
| R5 | Assinatura por Pix → rejeitada | App rejeitado na App Store | RevenueCat (entitlements) | ✅ DECIDIDO |
| R6 | Room KMP primeira adoção | Atrito de build, KSP iOS | Spike Fase 0 | ✅ FEITO |
| R7 | Custo Firestore (notificações) | Conta cresce | Raio máximo, limite destinatários | ⏳ MONITOR |
| R8 | Múltiplos Firebase pods (iOS) | Erros de linkagem | Módulo firestore/ único | ✅ IMPLEMENTADO |
| R9 | QR Pix via API terceiro | Vazamento de chave | Gerar QR no dispositivo | ✅ IMPLEMENTADO |
| R10 | AnalyticsTracker com métodos Bible | Confusão, erros | Limpar na Fase 0 | ✅ FEITO |

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

### Próximo (Sprint 3 — Polish)

5. **Caching & Performance**
   - [ ] In-memory cache (TTL 5 min)
   - [ ] Pagination nos resultados de busca de jogadores
   - [ ] Pre-loading de jogadores próximos

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
notifyRadius = max(userRadiusKm, 20)
```

**Status:** ⏳ Implementada em Phase 4

### B5: Janela de Disponibilidade = now + 6h

```
Toggle "Estou disponível" abre dialog
availableUntil = now + 6 horas
```

**Status:** ⏳ Phase 2

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
- [ ] Limites por plano (free = 1 ativa)

### Phase 4 Acceptance

- [ ] Leave/Cancel dialogs com confirmação
- [ ] Atualização realtime de status
- [ ] Ratings mostram corretamente
- [ ] Sem overbooking / race conditions
- [ ] Notificações funcionam

### Phase 5 Acceptance

- [ ] Search jogadores por nome, esporte, rating
- [ ] Filtros avançados funcionam
- [ ] Player details mostra stats + reviews
- [ ] Rating distribution visível
- [ ] Pagination 20 resultados
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
