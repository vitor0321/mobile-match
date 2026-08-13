# Plano de Migração — `score-link-go` (Lovable/Supabase) → `mobile-match` (KMP + CMP + Firebase)

**Autor:** Vitor Walcker · **Data:** 12/08/2026 · **Status:** plano final consolidado

---

## 1. Contexto

| | Origem | Destino |
|---|---|---|
| **Repo** | `score-link-go-main` | `mobile-match` |
| **Stack** | TanStack Start + React 19 + Tailwind + shadcn/ui | Kotlin Multiplatform + Compose Multiplatform |
| **Backend** | Supabase (Postgres + RLS + Realtime + Triggers) | Firebase (Auth + Firestore + Functions + FCM) |
| **Navegação** | TanStack Router (file-based) | Voyager (`Screen` / `ScreenModel`) |
| **DI** | — (hooks) | Koin |
| **Plataformas** | Web responsivo | Android + iOS nativos |
| **Estado** | `useState` + `useEffect` por rota | `StateScreenModel` (estado) + `Channel` (efeitos) |

O `score-link-go` é um protótipo funcional gerado no Lovable: prova o fluxo de negócio e traz o
modelo de dados já pensado, mas mistura camadas (query do Supabase dentro do componente de tela)
e depende de infraestrutura que não existe no destino. **Nada de código será portado literalmente
— o que migra é a regra de negócio e o modelo de dados.**

### Decisões desta migração

| # | Decisão | Escolha |
|---|---|---|
| D1 | Acesso ao Firestore no KMP | `expect`/`actual` com SDK nativo (mesmo padrão do `products/identity`) |
| D2 | Organização de módulos | `products/games` + `products/player` + `products/notifications` |
| D3 | Cache local | Room KMP (fonte da verdade) + DataStore (preferências) |
| D4 | Escopo da fase 1 | Paridade total com o Lovable |
| D5 | Padrão de UI | `StateScreenModel` + `Channel` para efeitos (padrão `identity`) |
| D6 | Módulo Firestore compartilhado | `firestore/` único para evitar duplicação de pods iOS |

---

## 2. Inventário: o que existe hoje na origem (MVP Lovable)

### 2.1 Rotas (React) → destino

| Rota origem | O que faz | Módulo destino | Tela destino |
|---|---|---|---|
| `routes/index.tsx` | Home: mapa ao vivo com pins, filtro por esporte, raio, toggle "estou disponível", geolocalização, realtime, contagem de vagas via participant counting | `products/games` | `HomeStep` |
| `routes/buscar.tsx` | Busca textual + filtros (esporte, raio, só com vaga), lista sem mapa | `products/games` | `SearchStep` |
| `routes/criar.tsx` | Formulário de criação (esporte, local, data, vagas, valor, nível, regras, Pix key), taxa 7%, auto-join do organizador como confirmed+paid | `products/games` | `CreateMatchStep` |
| `routes/partida.$id.tsx` | Detalhe: entrar/sair, lista de espera, Pix "Já paguei", denunciar, WhatsApp do organizador, realtime de participantes | `products/games` | `MatchDetailStep` |
| `routes/meus-jogos.tsx` | Abas jogador (participações) e organizador (partidas criadas + arrecadação) | `products/games` | `MyMatchesStep` |
| `routes/perfil.tsx` | Perfil, chave Pix, telefone, posição, raio, planos | `products/player` | `PlayerProfileStep` |
| `routes/auth.tsx` | Login/cadastro e-mail + Google | — | **já existe** em `products/identity` |
| `routes/__root.tsx` | Shell + BottomNav (5 abas) + Toaster global | `app` | `MatchScaffold` (novo) |

### 2.2 Regras de negócio críticas (extraídas do MVP)

