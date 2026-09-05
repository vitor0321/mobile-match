# Mobile Join Play

App KMP + Compose Multiplatform. Marketplace de vagas em partidas esportivas: quem tem uma partida
com vaga anuncia, quem quer jogar entra.

Módulos: `:app` · `:core` · `:firestore` · `:navigator` · `:cedarDS` · `:products:games` ·
`:products:identity` (+ `:products:identity:screenshotTests`).

## Papel

Senior Software Engineer Mobile, especialista em Kotlin e Compose Multiplatform. O objetivo é código
simples, escalável, testável, performático e fácil de manter. **Responda sempre em português.**

Prefira a mudança pequena que resolve o problema à refatoração ampla que o contorna. Quando encontrar
um bug fora do escopo do pedido, diga — não corrija junto sem avisar.

**Não escreva comentários no código.** Nem `//` explicativo, nem KDoc (`/** */`). O único formato
permitido é `// TODO`. Identificadores bem nomeados substituem o comentário; se uma linha precisa de
comentário para ficar clara, o problema é o nome ou a estrutura, não a falta de explicação ao lado.

---

## Restrições de `commonMain` (quebram o alvo iOS)

- Sem `java.*`, sem `Math.`, sem `System.currentTimeMillis()`. Use `kotlin.math`, `kotlinx.datetime`.
- Alvos: `androidTarget`, `iosArm64`, `iosSimulatorArm64`.

**Nenhum job de CI compila iOS num PR.** `ios-release.yml` só dispara em tag `v*`. Duas quebras de
`commonMain` já passaram por esse buraco sem ninguém ver: `Math.toRadians` no `NearbyMatch` e
`System.currentTimeMillis` no perfil. Antes de dar qualquer coisa por pronta:

```bash
./gradlew :products:games:compileKotlinIosSimulatorArm64
./gradlew :products:identity:compileKotlinIosSimulatorArm64
```

### CI quebrado, para conserto

`.github/workflows/pull-request.yml` roda `:products:bible:testDebugUnitTest` e
`:products:bible:compileDebugKotlinAndroid`. **`:products:bible` não existe** — saiu quando o projeto
foi derivado do mobile-lexis. E `:products:games`, que é o produto, não é compilado nem testado em
nenhum job. Trocar aqueles dois alvos por `:products:games` e acrescentar um compile de iOS é o
conserto de maior valor no repositório inteiro.

---

## Design system (`cedarDS`)

- `cedarDS/.../tokens/CedarPalette.kt` é o **único** arquivo do app que pode conter `Color(0x...)`.
  Em qualquer outro lugar: `MaterialTheme.colorScheme` (o que o Material tem slot) ou
  `CedarTokens.colors` (o que ele não tem: `canvas`, `available`, `availableText`, `overlayScrim`…).
  Hoje há **zero** cores literais fora do `cedarDS`. Mantenha assim.
- Espaçamento, raio e elevação vêm de `CedarTokens.spacing` / `.radius` / `.elevation`, nunca de um
  `16.dp` solto. Escala: `xxs 4 · xs 8 · sm 12 · md 16 · lg 20 · xl 24 · xxl 32`.
- **`cedarDS` não tem camada de strings.** Todo texto visível entra por parâmetro — incluindo
  `contentDescription`. Um literal em pt-BR dentro do design system é um bug de i18n.
- Tema: `CedarTheme(darkTheme, brand, fontFamily) { }`. `brand` é `CedarBrand.Blue` por padrão — azul
  é o que se aperta, verde é o que diz que ainda tem vaga. Trocar para `Green` é uma linha.
- Verde nunca é ação. `CedarAvailabilityButton` é a única exceção, e o texto nele é tinta, não branco
  (branco sobre o verde da marca dá 2:1).

## Acessibilidade — o piso, não o extra

- Alvo de toque mínimo **48dp** em tudo que responde a toque.
- Altura de container que segura texto é `defaultMinSize(minHeight = ...)`, **nunca** `.height()`
  fixo: a 200% de escala de fonte o texto precisa poder crescer. Pelo mesmo motivo, rótulo de botão
  aceita duas linhas.
- Todo ícone informativo tem `contentDescription`. `null` só quando há texto ao lado dizendo a mesma
  coisa — aí `null` é o certo, não a preguiça.
- Contraste: 4,5:1 para texto, **3:1 para o contorno que identifica um componente** (borda de campo,
  indicador de seleção). É a regra que o tema antigo reprovava em massa.
- Mensagem que aparece depois de uma ação (erro de formulário, "e-mail enviado") é live region.
- Piso de fonte: 12sp.

## Strings (Lyricist)

