# Plano de Migração — `score-link-go` (Lovable/Supabase) → `mobile-match` (KMP + CMP + Firebase)

**Autor:** Vitor Walcker · **Data:** 12/08/2026 · **Status:** proposta para aprovação

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
| **Estado** | `useState` + `useEffect` por rota | `StateFlow` (estado) + `Channel` (efeitos) |

O `score-link-go` é um protótipo funcional gerado no Lovable: prova o fluxo de negócio e traz o
modelo de dados já pensado, mas mistura camadas (query do Supabase dentro do componente de tela)
e depende de infraestrutura que não existe no destino. **Nada de código será portado literalmente
— o que migra é a regra de negócio e o modelo de dados.**

### Decisões desta migração (aprovadas)

| # | Decisão | Escolha |
|---|---|---|
| D1 | Acesso ao Firestore no KMP | `expect`/`actual` com SDK nativo (mesmo padrão do `products/identity`) |
| D2 | Organização de módulos | `products/games` + `products/player` + `products/notifications` |
| D3 | Cache local | Room KMP (fonte da verdade) + DataStore (preferências) |
| D4 | Escopo da fase 1 | Paridade total com o Lovable |

---

## 2. Inventário: o que existe hoje na origem

### 2.1 Rotas (React) → destino

| Rota origem | O que faz | Módulo destino | Tela destino |
|---|---|---|---|
| `routes/index.tsx` | Home: jogos perto, filtro por esporte, raio, toggle "estou disponível", geolocalização, realtime | `products/games` | `HomeStep` |
| `routes/buscar.tsx` | Busca textual + filtros (esporte, raio, só com vaga) | `products/games` | `SearchStep` |
| `routes/criar.tsx` | Formulário de criação de partida + marcação no mapa | `products/games` | `CreateMatchStep` |
| `routes/partida.$id.tsx` | Detalhe: entrar, sair, lista de espera, pagar Pix, denunciar, realtime | `products/games` | `MatchDetailStep` |
| `routes/meus-jogos.tsx` | Abas jogador/organizador | `products/games` | `MyMatchesStep` |
| `routes/perfil.tsx` | Perfil, chave Pix, telefone, posição, planos | `products/player` | `PlayerProfileStep` |
| `routes/auth.tsx` | Login/cadastro e-mail + Google | — | **já existe** em `products/identity` |
| `routes/__root.tsx` | Shell + BottomNav + sino de notificação | `app` | `MatchScaffold` (novo) |

### 2.2 Componentes e libs → destino

| Origem | Destino |
|---|---|
| `components/MatchCard.tsx` | `cedarDS/MatchCard.kt` |
| `components/BottomNav.tsx` | `cedarDS/MatchBottomBar.kt` |
| `components/NotificationBell.tsx` | `products/notifications` (`NotificationBell` no cedarDS, lógica no módulo) |
| `components/MapView.tsx` (Leaflet) | `cedarDS/map/CedarMap.kt` — `expect`/`actual` (Google Maps / MapKit) |
| `components/PixDialog.tsx` | `cedarDS/PixDialog.kt` + `core/payments/PixPayloadBuilder.kt` |
| `components/WhatsAppBroadcast.tsx` | `products/notifications` + Callable Function |
| `lib/pix.ts` (BR Code EMV + CRC16) | `core/payments/PixPayloadBuilder.kt` — **Kotlin puro, portável 1:1** |
| `lib/geo.ts` (haversine, formatação) | `core/geo/Geo.kt` + `core/datetime/MatchDateFormatter.kt` |
| `lib/share.ts` (link WhatsApp) | `core/share/WhatsAppLink.kt` + `expect fun openUrl` |
| `lib/db.ts` (tipos, SPORTS, PLANS) | `products/games/domain/model` + `products/player/domain/model` |
| `lib/broadcast.functions.ts` (server fn) | Cloud Function callable `nearbyAvailablePlayers` |
| `hooks/useAuth.ts` | **já existe**: `SessionHolder` / `AuthRepository` no `identity` |