| # | Regra | Origem | Destino |
|---|---|---|---|
| B1 | **Taxa de plataforma = 7%** do preço (`platform_fee_cents = round(price_cents * 0.07)`) | `criar.tsx` | `CreateMatchStep` + model |
| B2 | **Slot logic:** `left = max(total_slots - confirmedCount, 0)` — joining com 0 vagas → waitlist | `partida.$id.tsx` | `joinMatch` callable |
| B3 | **Waitlist auto-promoção** FIFO por `created_at` ao cancelar | DB trigger `promote_waitlist()` | Function `onParticipantChanged` |
| B4 | **Raio mínimo de notificação** = `GREATEST(radius_km, 20)` — piso de 20km | trigger `notify_nearby_players()` | Function `onMatchCreated` |
| B5 | **Janela de disponibilidade** = `now + 6h` | `index.tsx` toggleAvailable | `products/player` |
| B6 | **Organizador auto-entra** como confirmed+paid na própria partida | `criar.tsx` submit | `joinMatch` callable |
| B7 | **Pix 100% local** — BR Code EMV gerado no cliente, sem gateway | `lib/pix.ts` | `core/payments/PixPayloadBuilder` |
| B8 | **Broadcast WhatsApp** — callable `nearbyAvailablePlayers` (service-role, raio 30km, top 60) | `broadcast.functions.ts` | Function `nearbyAvailablePlayers` |
| B9 | **Contagem de vagas** via participant counting (client-side reduce), não agregado no DB | `index.tsx` | Denormalizado no Firestore via Functions |
| B10 | **Plataforma taxa cobrada via Pix direto** — sem escrow | `criar.tsx` | Desligada no MVP (Fase 5) |

### 2.3 Supabase → Firestore: mapeamento de funções do banco

| Função Postgres | Comportamento | Destino Firebase |
|---|---|---|
| `handle_new_user()` | Cria profile, role `user`, subscription `free` | Function `onUserCreate` (Auth trigger) |
| `notify_nearby_players()` | Ao criar partida: notifica jogadores no raio ≥ 20km | Function `onMatchCreated` (Firestore trigger) |
| `notify_vacancy()` | Ao liberar vaga: notifica disponíveis próximos se não há fila | Function `onParticipantChanged` |
| `promote_waitlist()` | Ao cancelar: promove primeiro da fila (FIFO) | Function `onParticipantChanged` (transação) |
| `distance_km()` | Haversine em SQL (R=6371) | `core/geo/GeoHash.kt` (cliente) + `functions/src/geo.ts` (servidor) |
| `has_role()` | Checagem de papel | **Custom Claims** do Firebase Auth |

---

## 3. Inventário: o que existe hoje no destino (mobile-match)

### 3.1 Módulos do projeto

```
app/                    entry point, Koin (só coreModules + identityModule), App.kt com else vazio
core/                   dispatchers, navigatorHolder, retry, analytics (com leftover do Bible app)
navigator/              GamesDestination { fun gameList(): Screen } + IdentityDestination
cedarDS/                CedarTheme (com leftover Bible bodyVerse), CedarTopBar, LoadingIndicator
products/identity/      ★ REFERÊNCIA — auth, sessão, paywall, RevenueCat (padrão a ser clonado)
products/games/         scaffolding: GameListStep com InMemoryGameSource (NÃO conectado ao app)
```

### 3.2 Estado atual do `products/games` (scaffolding)

- `Game` data class com `startsAt: String` (TODO: trocar por `kotlinx-datetime`)
- `Sport` enum com 10 modalidades (PT labels)
- `GameSource` interface + `InMemoryGameSource` (dados hardcoded)
- `GameListStep` / `GameListStepModel` (usa `ScreenModel` + `MutableStateFlow` — **diverge do padrão identity**)
- **Sem** `api/`, `domain/error/`, Firebase data source, Lyricist strings, Room, DataStore, testes
- **NÃO está no AppModule** — `initKoin` só carrega `coreModules + identityModule`

### 3.3 gaps do functions/ (Bible app stale)

`functions/src/index.ts` contém callables do app Bíblico (`getVerseExplanation`, `getBookExplanation`, `translateStrongsDefinition`) — **nenhum callable do Match existe ainda**. O `firestore.rules` já está match-ready e espera callables que ainda não existem.

### 3.4 Dependências já no `libs.versions.toml` (não usadas ainda)

Room 2.7.1, sqlite-bundled 2.5.1, DataStore 1.1.4, Firebase BOM 33.12.0 (auth/functions/firestore/crashlytics), Coil 3.3.0, Ktor 3.4.0, RevenueCat 2.10.2, Turbine 1.2.0, Lyricist 1.8.0, Paparazzi 2.0.0-alpha04

**Faltam:** `kotlinx-datetime`, `firebase-messaging`, `maps-compose` + `play-services-location`, `firebase-appcheck`

### 3.5 `firestore.rules` — JÁ CORRETO

As regras no repo já são do Match (não do Lexis como anteriormente documentado). Princípio:
- Escrita em `participants`, `confirmedCount`, `payments`, `subscription`: **negada ao cliente** (só Functions)
- `profiles/{uid}/private/**`: `request.auth.uid == uid`
- Admin via `request.auth.token.admin == true`
- Validações de conteúdo em `matches` (slots 2-40, preço 0-100000, startsAt futuro)