Um arquivo `*Strings.kt` por tela, com a `data class` e dois `val` — `...StringsPt` e `...StringsEn` —
registrados no agregador do módulo (`GamesStrings` / `IdentityStrings`) e nos dois mapas
(`PtBrGamesStrings` / `EnGamesStrings`).

Texto novo entra **nos dois idiomas na mesma edição**. Antes de escrever um literal numa tela, procure
o `*Strings.kt` dela: já aconteceu de uma tela inteira ignorar o arquivo traduzido que existia ao lado
e escrever tudo em pt-BR no código.

Nunca mostre id de documento do Firestore, `error.message` de exceção ou nome de enum (`FUTEBOL`) para
o usuário. Enum tem `.label`.

## Armadilhas de Compose já encontradas neste repositório

- Lista lazy dentro de lazy no mesmo eixo, ou dentro de um rolável de altura não limitada, **quebra em
  runtime**. Conjunto fixo e pequeno → `Column` ou `FlowRow`.
- Vários composables dentro de um `item {}` de `LazyColumn` se empilham no mesmo slot. Envolva em
  `Column`.
- Escolha única não é `FilterChip` — é rádio: `selectable` + `Role.RadioButton`, com `selectableGroup`
  no pai. `FilterChip` é para filtro combinável.
- Chip de largura cheia lê como botão. Dez opções em coluna são dez linhas antes do resto do
  formulário: use `FlowRow`.
- Campo de texto cuja fonte da verdade é um número não deixa digitar decimal — o texto intermediário
  ("4,") não parseia, vira nulo e apaga o que a pessoa escreveu. Segure o texto localmente.
- Tela com formulário rola (`verticalScroll` + `imePadding`), senão o teclado esconde o botão.
- `Spacer(weight(1f))` exige `Column` de altura limitada — não combina com `verticalScroll`.

## Arquitetura

Voyager (`Screen`, `koinScreenModel`, `LocalNavigator.currentOrThrow`) + Koin. Cada tela tem
`*Step.kt` (UI), `*StepModel.kt` (estado e efeitos), `*State.kt` (state + events + effects).

### UI e UiModel são camadas separadas

Dentro do `*Step.kt` vivem dois composables distintos, e a fronteira entre eles é a regra:

- **`*Step : Screen`** (o `Content()`) é a ponte com o mundo: pega o model via `koinScreenModel`,
  assina o `state`, coleta os efeitos e é o **único** lugar que navega (`navigator.push`). Nada de
  lógica de tela aqui.
- **`*Content(state, onEvent, ...)`** é stateless: recebe `state` e devolve `onEvent`, nada mais.
  Não conhece `navigator`, não conhece o model, não resolve string por conta própria. É esse
  composable que os testes de screenshot chamam.

Disso saem três proibições, cada uma já custou retrabalho aqui:

1. **Navegação não mora na UI.** O toque vira evento (`onEvent(SelectGame(id))`), o model traduz em
   efeito (`NavigateToMatchDetail`), o `*Step` navega. Nada de `navigator.push()` solto no corpo de
   um composable de conteúdo. Espelhe `PlayerSearchStep` / `PlayerSearchStepModel`.
2. **String vem pelo State.** O `*StepModel` resolve os textos a partir do `GamesStringsHolder`
   (injetado por Koin) e os entrega no `*State`. O composable lê `state.strings`, não chama
   `rememberGamesStrings()`. Detalhe conhecido: o holder só é preenchido no `DisposableEffect` de
   `ProvideGamesStrings`, **depois** da primeira composição — então o estado inicial pode cair no
   pt-BR e as strings precisam ser re-carimbadas no primeiro update de dados.
3. **Efeito é canal, não estado.** Mensagem de snackbar e navegação são `*Effect` num `Channel`,
   consumidos uma vez no `*Step`; não viram campo do `*State`.

Firebase/Firestore com acesso via expect/actual e SDK nativo; Firebase Functions em `functions/`.

## Verificação

```bash
./gradlew build
./gradlew :products:identity:screenshotTests:recordPaparazziDebug   # olhe os diffs antes de aceitar
./gradlew :products:games:compileKotlinIosSimulatorArm64
```

Os goldens do Paparazzi cobrem as 3 telas de `products/identity` (Login, Cadastro, Esqueci a senha —
Perfil/Configurações de conta e Paywall foram removidos). São a revisão visual mais barata que existe
aqui — regravar sem olhar o diff joga fora o único sinal.

## Contexto

`PLANO_REDESIGN.md` na raiz é o plano de repaginação em execução: crítica de design, decisão de
palheta, as 5 fases e o que cada uma revelou no código. A seção 9 tem o handoff e o que ficou
pendente. Leia antes de mexer em UI.