### 2.3 Regra de negócio embutida no banco (o mais importante)

Estas quatro funções Postgres são o núcleo do produto e **precisam virar Cloud Functions**:

| Função Postgres | Comportamento | Destino |
|---|---|---|
| `handle_new_user()` | Ao criar usuário: cria profile, role `user` e subscription `free` | Function `onUserCreate` (Auth trigger) |
| `notify_nearby_players()` | Ao criar partida: notifica jogadores no raio | Function `onMatchCreated` (Firestore trigger) |
| `notify_vacancy()` | Ao liberar vaga: notifica disponíveis próximos, se não há fila | Function `onParticipantChanged` |
| `promote_waitlist()` | Ao cancelar: promove o primeiro da lista de espera | Function `onParticipantChanged` (transação) |
| `distance_km()` | Haversine em SQL | `core/geo` (cliente) + `functions/src/geo.ts` (servidor) |
| `has_role()` | Checagem de papel | **Custom Claims** do Firebase Auth |

---

## 3. Arquitetura de destino

### 3.1 Mapa de módulos

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
functions/                  ⊕ triggers, callables, moderação                 [existe, vazio]
```

> **Por que um módulo `firestore/` separado:** com D1 (SDK nativo por `expect`/`actual`), cada
> módulo que falasse com o Firestore precisaria do seu próprio bloco `cocoapods { pod("FirebaseFirestore") }`
> e do seu próprio framework exportado para o Xcode. Três frameworks embutindo o mesmo SDK é
> duplicação e dor de linkagem. Um módulo `firestore/` declara o pod uma vez e expõe uma API
> comum enxuta — `document`, `collection`, `query`, `snapshots`, `runTransaction`, `callable` —
> que os três produtos consomem sem tocar no SDK.

### 3.2 Camadas dentro de cada produto (padrão já vigente no repo)

```
products/games/src/commonMain/kotlin/com/walcker/games/
├── GamesDestinationImpl.kt          implementa o contrato do navigator
├── di/GamesModule.kt                agrega os submódulos Koin
└── features/
    ├── domain/                      internal — sem dependência de framework
    │   ├── model/                   Match, Sport, MatchStatus, Participant, MatchFilter
    │   ├── repository/              interfaces, retornam Result<T>
    │   └── usecase/                 interface + Impl, operator fun invoke()
    ├── data/                        internal
    │   ├── remote/                  MatchSource (interface) + expect fun create…
    │   ├── local/                   Room: MatchEntity, MatchDao, GamesDatabase
    │   ├── prefs/                   DataStore: filtros e última localização
    │   ├── mapper/                  Dto ↔ Entity ↔ Model
    │   ├── repository/              …RepositoryImpl
    │   └── di/GamesDataModule.kt
    └── ui/                          internal
        ├── home/                    HomeStep, HomeStepModel, HomeState, HomeEvents
        ├── search/  create/  detail/  mymatches/
        └── di/GamesUiModule.kt