---

## 4. Arquitetura de destino

### 4.1 Mapa de módulos

```
app/                        entry point, Koin, shell de navegação, bottom bar
core/                       DI, analytics, crash, dispatchers, retry
  └ geo/                    ⊕ geohash + haversine + formatação de distância
  └ payments/               ⊕ PixPayloadBuilder (EMV BR Code + CRC16)
  └ datetime/               ⊕ "Hoje · 20:00", "Amanhã · 19:30"
  └ location/               ⊕ expect/actual — permissão + GPS
firestore/                  ⊕ NOVO — encapsula o SDK Firestore (cocoapods + framework iOS)
navigator/                  contratos entre produtos (Voyager Screens)
cedarDS/                    design system: MatchCard, SportChip, SlotBadge, BottomBar, Map
products/identity/          auth, perfil de conta, paywall, RevenueCat        [existe]
products/games/             ⊕ partidas: home, busca, criar, detalhe, participação, meus jogos
products/player/            ⊕ perfil do jogador, disponibilidade, raio, esportes, Pix, avaliações
products/notifications/     ⊕ feed, FCM, WhatsApp broadcast
functions/                  ⊕ triggers, callables, moderação                 [existe, stale]
```

> **Módulo `firestore/` separado:** com D1 (SDK nativo por `expect`/`actual`), cada
> módulo que fala com o Firestore precisaria do seu próprio `cocoapods { pod("FirebaseFirestore") }`
> e do seu próprio framework exportado para o Xcode. Um módulo `firestore/` declara o pod uma vez
> e expõe uma API comum — `document`, `collection`, `query`, `snapshots`, `runTransaction`, `callable` —
> que os produtos consomem sem tocar no SDK.

### 4.2 Camadas dentro de cada produto (padrão identity)

```
products/<name>/src/commonMain/kotlin/com/walcker/<name>/
├── <Name>DestinationImpl.kt          implementa o contrato do navigator
├── api/                              CONTRATOS PÚBLICOS entre módulos
│   └── <SharedHolder>.kt             interfaces que outros módulos consomem
├── di/<Name>Module.kt                public val = listOf(strings, platform, data, ui)
├── features/
│   ├── domain/                       internal — sem dependência de framework
│   │   ├── model/                    entidades de domínio
│   │   ├── error/                    sealed class <Name>Error + message resolvers
│   │   ├── repository/               interfaces retornando Result<T>
│   │   └── usecase/                  interface + Impl, operator fun invoke()
│   ├── data/                         internal
│   │   ├── remote/                   <Name>Source (interface) + expect fun create…
│   │   ├── local/                    Room: <Name>Entity, <Name>Dao, <Name>Database
│   │   ├── prefs/                    DataStore: filtros, preferências, timestamps
│   │   ├── mapper/                   Dto ↔ Entity ↔ Model
│   │   ├── repository/               …RepositoryImpl
│   │   └── di/<Name>DataModule.kt
│   └── ui/                           internal
│       ├── <feature>/                <FeatureStep>, <FeatureStepModel>, <FeatureState>, <FeatureEvents>
│       └── di/<Name>UiModule.kt
└── strings/                          Lyricist i18n
```

**Regras (clonadas do `identity`):**

- Fluxo `data/ → domain/ → ui/`; `domain` não conhece `data`.
- `Step` implementa `Screen` do Voyager; `StepModel` estende `StateScreenModel`.
- `State` com campos `val` e `ImmutableList` / `ImmutableMap`.
- `domain/` e `data/` são `internal` por padrão; só `api/` e o `Destination` são públicos.
- Repositórios retornam `Result<T>`; exceções do SDK são mapeadas para um `sealed class …Error` antes de cruzar a fronteira do `data`.
- `koinScreenModel()` apenas dentro de um `Screen`.
- Strings via Lyricist — **nada hardcoded**.

### 4.3 Contratos no `navigator`

```kotlin
public interface GamesDestination {
    fun home(): Screen
    fun search(sport: String? = null): Screen
    fun createMatch(): Screen
    fun matchDetail(matchId: String): Screen
    fun myMatches(): Screen
}

public interface PlayerDestination {
    fun profile(): Screen
    fun availability(): Screen
    fun ratings(userId: String): Screen
}

public interface NotificationsDestination {
    fun feed(): Screen
}
```

Nenhum produto depende de outro produto — só do `navigator`. Estado compartilhado entre produtos
sai por `api/` (como `SessionHolder` e `ProStateHolder` do `identity`); o `player` vai expor um
`PlayerProfileHolder` para que `games` leia localização e raio sem depender do módulo.

