# Join Play — app esportivo de vagas

Marketplace que conecta **vagas em partidas** a **jogadores disponíveis**.
A partida já existe e a quadra já está reservada; o app resolve o espaço vazio.

> TEM UMA VAGA. TEM ALGUÉM QUERENDO JOGAR. A GENTE FAZ O JOIN PLAY.

Kotlin Multiplatform + Compose Multiplatform, rodando em Android e iOS a partir
de um único codebase. Derivado da base do projeto Lexis.

## Módulos

```
app/                  — Entry point Android/iOS, Koin, framework exportado ao Xcode
core/                 — Infra KMP: DI, analytics, crash reporting, dispatchers, retry
navigator/            — Contratos de navegação entre produtos
cedarDS/              — Design System compartilhado
products/identity/    — Autenticação, perfil, paywall e assinatura (RevenueCat)
products/games/       — Partidas, vagas e match  ← feature nova
iosApp/               — Shell SwiftUI + CocoaPods
functions/            — Cloud Functions (exclusão de conta)
```

## Arquitetura

- `data/ → domain/ → ui/`
- `Step` implementa `Screen` do Voyager
- `StepModel` usa `StateFlow` para estado e `Channel` para side-effects
- `State` usa `ImmutableList` / `ImmutableMap`, campos sempre `val`
- `domain/` e `data/` são `internal` por padrão
- Repositórios retornam `Result<T>`
- `koinScreenModel()` apenas dentro de um `Screen` do Voyager

## Antes do primeiro build

1. **Firebase** — criar projeto, registrar `com.walcker.match.app` e
   `com.walcker.match.app.dev`, colocar `google-services.json` em `app/` e
   `GoogleService-Info.plist` em `iosApp/iosApp/`
2. **RevenueCat** — projeto novo; `REVENUECAT_ANDROID_KEY` em `local.properties`
   e a chave iOS em `iosApp/Secrets.xcconfig`
3. **Keystore** — gerar novo para release Android
4. **Ícones e paleta** — `app/src/androidMain/res/mipmap-*`,
   `iosApp/iosApp/Assets.xcassets` e `cedarDS/.../CedarTheme.kt` ainda são do Lexis

## Build

```bash
./gradlew :app:installDebug          # Android
cd iosApp && pod install             # iOS
```

## MVP

Cadastro · localização · esportes · disponibilidade · criar vaga · encontrar vaga ·
entrar no jogo · confirmação · notificações · WhatsApp · pagamento/assinatura ·
mecanismos básicos de segurança e denúncias.