```

**Regras (do `README.md` do repo, mantidas):**

- Fluxo `data/ → domain/ → ui/`; `domain` não conhece `data`.
- `Step` implementa `Screen` do Voyager; `StepModel` estende `ScreenModel`.
- `StepModel`: `StateFlow` para estado, `Channel` para side-effects.
- `State` com campos `val` e `ImmutableList` / `ImmutableMap`.
- `domain/` e `data/` são `internal` por padrão; só `api/` e o `Destination` são públicos.
- Repositórios retornam `Result<T>`; exceções do SDK são mapeadas para um `sealed class …Error`
  antes de cruzar a fronteira do `data` (padrão de `IdentityError`).
- `koinScreenModel()` apenas dentro de um `Screen`.
- Strings via Lyricist (`GamesStrings`, `PtBrGamesStrings`, `EnGamesStrings`) — **nada hardcoded**,
  ao contrário do `GameListStep` atual.

### 3.3 Contratos no `navigator`

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

## 4. Modelo de dados no Firestore

### 4.1 A diferença que mais dói: RLS por coluna não existe

No Postgres, `profiles` tem `profiles_public_read` (leitura pública da linha inteira) e ao mesmo
tempo guarda `phone`, `pix_key`, `lat`, `lng`. No Firestore a permissão é **por documento**, então
os campos sensíveis precisam sair para um subdocumento privado. Sem essa separação, expor o
perfil público vaza telefone e chave Pix de todos os usuários.

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

### 4.2 Coleções

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

### 4.3 Busca por raio: geohash é obrigatório

O Firestore não faz consulta por distância. O padrão é o do GeoFire:

1. Gravar `geohash` (precisão 9) junto com `lat`/`lng` em `matches` e em `profiles/{uid}/private/data`.
2. Para um raio *R*, calcular os **intervalos de prefixo de geohash** que cobrem a caixa delimitadora.
3. Disparar uma query `orderBy(geohash).startAt(x).endAt(y)` por intervalo (tipicamente 4 a 9).
4. Filtrar o excesso no cliente com haversine — a caixa é maior que o círculo.

Implementar `core/geo/GeoHash.kt` em Kotlin puro (`encode`, `boundsForRadius`, `distanceKm`),
com testes em `commonTest` cobrindo os mesmos casos da função `distance_km` do Postgres. A mesma
lógica é replicada em TypeScript nas Functions (`functions/src/geo.ts`) — os dois lados precisam
concordar, então os testes usam vetores compartilhados.

**Índices necessários** (`firestore.indexes.json`):

- `matches`: `status` ASC + `geohash` ASC
- `matches`: `sport` ASC + `status` ASC + `startsAt` ASC
- `matches`: `organizerId` ASC + `startsAt` DESC
- collection group `participants`: `userId` ASC + `status` ASC

### 4.4 `firestore.rules` — atenção

**As regras atuais no repositório são do projeto Lexis** (`book_explanations`, `verse_explanations`,
`verse_comments`). Elas não têm nada a ver com o Match e vão negar 100% dos acessos novos.
Reescrever integralmente é item da Fase 0, não um ajuste posterior.

Princípios das novas regras:

- Escrita em `participants`, `confirmedCount`, `payments` e `subscription`: **negada ao cliente**.
  Só Cloud Functions escrevem. Isso é o que impede overbooking e fraude de status de pagamento.
- `profiles/{uid}/private/**`: `request.auth.uid == uid`.
- Admin via `request.auth.token.admin == true` (Custom Claim), substituindo `has_role()`.
- `matches`: criar exige `organizerId == request.auth.uid` e valida `totalSlots` entre 2 e 40,
  `startsAt` no futuro, `priceCents >= 0`.
- Usuário banido (`isBanned`) não cria partida nem entra em jogo — validado na Function.

### 4.5 Cloud Functions

| Função | Tipo | Substitui |
|---|---|---|
| `onUserCreate` | Auth trigger | `handle_new_user()` — cria `profiles/{uid}`, `private/data`, `subscription/current` free, claim `role: user` |
| `joinMatch` | **Callable** | `INSERT match_participants` — transação: valida banimento, lotação, duplicidade; decide `confirmed` vs `waitlist`; atualiza contadores |
| `leaveMatch` | **Callable** | `UPDATE status='cancelled'` — transação: libera vaga, promove o primeiro da fila (`promote_waitlist`), notifica |
| `onMatchCreated` | Firestore trigger | `notify_nearby_players()` — busca por geohash, grava notificações, dispara FCM |
| `onParticipantChanged` | Firestore trigger | `notify_vacancy()` — notifica disponíveis próximos quando abre vaga e não há fila |
| `nearbyAvailablePlayers` | Callable | `lib/broadcast.functions.ts` — lista jogadores com telefone para o organizador (só o dono da partida) |
| `registerPixPayment` | Callable | `INSERT payments` — grava intenção de pagamento e o payload EMV |
| `confirmPixPayment` | Callable | organizador marca `paid`; notifica o jogador |
| `submitRating` | Callable | grava rating e recalcula `rating`/`ratingCount` do avaliado em transação |
| `submitReport` | Callable | grava denúncia e escalona para `moderation` |
| `revenuecatWebhook` | HTTPS | espelha assinatura em `subscription/current` e atualiza claim `plan` |
| `finishMatches` | Scheduled (hora em hora) | `status = finished` após `startsAt + durationMin`, abre janela de avaliação |
| `deleteAccount` | Callable | **já existe** no `identity` — estender para limpar partidas e participações |

> **Por que `joinMatch` é callable e não escrita direta com transação no cliente:** o
> `total_slots` no Postgres era protegido por trigger no servidor. No Firestore, uma transação no
> cliente até resolve concorrência, mas depende de regras que precisariam liberar escrita em
> `participants` — e aí qualquer cliente adulterado entra em jogo lotado, se autoconfirma como
> pago ou pula a fila. A callable mantém a decisão no servidor, como estava.

---

## 5. Cache local: Room + DataStore

### 5.1 Divisão de responsabilidades

| Camada | Guarda | Onde |
|---|---|---|
| **Room** | partidas, participantes, notificações — dados de domínio consultáveis | `products/*/data/local` |
| **DataStore** | filtros (esporte, raio, só-com-vaga), última localização conhecida, flag de disponibilidade, timestamps de sincronização | `products/*/data/prefs` |
| **Memória** | nada persistente — só o `StateFlow` do `StepModel` | `ui` |

### 5.2 Padrão do repositório (offline-first)

```kotlin
internal class MatchRepositoryImpl(
    private val remote: MatchSource,
    private val dao: MatchDao,
    private val prefs: GamesPreferences,
    private val dispatcher: Dispatcher,
) : MatchRepository {

    // A UI observa SEMPRE o banco local — nunca a rede diretamente.
    override fun nearbyMatches(filter: MatchFilter): Flow<List<Match>> =
        dao.observeNearby(filter.toQuery()).map { it.map(MatchEntity::toDomain) }

    // O refresh é explícito e reporta falha sem derrubar a tela.
    override suspend fun refreshNearby(filter: MatchFilter): Result<Unit> =
        withContext(dispatcher.io) {
            withRetry {                                   // já existe em core/network
                remote.nearbyMatches(filter)
            }.map { dtos ->
                dao.upsertAll(dtos.map(MatchDto::toEntity))
                prefs.setLastSync(Clock.System.now())
            }
        }
}
```

Consequências desejadas: abrir o app offline mostra a última lista de partidas; a tela nunca fica
em branco esperando rede; o `StepModel` some com o estado `isLoading` bloqueante e passa a usar
`isRefreshing` sobre conteúdo já visível.

### 5.3 Configuração do Room KMP

`androidx.room` 2.7.1 e `sqlite-bundled` 2.5.1 **já estão no `libs.versions.toml` e ainda não são
usados** — é a primeira adoção no projeto. Cada produto com Room precisa de:

- plugins `ksp` e `room-plugin` no `build.gradle.kts` do módulo;
- `ksp(libs.room.compiler)` para `kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`;
- `expect fun createDatabaseBuilder(): RoomDatabase.Builder<GamesDatabase>` com actual em Android
  (`context.getDatabasePath`) e iOS (`NSDocumentDirectory`);
- `.setDriver(BundledSQLiteDriver())`.

Invalidação: `refreshNearby` grava `lastSyncAt` no DataStore; a Home dispara refresh se passaram
mais de 60 s ou se a localização mudou mais de 500 m. Partidas com `startsAt` no passado são
apagadas do cache no boot.

---

## 6. Plano faseado

Cada fase termina compilando nas duas plataformas, com testes verdes e `detekt` limpo. As fases 2
a 4 podem ser paralelizadas por módulo depois que a Fase 1 estabilizar os contratos.

---

### Fase 0 — Fundação (bloqueia tudo)

**Objetivo:** infraestrutura pronta para qualquer feature ser escrita.

1. **Firebase**: habilitar Firestore no projeto; `google-services.json` e `GoogleService-Info.plist`
   nos dois flavors (`com.walcker.match.app` e `.dev`); ativar App Check.
2. **`firestore.rules`**: reescrever do zero conforme §4.4 — remover as regras do Lexis. Escrever
   testes de regras com `@firebase/rules-unit-testing` no emulador.
3. **`firestore.indexes.json`**: índices de §4.3; registrar no `firebase.json`.
4. **Módulo `firestore/`**: novo módulo KMP com bloco `cocoapods { pod("FirebaseFirestore") }`,
   framework `FirestoreKit`, e API comum (`FirestoreClient`, `FirestoreQuery`, `snapshots()`,
   `runTransaction`, `callable`), com `actual` Android e iOS.
5. **`core/geo`**: `GeoHash.encode/boundsForRadius`, `distanceKm`, `formatDistance`. Testes contra
   os vetores da função `distance_km` do Postgres.
6. **`core/datetime`**: `formatWhen` ("Hoje · 20:00") sobre `kotlinx-datetime`. Substitui o
   `startsAt: String` do `Game` atual.
7. **`core/payments`**: `PixPayloadBuilder` (TLV + CRC16, porte direto de `lib/pix.ts`) e
   `formatBRLCents`. Testes com payloads conhecidos.
8. **`core/location`**: `expect`/`actual` para permissão e posição (FusedLocation / CoreLocation).
9. **Shell de navegação no `app`**: `App.kt` hoje renderiza um `if/else` vazio. Montar
   `Navigator` do Voyager, `MatchScaffold` com bottom bar de 5 abas e o gate de autenticação
   (`SessionHolder.isAuthenticated` → `IdentityDestination.login()` ou `GamesDestination.home()`).
10. **`AppModule`**: registrar `gamesModule`, `playerModule`, `notificationsModule` (o `AppModule`
    atual só carrega `coreModules + identityModule`).
11. **`functions/`**: bootstrap TypeScript, ESLint, emulador; `onUserCreate` e `functions/src/geo.ts`.

**Aceite:** app abre no Android e no iOS, autentica com a conta existente, cai numa Home vazia com
bottom bar funcional; emulador do Firestore roda os testes de regras.

---

### Fase 1 — `products/games`: leitura de partidas

**Objetivo:** o jogador vê e busca jogos perto dele. Substitui `routes/index.tsx` e `routes/buscar.tsx`.

- `domain/model`: `Match`, `Sport` (10 modalidades, ampliando as 4 do Lovable), `MatchStatus`,
  `MatchLevel`, `MatchFilter`.
- `domain/repository/MatchRepository` + `usecase`: `GetNearbyMatchesUseCase`,
  `SearchMatchesUseCase`, `ObserveMatchUseCase`.
- `data/remote/MatchSource`: `expect`/`actual` sobre o módulo `firestore/` — query por intervalos
  de geohash, filtro fino no cliente.
- `data/local`: `GamesDatabase`, `MatchEntity`, `MatchDao` (com `observeNearby`).
- `data/prefs/GamesPreferences`: esporte, raio, só-com-vaga, última localização.
- `ui/home`: `HomeStep`, `HomeStepModel`, `HomeState`, `HomeEvents` — saudação, chips de esporte,
  slider de raio, botão "usar minha localização", lista de `MatchCard`.
- `ui/search`: `SearchStep` com busca textual (local, sobre o cache) e filtros.
- `cedarDS`: `MatchCard`, `SportChip`, `SlotBadge`, `MatchBottomBar`, `EmptyState`.
- Strings via Lyricist: `GamesStrings` + `PtBrGamesStrings` + `EnGamesStrings`.
- **Reescrever** `products/games` atual: `InMemoryGameSource`, `Game`, `GameListStep` e
  `GameListStepModel` são scaffolding do bootstrap — viram o novo `Match`/`HomeStep`. O
  `InMemoryGameSource` é preservado como **fake de teste** em `commonTest`.

**Aceite:** partidas reais do Firestore aparecem na Home ordenadas por distância; filtros
persistem entre sessões; app aberto em modo avião mostra o cache; testes de `HomeStepModel` e
`MatchRepositoryImpl` verdes; screenshot tests de `MatchCard` (padrão `identity/screenshotTests`).

---

### Fase 2 — `products/player`: perfil e disponibilidade

**Objetivo:** o jogador é encontrável. Substitui `routes/perfil.tsx` e o toggle de disponibilidade.
É o que alimenta o matchmaking — sem isso as notificações da Fase 4 não têm para quem ir.

- `domain/model`: `PlayerProfile` (público) e `PlayerPrivateData` (telefone, Pix, geo, disponibilidade).
- `data/remote/ProfileSource`: leitura/escrita nos dois documentos de §4.1.
- `data/local`: DataStore para o perfil do usuário corrente + espelho da disponibilidade.
- `ui/profile`: nome, apelido, foto (Coil), posição, nível, esportes, cidade, chave Pix, telefone.
- `ui/availability`: "🟢 ESTOU DISPONÍVEL" — esportes, janela de horário, raio, expiração
  (`availableUntil`).
- `api/PlayerProfileHolder`: expõe `Flow<PlayerProfile?>` e a localização corrente para o `games`
  sem acoplar os módulos.
- Onboarding pós-cadastro: pedir localização, esportes e telefone (é aqui que a base de
  matchmaking se forma).

**Aceite:** perfil salva e reflete no Firestore com os campos privados isolados; toggle de
disponibilidade grava `geohash` e `availableUntil`; regras negam leitura do `private/data` de
terceiros (teste de regras).

---

### Fase 3 — `products/games`: escrita e participação

**Objetivo:** o loop completo do produto. Substitui `routes/criar.tsx`, `routes/partida.$id.tsx`
e `routes/meus-jogos.tsx`.

- `ui/create`: formulário (esporte, título, local, endereço, data/hora, duração, vagas, valor,
  nível, regras, chave Pix) + marcação de ponto no mapa + validação.
- `ui/detail`: cabeçalho da partida, lista de confirmados, lista de espera, mapa, regras, contato
  do organizador (respeitando permissão), botões Entrar / Sair / Denunciar.
- `ui/mymatches`: abas Jogador e Organizador.
- Callables `joinMatch` / `leaveMatch` com transação, promoção de fila e notificação.
- Realtime: `snapshots()` na partida e na subcoleção de participantes (substitui os canais
  `supabase.channel(...)`).
- Limite de partidas ativas por plano (free = 1 ativa), lendo `ProStateHolder` do `identity`.

**Aceite:** dois dispositivos entrando ao mesmo tempo na última vaga — um confirma, o outro vai
para a lista de espera, sem overbooking; ao sair, o primeiro da fila é promovido e notificado.

---

### Fase 4 — `products/notifications`

**Objetivo:** o sistema procura jogadores, não o contrário. Substitui `NotificationBell.tsx`,
`WhatsAppBroadcast.tsx` e os triggers de notificação.

- FCM: `expect`/`actual` para token e permissão (Android 13+ e iOS); registrar token em
  `users/{uid}/devices/{token}`.
- Functions `onMatchCreated` e `onParticipantChanged`: busca por geohash + escrita de notificação
    + push. Deduplicação: não notificar quem já está na partida nem o organizador; teto de
      notificações por usuário por dia.
- `ui/feed`: lista, marcar como lida, deep link para a partida.
- Deep links: `match://partida/{id}` (Android intent filter, iOS Universal Links).
- WhatsApp broadcast: callable `nearbyAvailablePlayers` + tela de seleção + `openUrl` para
  `wa.me` com o texto de `matchShareText`. **Só API oficial ou link `wa.me`** — nada de
  automação não oficial, como já dizia o brief.

**Aceite:** criar partida em um dispositivo faz o segundo (disponível, dentro do raio) receber
push em segundos; tocar no push abre a partida.

---

### Fase 5 — Pagamentos e assinatura

**Objetivo:** dinheiro. Substitui `PixDialog.tsx` e a tela de planos do `perfil.tsx`.

**Ponto de atenção de compliance — muda o desenho:** no Lovable, a assinatura do organizador era
cobrada por Pix. Em app nativo, Apple e Google **exigem** compra in-app para assinatura de
funcionalidade digital; Pix para isso derruba a publicação. Então:

| O quê | Como |
|---|---|
| Assinatura do organizador (free / pro / business) | **RevenueCat** — já integrado no `identity`, com `PaywallStep` e `ProStateHolder` prontos. Planos viram entitlements; `revenuecatWebhook` espelha em `subscription/current`. |
| Rateio da partida entre jogador e organizador | **Pix P2P** — pagamento entre pessoas por bem/serviço do mundo real, fora do escopo de IAP. Mantém o copia-e-cola gerado localmente. |
| Taxa da plataforma sobre a partida | Estrutura pronta no modelo (`platformFeeCents`), **desligada no MVP** — como o brief pedia. |

- `PixDialog` no cedarDS + QR gerado **localmente** (o Lovable usava `api.qrserver.com`; num app
  nativo isso vaza a chave Pix para terceiro — gerar o QR no dispositivo).
- Callables `registerPixPayment` / `confirmPixPayment`; estado do pagamento no card da partida.
- Ajustar o `PaywallStep` do `identity` para os três planos do Match.

**Aceite:** organizador assina pelo paywall e o limite de partidas sobe; jogador copia o Pix,
organizador confirma, status vira `paid` para os dois em tempo real.

---

### Fase 6 — Confiança e segurança

**Objetivo:** o produto sobrevive ao primeiro usuário mal-intencionado.

- Avaliações pós-partida (pontualidade, respeito, fair play, comportamento) com janela aberta pela
  Function `finishMatches`; `submitRating` recalcula a média em transação.
- Denúncias com os 10 motivos do brief; `submitReport`.
- Níveis de moderação: advertência → suspensão → banimento; `isBanned` bloqueia criar e entrar.
- Verificação de e-mail e telefone (SMS via Firebase Auth).
- LGPD: exclusão de conta estendida (a Function já existe no `identity`), exportação de dados,
  minimização — telefone só visível para participantes confirmados da mesma partida.

---

### Fase 7 — Mapa e polimento

- `cedarDS/map/CedarMap.kt`: `expect`/`actual` — Google Maps Compose no Android, `MKMapView` via
  `UIKitView` no iOS. Pins por esporte, clique abre bottom sheet com o resumo da partida.
- Tema Cedar: `CedarTheme.kt` ainda é a paleta do Lexis — trocar por identidade esportiva própria.
- Ícones Android (`mipmap-*`) e iOS (`Assets.xcassets`), ainda do Lexis.
- Analytics de funil (`AnalyticsTracker` já existe): ver partida → entrar → confirmar → jogar.
- Painel administrativo: **fora do app** — web separado ou Firebase Console + Functions. Não
  cabe no binário de loja.

---

## 7. Riscos

| # | Risco | Impacto | Mitigação |
|---|---|---|---|
| R1 | `firestore.rules` do Lexis nega tudo | Bloqueia toda a Fase 1 | Reescrita completa na Fase 0, com testes no emulador |
| R2 | Busca por raio no Firestore | Sem isso não existe "jogos perto de você" | Geohash em `core/geo` com testes desde a Fase 0 |
| R3 | Overbooking na última vaga | Quebra de confiança irreversível | `joinMatch` callable com transação; regras negam escrita direta |
| R4 | Mapa em Compose Multiplatform | Não há solução comum madura | `expect`/`actual` isolado no cedarDS; deixado para a Fase 7 — nenhuma outra fase depende dele |
| R5 | Assinatura por Pix reprovada nas lojas | App rejeitado | RevenueCat para assinatura, decidido na Fase 5 |
| R6 | Room KMP é primeira adoção no projeto | Atrito de build, KSP para iOS | Provar num spike de meio dia na Fase 0, antes de escrever features |
| R7 | Custo do Firestore com notificação por proximidade | Conta cresce rápido com leitura por geohash a cada partida criada | Teto de raio, limite de destinatários por partida, batch nas Functions, monitorar leituras |
| R8 | Múltiplos frameworks Firebase no Xcode | Erros de linkagem no iOS | Módulo `firestore/` único com o pod (§3.1) |
| R9 | QR Pix via API de terceiro (`qrserver.com`) | Vazamento de chave Pix | Gerar o QR no dispositivo |

---

## 8. Testes

| Nível | Ferramenta | Cobertura mínima |
|---|---|---|
| Unidade — `domain` | `kotlin-test` + `kotlinx-coroutines-test` | 100% dos use cases |
| Unidade — `StepModel` | Turbine (estado + efeitos) | todos os `StepModel`, seguindo `LoginStepModelTest` |
| Unidade — `data` | fakes em `commonTest` (padrão `FakeAuthRepository`) | repositórios, mappers, cache |
| Puro | `kotlin-test` | `GeoHash`, `distanceKm`, `PixPayloadBuilder`, `formatWhen` |
| Screenshot | Paparazzi (`products/*/screenshotTests`) | `MatchCard`, `HomeStep`, `MatchDetailStep`, `CreateMatchStep` |
| Regras | `@firebase/rules-unit-testing` no emulador | cada coleção: dono, terceiro, anônimo, admin |
| Functions | Jest + emulador | `joinMatch` concorrente, promoção de fila, geo query |

---

## 9. Ordem de execução

```
Fase 0 ── Fase 1 ──┬── Fase 3 ──┬── Fase 5
                   │            │
        Fase 2 ────┴── Fase 4 ──┘
                                └── Fase 6 ── Fase 7