---

## 5. Modelo de dados no Firestore

### 5.1 Separation: RLS por documento

No Postgres (Lovable), `profiles` tinha `profiles_public_read` (linha inteira pública). No Firestore a permissão é **por documento**, então campos sensíveis ficam no subdocumento privado:

```
profiles/{uid}                         leitura: qualquer autenticado · escrita: dono
  fullName, nickname, avatarUrl, position, level, sports[],
  city, neighborhood, rating, ratingCount, matchesPlayed,
  isBanned, createdAt, updatedAt

profiles/{uid}/private/data            leitura/escrita: só o dono (e Functions)
  phone, pixKey, email,
  lat, lng, geohash, radiusKm,
  isAvailable, availableUntil, availableSports[]
```

### 5.2 Coleções

```
matches/{matchId}                      leitura: autenticado · criação/edição: organizador
  organizerId, organizerName, organizerAvatarUrl, organizerRating   ← denormalizado
  sport, title, venue, address, neighborhood
  lat, lng, geohash                                                 ← para busca por raio
  startsAt (Timestamp), durationMin
  totalSlots, confirmedCount, waitlistCount                         ← denormalizado, só Functions escrevem
  priceCents, platformFeeCents, level, rules[]
  status: open | full | cancelled | finished
  createdAt, updatedAt

matches/{matchId}/participants/{uid}   leitura: autenticado · escrita: só Functions
  userId, displayName, avatarUrl, position
  status: confirmed | waitlist | cancelled
  paymentStatus: pending | paid | expired | refunded
  joinedAt, order

users/{uid}/notifications/{notifId}    leitura/escrita: só o dono
  type: new_match | vacancy | promoted | payment | rating
  title, body, matchId, readAt, createdAt

users/{uid}/payments/{paymentId}       leitura: dono · escrita: só Functions
  matchId, kind, amountCents, method, pixKey, pixPayload, txid,
  status, paidAt, createdAt

users/{uid}/subscription/current       leitura: dono · escrita: só Functions (webhook RevenueCat)
  plan: free | pro | business, status, currentPeriodEnd, source

matches/{matchId}/ratings/{raterUid}   leitura: autenticado · criação: participante da partida
  ratedId, stars, punctuality, respect, fairPlay, behavior, comment, createdAt

reports/{reportId}                     criação: autenticado · leitura: só admin
  reporterId, reportedUserId, matchId, reason, details, status, createdAt

moderation/{uid}                       só admin
  level: warning | suspended | banned, until, reason, history[]
```

### 5.3 Busca por raio: geohash

Firestore não faz consulta por distância. Padrão GeoFire:

1. Gravar `geohash` (precisão 9) com `lat`/`lng` em `matches` e `profiles/{uid}/private/data`.
2. Para raio *R*, calcular intervalos de prefixo de geohash que cobrem a bounding box.
3. Query `orderBy(geohash).startAt(x).endAt(y)` por intervalo (tipicamente 4 a 9).
4. Filtrar excesso no cliente com haversine.

`core/geo/GeoHash.kt`: `encode`, `boundsForRadius`, `distanceKm` — Kotlin puro com testes em `commonTest`.

**Índices** (`firestore.indexes.json`):
- `matches`: `status` ASC + `geohash` ASC
- `matches`: `sport` ASC + `status` ASC + `startsAt` ASC
- `matches`: `organizerId` ASC + `startsAt` DESC
- collection group `participants`: `userId` ASC + `status` ASC

### 5.4 Cloud Functions

| Função | Tipo | Substitui |
|---|---|---|
| `onUserCreate` | Auth trigger | `handle_new_user()` |
| `joinMatch` | **Callable** | Transação: valida ban, lotação, duplicidade; confirmed vs waitlist; atualiza contadores |
| `leaveMatch` | **Callable** | Transação: libera vaga, promove fila, notifica |
| `onMatchCreated` | Firestore trigger | Busca geohash + notifica + FCM |
| `onParticipantChanged` | Firestore trigger | Notifica vaga disponível / promove fila |
| `nearbyAvailablePlayers` | Callable | Lista jogadores com telefone (raio 30km, top 60) |
| `registerPixPayment` | Callable | Grava intenção + payload EMV |
| `confirmPixPayment` | Callable | Organizador marca paid; notifica |
| `submitRating` | Callable | Grava rating + recalcula média em transação |
| `submitReport` | Callable | Grava denúncia |
| `revenuecatWebhook` | HTTPS | Espelha assinatura + atualiza claim |
| `finishMatches` | Scheduled (hora) | `status=finished` após `startsAt + durationMin` |
| `deleteAccount` | Callable | Estender do identity para limpar partidas |

---

## 6. Cache local: Room + DataStore

### 6.1 Divisão de responsabilidades

| Camada | Guarda | Onde |
|---|---|---|
| **Room** | partidas, participantes, notificações — dados de domínio | `products/*/data/local` |
| **DataStore** | filtros, última localização, flag disponibilidade, timestamps sync | `products/*/data/prefs` |
| **Memória** | só o `StateFlow` do `StepModel` | `ui` |

### 6.2 Padrão do repositório (offline-first)

```kotlin
internal class MatchRepositoryImpl(
    private val remote: MatchSource,
    private val dao: MatchDao,
    private val prefs: GamesPreferences,
    private val dispatcher: CoroutineDispatcher,
) : MatchRepository {

    override fun nearbyMatches(filter: MatchFilter): Flow<List<Match>> =
        dao.observeNearby(filter.toQuery()).map { it.map(MatchEntity::toDomain) }

    override suspend fun refreshNearby(filter: MatchFilter): Result<Unit> =
        withContext(dispatcher) {
            withRetry { remote.nearbyMatches(filter) }
                .map { dtos ->
                    dao.upsertAll(dtos.map(MatchDto::toEntity))
                    prefs.setLastSync(Clock.System.now())
                }
        }
}
```

Consequências: abrir o app offline mostra a última lista; a tela nunca fica em branco; `isRefreshing` sobre conteúdo visível.

### 6.3 Room KMP setup

`androidx.room` 2.7.1 e `sqlite-bundled` 2.5.1 já no `libs.versions.toml`. Para cada módulo:
- plugins `ksp` e `room-plugin` no `build.gradle.kts`
- `ksp(libs.room.compiler)` para `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`
- `expect fun createDatabaseBuilder()` com actual por plataforma
- `.setDriver(BundledSQLiteDriver())`

Invalidação: refresh se passaram > 60s ou localização mudou > 500m. Partidas expiradas apagadas no boot.

---

## 7. Plano faseado

### Fase 0 — Fundação (bloqueia tudo)

**Objetivo:** infraestrutura pronta para qualquer feature ser escrita.

**A. Firebase & Backend**
1. Firestore habilitado; `google-services.json` e `GoogleService-Info.plist` já presentes ✓
2. `firestore.rules` já match-ready ✓ — validar com testes no emulador
3. `firestore.indexes.json` — criar com índices da §5.3
4. `functions/`: bootstrap TypeScript (remover código Bible), ESLint, emulador
5. `functions/src/geo.ts` — haversine (mesmos vetores de testes do `core/geo`)
6. `functions/src/index.ts` — `onUserCreate` (Auth trigger)
7. `functions/src/index.ts` — `deleteAccount` estendido para limpar match data

**B. Módulo `firestore/` (novo)**
8. Criar `firestore/build.gradle.kts` com cocoapods `FirebaseFirestore`
9. API comum: `FirestoreClient`, `FirestoreQuery`, `snapshots()`, `runTransaction`, `callable`
10. `actual` Android (Firebase SDK) e iOS (cocoapods)

**C. `core/` — utilitários novos**
11. Adicionar `kotlinx-datetime` ao `libs.versions.toml`
12. `core/geo/`: `GeoHash.encode`, `boundsForRadius`, `distanceKm`, `formatDistance`
13. `core/datetime/`: `formatWhen` ("Hoje · 20:00")
14. `core/payments/`: `PixPayloadBuilder` (TLV + CRC16, porte de `lib/pix.ts`)
15. `core/location/`: `expect`/`actual` permissão + posição (FusedLocation / CoreLocation)

**D. Navegação & App Shell**
16. Atualizar `navigator/GamesDestination.kt` — interface completa (home, search, createMatch, matchDetail, myMatches)
17. Criar `navigator/PlayerDestination.kt` e `navigator/NotificationsDestination.kt`
18. Montar `App.kt` com `Navigator` do Voyager + `MatchScaffold` com bottom bar de 5 abas
19. Gate de autenticação: `SessionHolder.isAuthenticated` → login ou home
20. Atualizar `AppModule.initKoin` para incluir `gamesModule` (+ futuros `playerModule`, `notificationsModule`)