```

Fases 1 e 2 podem correr em paralelo depois que os contratos do `navigator` e o módulo
`firestore/` estiverem de pé. A Fase 3 depende das duas. A Fase 7 não bloqueia nenhuma outra e
pode ser puxada para frente se o mapa for considerado essencial para a validação no Green Ball.

---

## 10. Dependências a acrescentar no `gradle/libs.versions.toml`

O catálogo já traz Room, `sqlite-bundled`, DataStore, KSP, `room-plugin`, `firebase-firestore`,
Coil, Ktor e RevenueCat — nenhuma delas usada ainda. Faltam:

| Dependência | Para quê | Fase |
|---|---|---|
| `kotlinx-datetime` | `startsAt`, `availableUntil`, `formatWhen` | 0 |
| `firebase-messaging` | push (FCM) no Android | 4 |
| `maps-compose` + `play-services-location` | mapa e GPS no Android | 0 / 7 |
| `qrose` (ou gerador próprio) | QR do Pix desenhado no dispositivo | 5 |
| `firebase-appcheck` | App Check | 0 |

Pods iOS correspondentes: `FirebaseFirestore` (no módulo `firestore/`), `FirebaseMessaging`.
CoreLocation e MapKit são de sistema — sem pod.

---

## 11. Checklist de configuração antes de começar

- [ ] Firestore habilitado nos projetos Firebase de produção e dev
- [ ] `google-services.json` em `app/` e `GoogleService-Info.plist` em `iosApp/iosApp/`
- [ ] App Check ativado (Play Integrity / DeviceCheck)
- [ ] `firebase deploy --only firestore:rules,firestore:indexes` funcionando
- [ ] Emulador suite rodando (`auth`, `firestore`, `functions` — já configurado no `firebase.json`)
- [ ] Chave de API do Google Maps (Android) e MapKit habilitado (iOS)
- [ ] `REVENUECAT_ANDROID_KEY` em `local.properties`, chave iOS em `iosApp/Secrets.xcconfig`
- [ ] Produtos de assinatura criados na Play Console e na App Store Connect
- [ ] Keystore de release do Android gerado