**E. Limpeza**
21. Limpar `core/analytics/AnalyticsTracker.kt` — remover métodos Bible (`trackVerseRead`, etc.)
22. Limpar `cedarDS/CedarTheme.kt` — remover `bodyVerse` serif leftover

**Aceite:** app abre no Android e iOS, autentica, cai numa Home vazia com bottom bar funcional; emulador Firestore roda testes de regras; `functions` compila sem erros.

---

### Fase 1 — `products/games`: leitura de partidas

**Objetivo:** o jogador vê e busca jogos perto dele.

**Substitui:** `routes/index.tsx` (Home com mapa/pins) + `routes/buscar.tsx` (busca textual)

**A. Domain**
1. `domain/model/`: `Match`, `Sport` (10 modalidades), `MatchStatus`, `MatchLevel`, `MatchFilter`, `Participant`
2. `domain/error/GamesError.kt`: sealed class (`Network`, `NotFound`, `PermissionDenied`, `Unknown`) + message resolvers
3. `domain/repository/MatchRepository.kt`: interfaces com `Flow<List<Match>>` + `suspend fun refresh(): Result<Unit>`
4. `domain/usecase/`: `GetNearbyMatchesUseCase`, `SearchMatchesUseCase`, `ObserveMatchUseCase`

**B. Data**
5. `data/remote/MatchSource.kt`: interface + `expect fun createMatchSource()`
6. `data/remote/FirestoreMatchSource.kt` (android/ios actuals): query por geohash, filtro por esporte/status
7. `data/local/`: `GamesDatabase`, `MatchEntity`, `MatchDao` (observeNearby, upsertAll, deleteExpired)
8. `data/prefs/GamesPreferences.kt`: DataStore para filtros (esporte, raio, só-com-vaga), última localização, lastSyncAt
9. `data/mapper/`: `MatchDto ↔ MatchEntity ↔ Match` (3 camadas)
10. `data/repository/MatchRepositoryImpl.kt`: offline-first com `withRetry`
11. `data/di/GamesDataModule.kt`

**C. UI**
12. `ui/home/HomeStep.kt` + `HomeStepModel` (StateScreenModel) + `HomeState` + `HomeEvents`
    - Chips de esporte, slider de raio, botão localização, lista de `MatchCard` ordenados por distância
    - Pull-to-refresh, `isRefreshing` sobre conteúdo
13. `ui/search/SearchStep.kt` + `SearchStepModel` + `SearchState` + `SearchEvents`
    - Busca textual local (sobre o cache), filtros
14. `ui/di/GamesUiModule.kt`: factories dos StepModels + bind `GamesDestination`

**D. CedarDS**
15. `cedarDS/MatchCard.kt` — card com esporte, local, data, vagas, nível
16. `cedarDS/SportChip.kt` — chip de filtro por esporte
17. `cedarDS/SlotBadge.kt` — badge "X vagas" / "Lista de espera"
18. `cedarDS/MatchBottomBar.kt` — bottom navigation de 5 abas
19. `cedarDS/EmptyState.kt` — estado vazio

**E. Strings & Tests**
20. `strings/GamesStrings.kt` + `EnGamesStrings.kt` + `PtBrGamesStrings.kt` + `GamesStringsHolder.kt`
21. Tests: `HomeStepModelTest`, `MatchRepositoryImplTest`, `GeoHashTest`, `distanceKmTest`
22. Screenshot tests: `MatchCard`, `HomeStep`

**Reescrever:** O `GameListStep`/`GameListStepModel`/`InMemoryGameSource` atuais são scaffolding — viram o novo `HomeStep`/`HomeStepModel`. O `InMemoryGameSource` vira fake de teste.

**Aceite:** partidas reais do Firestore aparecem na Home ordenadas por distância; filtros persistem; app offline mostra cache; testes verdes.

---

### Fase 2 — `products/player`: perfil e disponibilidade

**Objetivo:** o jogador é encontrável. Alimenta o matchmaking.

**Substitui:** `routes/perfil.tsx` (perfil + toggle de disponibilidade)

**A. Domain**
1. `domain/model/`: `PlayerProfile` (público) + `PlayerPrivateData` (telefone, Pix, geo, disponibilidade)
2. `domain/error/PlayerError.kt`
3. `domain/repository/PlayerRepository.kt`
4. `domain/usecase/`: `GetProfileUseCase`, `UpdateProfileUseCase`, `ToggleAvailabilityUseCase`

**B. Data**
5. `data/remote/PlayerSource.kt` — leitura/escrita nos dois documentos (§5.1)
6. `data/local/` — DataStore para perfil + disponibilidade
7. `data/repository/PlayerRepositoryImpl.kt`
8. `data/di/PlayerDataModule.kt`

**C. UI**
9. `ui/profile/PlayerProfileStep.kt` + `PlayerProfileStepModel` + State + Events
    - Nome, apelido, foto (Coil), posição, nível, esportes, cidade, Pix key, telefone
10. `ui/availability/AvailabilityStep.kt` + `AvailabilityStepModel`
    - "🟢 ESTOU DISPONÍVEL" — esportes, janela de horário, raio, expiração
11. `ui/di/PlayerUiModule.kt`

**D. API público**
12. `api/PlayerProfileHolder.kt`: `Flow<PlayerProfile?>` + localização corrente (para `games`)

**E. Strings & Tests**
13. `strings/PlayerStrings.kt` + traduções
14. Tests: `PlayerProfileStepModelTest`, `PlayerRepositoryImplTest`

**Aceite:** perfil salva no Firestore com campos privados isolados; toggle grava geohash + availableUntil; regras negam leitura de `private/data` de terceiros.

---

### Fase 3 — `products/games`: escrita e participação

**Objetivo:** loop completo do produto.

**Substitui:** `routes/criar.tsx` + `routes/partida.$id.tsx` + `routes/meus-jogos.tsx`

**A. UI**
1. `ui/create/CreateMatchStep.kt` + `CreateMatchStepModel`
    - Formulário completo (esporte, local, data, vagas 2-40, valor, nível, regras, Pix key)
    - Pré-preenche Pix key e coords do perfil
    - Taxa 7% preview ("cobrança direto no seu Pix")
    - Auto-join do organizador como confirmed+paid
2. `ui/detail/MatchDetailStep.kt` + `MatchDetailStepModel`
    - Cabeçalho, participantes confirmados, waitlist, regras, WhatsApp organizador
    - Botões: Entrar / Sair / Denunciar / Pix (se preço > 0 e não pagou)
    - Realtime de participantes via `snapshots()`
3. `ui/mymatches/MyMatchesStep.kt` + `MyMatchesStepModel`
    - Abas Jogador (participações) e Organizador (partidas criadas + arrecadação)
    - Requer login (CTA para auth se não autenticado)

**B. Cloud Functions**
4. `joinMatch` callable — transação: valida ban, lotação, duplicidade; decided confirmed vs waitlist; atualiza contadores
5. `leaveMatch` callable — transação: libera vaga, promove fila, notifica
6. Realtime: `snapshots()` na partida e subcoleção participantes

**C. Integração**
7. Limite de partidas ativas por plano (free = 1 ativa), lendo `ProStateHolder` do `identity`

**Aceite:** dois dispositivos na última vaga — um confirma, outro vai para waitlist, sem overbooking; ao sair, primeiro da fila é promovido e notificado.

---

### Fase 4 — `products/notifications`

**Objetivo:** o sistema procura jogadores.

**Substitui:** `NotificationBell.tsx` + `WhatsAppBroadcast.tsx` + triggers

**A. FCM**
1. `expect`/`actual` para token e permissão (Android 13+ / iOS)
2. Registrar token em `users/{uid}/devices/{token}`

**B. Functions**
3. `onMatchCreated` — busca geohash + notifica + push (deduplicação, teto por dia)
4. `onParticipantChanged` — notifica vaga / promove fila

**C. UI**
5. `ui/feed/NotificationFeedStep.kt` — lista, marcar lida, deep link para partida
6. WhatsApp broadcast: callable `nearbyAvailablePlayers` + tela de seleção + link `wa.me`

**D. Deep Links**
7. `match://partida/{id}` (Android intent filter, iOS Universal Links)

**Aceite:** criar partida em um dispositivo faz o segundo receber push em segundos; tocar no push abre a partida.

---

### Fase 5 — Pagamentos e assinatura

**Objetivo:** monetização.

**Substitui:** `PixDialog.tsx` + tela de planos

| O quê | Como |
|---|---|
| Assinatura organizador (free/pro/business) | **RevenueCat** — já integrado no `identity` |
| Rateio da partida (jogador → organizador) | **Pix P2P** — pagamento direto, sem gateway |
| Taxa da plataforma | Desligada no MVP |

1. `PixDialog` no cedarDS + QR gerado **localmente** (nunca API de terceiro)
2. Callables `registerPixPayment` / `confirmPixPayment`
3. Ajustar `PaywallStep` do `identity` para planos do Match

**Aceite:** organizador assina pelo paywall; jogador copia Pix; organizador confirma; status vira `paid` em tempo real.

---

### Fase 6 — Confiança e segurança

1. Avaliações pós-partida (`submitRating` com transação)
2. Denúncias com 10 motivos (`submitReport`)
3. Moderação: advertência → suspensão → banimento
4. Verificação e-mail + telefone (SMS)
5. LGPD: exclusão estendida, exportação, minimização de dados

---

### Fase 7 — Mapa e polimento

1. `cedarDS/map/CedarMap.kt`: `expect`/`actual` — Google Maps (Android) / MKMapView (iOS)
2. Tema Cedar: identidade esportiva (remover paleta Lexis/Bible)
3. Ícones Android/iOS (remover do Lexis)
4. Analytics de funil (atualizar `AnalyticsTracker`)
5. Painel admin: fora do app (web ou Firebase Console)

---

## 8. Ordem de execução

```
Fase 0 ── Fase 1 ──┬── Fase 3 ──┬── Fase 5
                   │            │
        Fase 2 ────┴── Fase 4 ──┘
                                └── Fase 6 ── Fase 7
```

Fases 1 e 2 podem correr em paralelo após contratos do navigator e módulo firestore. Fase 3 depende das duas. Fase 7 não bloqueia nenhuma outra.

---

## 9. Riscos

| # | Risco | Impacto | Mitigação |
|---|---|---|---|
| R1 | `functions/` está stale (código Bible) | Nenhum callable funciona | Reescrever na Fase 0, antes de qualquer feature |
| R2 | Busca por raio no Firestore | Sem isso não existe matchmaking | Geohash em `core/geo` com testes desde Fase 0 |
| R3 | Overbooking na última vaga | Quebra de confiança | `joinMatch` callable com transação; regras negam escrita direta |
| R4 | Mapa em CMP | Sem solução comum madura | `expect`/`actual` isolado; Fase 7, nenhuma depende |
| R5 | Assinatura por Pix nas lojas | App rejeitado | RevenueCat (Fase 5) |
| R6 | Room KMP é primeira adoção | Atrito de build | Spike na Fase 0 |
| R7 | Custo Firestore com notificações | Conta cresce rápido | Raio máximo, limite de destinatários, batch |
| R8 | Múltiplos Firebase pods no Xcode | Erros de linkagem | Módulo `firestore/` único |
| R9 | QR Pix via API de terceiro | Vazamento de chave | Gerar QR no dispositivo |
| R10 | `AnalyticsTracker` com métodos Bible | Confusão, erros | Limpar na Fase 0 |

---

## 10. Testes

| Nível | Ferramenta | Cobertura mínima |
|---|---|---|
| Unidade — `domain` | `kotlin-test` + `kotlinx-coroutines-test` | 100% dos use cases |
| Unidade — `StepModel` | Turbine (estado + efeitos) | todos os StepModels |
| Unidade — `data` | fakes em `commonTest` | repositórios, mappers, cache |
| Puro | `kotlin-test` | `GeoHash`, `distanceKm`, `PixPayloadBuilder`, `formatWhen` |
| Screenshot | Paparazzi | `MatchCard`, `HomeStep`, `MatchDetailStep`, `CreateMatchStep` |
| Regras | `@firebase/rules-unit-testing` | cada coleção: dono, terceiro, anônimo, admin |
| Functions | Vitest + emulador | `joinMatch` concorrente, promoção de fila, geo query |

---

## 11. Dependências a acrescentar

| Dependência | Para quê | Fase |
|---|---|---|
| `kotlinx-datetime` | `startsAt`, `availableUntil`, `formatWhen` | 0 |
| `firebase-messaging` | push (FCM) no Android | 4 |
| `maps-compose` + `play-services-location` | mapa e GPS no Android | 0/7 |
| `firebase-appcheck` | App Check | 0 |

Pods iOS: `FirebaseFirestore` (módulo `firestore/`), `FirebaseMessaging`.
CoreLocation e MapKit são de sistema — sem pod.

---

## 12. Checklist pré-implementation

- [ ] Firestore habilitado nos projetos Firebase (produção + dev)
- [ ] `google-services.json` e `GoogleService-Info.plist` presentes ✓
- [ ] App Check ativado (Play Integrity / DeviceCheck)
- [ ] `firebase deploy --only firestore:rules,firestore:indexes` funcionando
- [ ] Emulador suite rodando (auth, firestore, functions)
- [ ] Chave Google Maps (Android) e MapKit habilitado (iOS)
- [ ] Keystore de release do Android gerado
