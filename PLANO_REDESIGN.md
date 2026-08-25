# Repaginação do Mobile Match — plano de redesign

**Base:** Figma *SportsMatch — Mobile Marketplace Redesign* (5 telas) · código em `developer` · Phase 7 do `ROADMAP.md`
**Decisões já tomadas:** extrapolar o sistema do Figma para as 14 telas do app · começar pelos tokens
**Data:** 20/08/2026

---

## 1. Crítica de design — o Figma

### Impressão geral

O Figma acerta o essencial de um marketplace de partidas: em 2 segundos você entende que existe um mapa,
partidas perto de você e vagas acabando. A hierarquia "onde → quando → quantas vagas → quanto custa"
está correta e é a mesma decisão que apps de marketplace maduros tomam.

A maior oportunidade é que ele é um **mockup de layout, não um sistema**: não há um único
component, variant ou variable no arquivo (`get_variable_defs` volta `{}`, tudo é retângulo solto).
Isso significa que a repaginação não pode ser "implementar o Figma" — tem que ser
"extrair o sistema que está implícito no Figma e depois aplicá-lo".

### Usabilidade

| Achado | Severidade | Recomendação |
|---|---|---|
| A bottom bar tem 5 destinos, mas o Figma chama o quarto de **"Atividade"** e o código tem **"Chats"** — feature que não existe no app | 🔴 Crítico | Aba morta. Trocar `Chats` por `Atividade` e apontar para o par notificações + minhas partidas, que já existem (`NotificationHistoryStep`, `MyMatchesStep`) |
| O card de partida no Figma não tem CTA visível — só o pill verde "2 vagas", que parece botão mas é status | 🔴 Crítico | Card inteiro clicável levando ao detalhe; o pill vira `SlotBadge` puramente informativo, sem aparência de botão (sem sombra, sem elevação) |
| Na Home, o bottom sheet "Partidas perto de você" tem 180px fixos e mostra 1 card de 3 anunciados | 🟡 Moderado | Sheet arrastável em 3 alturas (peek / meio / cheio). Sem isso o mapa vira decoração: 430px de tela para pins que não levam a lugar nenhum |
| A tela de filtros usa 3 campos que parecem inputs de texto mas são seletores | 🟡 Moderado | Dar affordance de seleção (chevron à direita) ou virar chips, como o Figma já faz na Home |
| "Aplicar filtros" fica acima da lista de resultados, então os resultados nascem fora da dobra | 🟡 Moderado | Aplicar filtro em tempo real e transformar o botão em contador fixo no rodapé ("Ver 8 partidas") |
| Voltar no detalhe é um glifo `‹` solto sobre a foto, sem área de toque nem contraste garantido | 🟡 Moderado | Botão circular com fundo semitransparente, 48dp |
| Nenhuma tela do Figma tem estado de loading, vazio, erro, partida lotada ou usuário já inscrito | 🟡 Moderado | O app já tem todos esses estados. Eles precisam entrar no sistema, senão a repaginação quebra o que hoje funciona |

### Hierarquia visual

- **O que puxa o olho primeiro:** na Home, o pill verde "2 vagas" — e isso está **certo**, escassez é o motor
  do produto. Na Busca, o botão azul "Aplicar filtros", que é um passo intermediário e não deveria dominar.
- **Fluxo de leitura:** bom no detalhe (local → quando → preço → confirmados → CTA). Fraco no perfil, onde
  "Estatísticas" e "Histórico recente" têm o mesmo peso tipográfico (18px SemiBold) que o nome do usuário (24px).
- **Ênfase:** o card de confirmação (tela 04) é o melhor momento do arquivo — check verde, "Temos Jogo!" grande,
  código da partida destacado em azul. É um padrão que dá para reaproveitar em outras confirmações do app.

### Consistência

| Elemento | Problema | Recomendação |
|---|---|---|
| Raio de canto | 8 valores distintos: 13, 14, 15, 16, 18, 20, 21, 24 | Escala de 4 degraus: `sm 12` · `md 16` · `lg 20` · `xl 24` · `pill` |
| Tipografia | 10 tamanhos (10, 11, 12, 13, 14, 15, 16, 18, 24, 28) sem line-height definido (`leading-normal` em tudo) | Escala de 8 estilos mapeada em `MaterialTheme.typography`, com line-height explícito |
| Cor do texto sobre verde | `#091729` sobre `#29D178` no CTA, mas `#29D178` sobre branco no texto "2 vagas restantes" — o verde é fundo e texto ao mesmo tempo | Verde é **fundo de disponibilidade**. `#29D178` como texto em branco dá **2,0:1** e é ilegível; para texto verde usar `#087A41` (5,4:1) |
| Espaçamento | Margem lateral oscila entre 16, 20 e 24 entre telas | Fixar 20dp de margem de tela; 16dp de padding interno de card |
| Ícones | Glifos de texto (`⌕ ⌂ ＋ ◉ ◎ ⚽`) no lugar de ícones | Trocar por ícones vetoriais reais — o Figma usou texto só para prototipar |

### Acessibilidade

- **Contraste** (calculado, WCAG 2.1)
  - `#091729` sobre branco → **18,0:1** ✅
  - Branco sobre `#146BF2` (botão primário) → **4,76:1** — passa AA por 0,26. Qualquer ajuste de tom quebra.
    `#0F5AD1` daria **6,2:1** sem mudar a identidade percebida.
  - `#637385` sobre `#F5FAFF` → **4,63:1** — passa AA, mas é usado em 12px e 10px, sem folga nenhuma.
  - `#091729` sobre `#29D178` (CTA verde) → **9,0:1** ✅ — o CTA verde é, ironicamente, o mais acessível da tela.
  - `#29D178` como **texto** sobre branco ("2 vagas restantes") → **2,0:1** ❌ **falha AA com folga**.
    É o pior achado de acessibilidade do arquivo, e está no texto que comunica escassez.
- **Alvos de toque:** chips (42px), botão "Recentrar" (42px), pills (30-32px) e o `‹` de voltar estão **abaixo dos 48dp**.
  Correção: manter a altura visual, aumentar a área de toque com `minimumInteractiveComponentSize` / padding invisível.
- **Legibilidade:** os rótulos de 10px ("2 vagas" dentro do pill) são pequenos demais. Piso de 12sp.
- **Escala de fonte:** nada no Figma indica o que acontece com o texto a 200%. Os cards de 66px de altura fixa
  vão cortar conteúdo. Tudo tem que virar altura intrínseca.

### O que funciona bem

- A semântica **azul = ação, verde = disponibilidade** é clara e escalável; poucos apps do gênero acertam isso.
- O card de confirmação com código da partida (`SP-4821`) é uma boa ideia de produto, não só de layout —
  resolve o "como eu provo que paguei" na quadra.
- O fundo levemente azulado (`#F5FAFF`) com cards brancos cria profundidade sem sombra, o que rende bem
  em Compose e não custa performance.

### Prioridades

1. **Resolver a aba "Chats"** — é a única coisa no plano que muda navegação, e navegação é a mudança
   mais cara de fazer depois.
2. **Extrair o sistema antes de mexer em tela** — tokens e componentes primeiro. Se as 14 telas forem
   repaginadas uma a uma no olho, o app fica com 14 dialetos visuais.
3. **Corrigir os alvos de toque e o piso de 12sp na extração**, não depois — sai de graça agora e
   vira retrabalho em 9 telas depois.

---

## 2. A decisão que ficou em aberto: azul ou verde como primária?

Hoje o `CedarTheme` usa `#06C167` (verde) como `primary`. O Figma usa `#146BF2` (azul) como primária
e `#29D178` (verde) como disponibilidade.

**Recomendação: adotar o azul do Figma como `primary` e promover o verde a cor semântica de disponibilidade.**

O motivo não é estético. No app atual o verde faz três trabalhos ao mesmo tempo: é o botão "Entrar na partida",
é o `SlotBadge` de "2 vagas" e é o `primaryContainer` de destaque. Quando a mesma cor significa "aperte aqui"
e "ainda tem vaga", o usuário perde a leitura mais importante do produto — **quais partidas estão acabando**.
Separar as duas devolve o verde para o único lugar onde ele carrega informação.

Custo: `#06C167` → `#146BF2` é uma troca de token, não de código, porque não existe **nenhum** `Color(0x...)`
hardcoded fora do `cedarDS` (verifiquei: zero ocorrências). A troca é literalmente um arquivo.

Contraponto honesto: se o verde já é a marca em ícone de loja, splash e material de divulgação, trocar a
primária cria dissonância. Nesse caso a saída é **manter o verde como cor de marca** (logo, splash, ícone)
e usar o azul só dentro do produto — é o que o Spotify faz com o verde e o Airbnb com o vermelho.

Deixei os dois esquemas prontos no código: o `CedarTheme` aceita `brand: CedarBrand = CedarBrand.Blue`
e `CedarBrand.Green` produz um esquema simétrico. Trocar o default é uma linha.

Um detalhe que vale saber antes de escolher: o verde atual `#06C167` tem **2,4:1** com branco —
ou seja, "Entrar na partida" em branco sobre o verde de hoje já falha AA. Se ficar no verde, ele
precisa escurecer para `#087A41` de qualquer jeito, o que muda a identidade quase tanto quanto ir para o azul.

---

## 3. Gap analysis — Figma × código

### O que existe hoje

```
cedarDS/
  CedarTheme.kt        ← tokens (com sobras do projeto anterior)
  MatchTopBar.kt · MatchLoadingIndicator.kt · PasswordTextField.kt · CedarNavigation.kt
  components/          ← EmptyState · MatchBottomBar · MatchCard · RatingStars
                          RatingSummary · SlotBadge · SportChip
  ads/                 ← CedarAdBanner · AdBannerProvider
products/games/features/ui/   ← 14 telas (Voyager Steps + StepModels)
products/identity/            ← login/cadastro
```

### Dívidas encontradas no `cedarDS`

| Achado | Impacto |
|---|---|
| `CedarTypography.bodyVerse` — `FontFamily.Serif`, 17sp, line-height 1.75 | Resquício do app de leitura anterior (Lexis). Nada esportivo usa isso |
| `CedarColors.Gold = Color(0xFFEEEEEE)` | Token chamado "Gold" que é cinza claro. Usado como `secondary` nos dois modos |
| `CedarColors.ErrorBgDark = Color(0xFFF3A1D1B)` | **9 dígitos hex** — provável erro de digitação de `0xFF3A1D1B`. Compila, mas a cor resultante não é a pretendida |
| `onSurfaceVariant = #B6B6B6` no tema claro | Cinza claro sobre branco: **2,03:1**. Falha AA. É a cor do `SlotBadge` quando a partida lota |
| Sem tokens de espaçamento, raio ou elevação | 73 ocorrências de `16.dp`, 63 de `8.dp`, 38 de `12.dp` espalhadas em `products/` |
| `MatchBottomBarTab` com labels PT-BR hardcoded no DS | O projeto tem sistema de strings (`GamesStrings`, `PtBrGamesStrings`, `EnGamesStrings`) — o DS fura ele |
| `MatchBottomBarTab.values()` | Deprecado; usar `entries` |
| `MatchCard` do DS não se parece com o card do Figma | Hoje: esporte em CAIXA ALTA, 5 linhas de texto, botão à direita. Figma: local + horário + pill, card inteiro clicável |
| `EmptyState` privado duplicado em `MyMatchesStep` e `NotificationHistoryStep` | Sombreiam o componente do DS |
| `MyMatchCard`, `PlayerSearchResultCard`, `RatingCard`, `DimensionAveragesCard` vivem em `products/` | 4 variantes de card fora do sistema |
| Nenhuma fonte empacotada; `composeResources/` não existe | O Figma pede Inter. Hoje o app usa a fonte padrão de cada plataforma — o que faz Android e iOS parecerem apps diferentes |

### O que o Figma não cobre (9 das 14 telas)

Criar partida (3 passos) · Minhas partidas (abas) · Histórico de notificações · Busca de jogadores + filtros ·
Perfil de jogador (outro) · Lista de avaliações · Formulário de avaliação (bottom sheet) · Denúncia (bottom sheet) ·
Login/cadastro (`products/identity`).

Essas ganham o visual por herança dos tokens + componentes. Onde o Figma não dá resposta
(ex.: como é um stepper, como é um bottom sheet), a regra é: **Material 3 com os tokens do Cedar aplicados**,
sem inventar padrão novo.

---

## 4. O Cedar DS alvo

### Tokens (`cedarDS/.../cedar/tokens/`)

```
CedarPalette.kt     cores cruas extraídas do Figma + derivação do modo escuro
CedarSpacing.kt     xxs 4 · xs 8 · sm 12 · md 16 · lg 20 · xl 24 · xxl 32
CedarRadius.kt      sm 12 · md 16 · lg 20 · xl 24 · pill
CedarElevation.kt   flat 0 · raised 2 · overlay 8
CedarTypeScale.kt   8 estilos mapeados na Typography do M3 + Inter
CedarSemanticColors.kt   o que o M3 não tem: available / canvas / mapBase / surfaceSubtle
```

Acesso: `CedarTheme.spacing.md`, `CedarTheme.radius.lg`, `CedarTheme.colors.available`.
Cores do M3 continuam em `MaterialTheme.colorScheme` — sem esquema paralelo.

### Componentes a criar ou refazer

| Componente | Ação | Vem de |
|---|---|---|
| `MatchCard` | **Refazer** — local, quando, nível, `SlotBadge`, card inteiro clicável | Figma 01/02 |
| `SlotBadge` | Ajustar: piso de 12sp, cor semântica `available`, sem cara de botão | Figma 01/02 |
| `SportChip` | Ajustar: pill 42px visual + 48dp de toque | Figma 01 |
| `CedarSearchField` | **Novo** — a barra branca de 56px arredondada | Figma 01/02 |
| `CedarFilterRow` | **Novo** — rótulo + valor + chevron | Figma 02 |
| `CedarStatCard` | **Novo** — número grande + rótulo (42 / 68% / 4.8) | Figma 05 |
| `CedarSuccessScreen` | **Novo** — check, título, card de resumo, CTA | Figma 04 |
| `CedarPrimaryButton` / `CedarSecondaryButton` | **Novo** — 58px, raio 18, estados incluindo loading | Figma 03/04 |
| `CedarSectionHeader` | **Novo** — título 18sp + subtítulo opcional | Figma 01/05 |
| `MatchBottomBar` | Ajustar: `Chats` → `Atividade`, labels via strings, `entries` | Figma 01 |
| `EmptyState` | Consolidar as 3 versões em uma | código |
| `CedarBottomSheetScaffold` | **Novo** — o sheet arrastável da Home | Figma 01 |
| `PlayerAvatar` | **Novo** — círculo com iniciais/foto, 3 tamanhos | Figma 05 |

---

## 5. Fases

### Fase 0 — Limpeza e tokens (2-3h) ✅ *iniciada nesta sessão*

- [x] Extrair a paleta real do Figma
- [x] `CedarPalette`, `CedarSpacing`, `CedarRadius`, `CedarElevation`, `CedarTypeScale`, `CedarSemanticColors`
- [x] `CedarTheme` novo: azul primária, verde semântico, modo escuro derivado, `CedarBrand` para trocar
- [x] Matar `bodyVerse`, `Gold`, o hex de 9 dígitos e o `onSurfaceVariant` que falha AA
- [ ] Empacotar Inter em `cedarDS/src/commonMain/composeResources/font/` (Inter Regular/Medium/SemiBold/Bold)
- [ ] Trocar `MaterialTheme.typography` pela `cedarTypography(interFamily)`

**Entrega:** o app inteiro muda de cara sem nenhuma tela ser tocada.

### Fase 1 — Componentes base (4-6h) ✅ *entregue*

- [x] `CedarButton` — Primary (azul), Availability (verde), Secondary, Text; estado de loading sem pulo de layout
- [x] `CedarSearchField` + `CedarSearchEntryPoint` (o campo da Home que é botão, não campo)
- [x] `CedarSectionHeader` + `CedarScreenTitle`, com `heading()` semântico
- [x] `CedarStatCard` / `CedarStat` / `CedarStatRow`
- [x] `PlayerAvatar` — iniciais quando não há foto, três tamanhos
- [x] `SlotBadge` e `SportChip` ajustados (12sp, alvo de 48dp, texto vindo do chamador)
- [x] `EmptyState` com subtítulo, ícone e ação — retrocompatível, os 6 call sites atuais não mudaram
- [x] `MatchCard` no layout do Figma
- [x] `MatchBottomBar`: `Activity` no lugar de `Chats`, labels e badges do chamador, `entries`
- [x] `MatchTopBar`: tokens e `backContentDescription` (era "Voltar" hardcoded)
- [ ] Apagar as duas cópias privadas de `EmptyState` — fica para quando as telas forem tocadas
      (`MyMatchesStep.kt` tem trabalho seu não commitado)

**Entrega:** biblioteca pronta. Duas telas mexidas só no ponto onde chamam o `MatchCard`.

### Fase 2 — As 5 telas do Figma ✅ *entregue*

Destravou quando você pushou o `ac821f4 feat : start design`.

| Tela | Arquivo | O que mudou |
|---|---|---|
| Busca / Filtros | `search/SearchStep.kt` | Título que rola, `CedarSearchField`, contador de resultados, filtro de esporte funcionando (multi-seleção), estado "ainda não buscou" separado do "não achou" |
| Lista de vagas | `gamelist/GameListStep.kt` | Canvas, chips com alvo de 48dp, tokens no lugar dos `dp` soltos |
| Confirmação | *nova* `matchdetail/MatchConfirmedStep.kt` | `CedarSuccessScreen` + `CedarCodeBlock`. Falta o call site |
| Detalhe | `matchdetail/MatchDetailStep.kt` | Hierarquia local → quando → preço → confirmados; CTA fixo no rodapé; voltar funcionando; a tela passou a rolar; toda a cópia veio para `MatchDetailStrings` |
| Home / Mapa | `map/MapStep.kt` | Barra de busca flutuando sobre o mapa (não existia); sheet de próximas usando `MatchCard` |
| Perfil | `playerprofile/PlayerProfileStep.kt` | `PlayerAvatar` + `CedarStatRow` com três estatísticas; `RatingStars` no lugar dos emojis; datas por `formatShortDate` |

### Fase 3 — As 9 telas por herança (6-8h) ✅ *entregue*

| Tela | Estado |
|---|---|
| Minhas partidas | ✅ `MyMatchesStep` + `MyMatchCard` — cópia privada de `EmptyState` apagada, vazio com saída ("Buscar partidas") |
| Notificações | ✅ `NotificationHistoryStep` — três bugs corrigidos (ver 7c) |
| Busca de jogadores | ✅ `PlayerSearchStep` + `PlayerSearchResultCard` — `CedarSearchField`, `PlayerAvatar` com iniciais |
| Perfil de jogador | ✅ `PlayerDetailsStep` + `RatingCard` — `CedarTopBar`, `PlayerAvatar`, erro com ação |
| Lista de avaliações | ✅ `PlayerRatingsListStep` — ordenação em chips, paginação com fallback manual |
| Form de avaliação | ✅ `RatingForm` + `RatingBottomSheet` — `CedarStarPicker` no lugar de cinco `TextButton { Text("⭐") }` |
| Denúncia | ✅ `ReportBottomSheet` — motivos viraram rádio, não `FilterChip` de largura cheia |
| Criar partida | ✅ `CreateMatchStep` — ligado ao `CreateMatchStrings` que existia e nunca foi usado |
| Login / cadastro | ✅ `products/identity` — as 5 telas, mais `AuthScaffold` e o `PasswordOutlinedTextField` do DS |

Componentes novos no `cedarDS` nesta fase:

- **`CedarTag`** (+ `CedarTagTone`) — rótulo não interativo para papel, status e categoria. O app usava
  um `AssistChip` **desabilitado** para isso, que um leitor de tela anuncia como botão desabilitado e
  convida a um toque que não faz nada.
- **`CedarStarPicker`** — grupo de rádio de 1 a 5 estrelas, alvo de 48dp por estrela e nome acessível
  em cada uma ("3 de 5 estrelas").
- **`PasswordOutlinedTextField` refeito** — os rótulos do olho eram `"Show password"` / `"Hide password"`
  em inglês dentro do componente, num app em pt-BR; e faltava `KeyboardType.Password`, então o teclado
  tratava a senha como texto comum, com sugestão e autocorreção.

E em `products/identity`, **`AuthScaffold`** — a moldura comum de entrar, criar conta e recuperar senha,
que eram o mesmo `Scaffold` + `Column` copiado três vezes com os mesmos números soltos.

Ainda em `products/`: `DimensionAveragesCard` (não migrado para o `cedarDS`).

### Fase 4 — Acessibilidade e polimento ✅ *entregue*

O `CedarTheme.kt` **entrou**: a sua cópia local estava idêntica ao que está pushado, então deu para
gravar. O app agora roda no esquema novo — azul como primária, verde só para disponibilidade, escala
tipográfica completa e modo escuro de verdade.

- [x] **Varredura de contraste nos dois modos** — tabela abaixo
- [x] **Alvos de toque ≥ 48dp** — um achado: os links de termos e privacidade do paywall tinham ~36dp
- [x] **`contentDescription` em todo ícone informativo** — zero `IconButton` sem nome; os sete
      `contentDescription = null` que restam são ícones decorativos ao lado de texto, que é o correto
- [x] **200% de escala de fonte** — `CedarButton` e `CedarSearchField` tinham `.height(56.dp)` fixo,
      e o rótulo do botão era `maxLines = 1`: "Enviar e-mail de recuperação" virava reticências.
      Viraram `defaultMinSize` e duas linhas
- [x] **Modo escuro** — nasce com o `CedarTheme` novo; a rampa escura é derivação minha (o Figma não
      tem modo escuro) e passa em todos os pares de texto

**O que mudou no código**

| Achado | Estado |
|---|---|
| **`OutlineStrong = #C9D9E8` dá 1,44:1 com branco.** É a borda do `OutlinedTextField` — o contorno que diz onde o campo começa. WCAG 1.4.11 pede 3:1 | ✅ `#6F8AA6`, 3,58:1 (ainda mais claro que o `outline` padrão do Material) |
| `OutlineStrongDark = #44566F` dá 2,23:1 sobre a superfície escura | ✅ `#627893`, 3,68:1 |
| A estrela vazia do `RatingStars` usava `outlineVariant` — a cor da divisória. Ela é o que diz "de 5", então precisa ser vista | ✅ passou a usar `outline` |
| **Links de termos e privacidade do paywall com ~36dp de alvo**, e `clickable` sem papel: o leitor de tela lia dois parágrafos, não dois links | ✅ 48dp, `Role.Button`, cor de link |
| `SubscriptionInfoSection` pintava o fundo com `surfaceVariant.copy(alpha = 0.35f)` — translúcido sobre um fundo que o componente não conhece, contraste incalculável, no texto que a loja exige que seja legível | ✅ cartão opaco |
| **Não dava para digitar uma nota decimal no filtro de jogadores.** O campo desenhava `value?.toString()`, então "4," não parseava, o valor virava nulo e o campo se apagava antes do segundo dígito | ✅ texto é estado local, só o `Float` sobe |
| `PlayerFiltersPanel` tinha os mesmos dez `FilterChip` de largura cheia da criação de partida (aqui a seleção múltipla está certa — só o layout estava errado) | ✅ `FlowRow` |
| `EmptyPaywallState` espaçava com `Spacer(Modifier.padding(top = 12.dp))` — um `Spacer` sem tamanho | ✅ `Arrangement.spacedBy` |
| `DimensionAveragesCard` era o último `Card` do Material: sombra e cor de container próprios, brigando com o relevo do canvas | ✅ `Surface` plana |

**Contraste — modo claro**

| Par | Razão | AA texto |
|---|---|---|
| Ink900 sobre Canvas (título, corpo) | 17,15:1 | ✅ |
| Ink500 sobre Canvas (subtítulo, rótulo) | 4,63:1 | ✅ |
| Ink500 sobre branco (subtítulo em cartão) | 4,86:1 | ✅ |
| Blue600 sobre branco (link, botão de texto) | 4,76:1 | ✅ |
| branco sobre Blue600 (botão primário) | 4,76:1 | ✅ |
| Green700 sobre branco (verde como texto) | 5,43:1 | ✅ |
| Ink900 sobre Green500 (botão de vaga) | 8,99:1 | ✅ |
| Red600 sobre branco (erro) | 4,83:1 | ✅ |
| OutlineStrong sobre branco (borda de campo) | 3,58:1 | ✅ (regra de 3:1) |

**Contraste — modo escuro**

| Par | Razão | AA texto |
|---|---|---|
| InkDark900 sobre CanvasDark | 16,05:1 | ✅ |
| InkDark500 sobre SurfaceDark | 7,55:1 | ✅ |
| Blue400 sobre SurfaceDark (primária) | 6,03:1 | ✅ |
| Green400 sobre SurfaceDark | 9,07:1 | ✅ |
| Red400 sobre SurfaceDark (erro) | 7,32:1 | ✅ |
| Ink900 sobre Blue400 (botão primário) | 6,50:1 | ✅ |
| OutlineStrongDark sobre SurfaceDark | 3,68:1 | ✅ (regra de 3:1) |

Dois pares reprovam de propósito: **texto desabilitado** (`Ink300`, 2,56:1) — o WCAG 1.4.3 isenta
controle desabilitado — e a **divisória** (`Outline`, 1,44:1), que é decorativa e não delimita
componente nenhum. Se quiser fechar os dois assim mesmo, é troca de token.

### Fase 5 — Ícones e identidade (2-3h)

Ícone de app Android (adaptive) e iOS · splash · troca dos glifos de texto por ícones vetoriais.

**Total: 23-32h.** Cabe dentro do orçamento de 8-12h da Phase 7 apenas se ficar nas Fases 0-2;
as fases 3-5 são o que transforma o redesign de "5 telas bonitas" em "app coerente".

---

## 6. Validação — o que dá para fazer sem usuários

O app ainda não tem base instalada, então pesquisa quantitativa está fora. O que rende agora:

### Teste de usabilidade moderado — 5 participantes, 1 semana

Recrutamento: jogadores de futebol/vôlei amador que já usam grupo de WhatsApp para marcar partida.
Cinco pessoas pegam ~85% dos problemas de usabilidade; passar disso é retorno decrescente.

**Roteiro (30 min):**

1. *Aquecimento (5 min)* — Como você marca uma pelada hoje? Me conta a última vez.
2. *Contexto (10 min)* — O que dá errado nesse processo? Já ficou sem jogar por causa disso?
3. *Tarefas no protótipo (10 min)* — sem ajuda, pensando em voz alta:
   - "Ache uma partida de futebol hoje à noite perto de você."
   - "Entre nessa partida."
   - "Descubra quantas pessoas já confirmaram."
   - "Ache uma partida para o fim de semana com nível iniciante."
4. *Reação (5 min)* — O que você achou que ia acontecer e não aconteceu? O que te faria desistir?

**As três perguntas que este redesign precisa responder:**

- O pill verde "2 vagas" é lido como **status** ou como **botão**? (crítico — define o card inteiro)
- O mapa ajuda ou as pessoas vão direto para a lista? (define quanto investir na Fase 2 do mapa)
- "Garantir minha vaga · R$ 25" é entendido como pagamento agora ou na quadra? (risco de abandono)

### Validação contínua, sem participante

- **Teste de 5 segundos** com a Home nova: mostrar 5s e perguntar "o que esse app faz?"
- **Preference test** azul vs. verde como primária — 20 respostas num formulário resolvem a seção 2 deste plano
- Instrumentar o funil `view → join → confirm → play` (já previsto na Phase 7) **antes** do redesign ir para produção,
  para que o antes/depois seja mensurável

---

## 7. Riscos e armadilhas

| Risco | Mitigação |
|---|---|
| **O clone do GitHub atrasa em relação à sua máquina.** `CedarTheme.kt` local tem 5911 bytes; o pushado tem 5445 | Os tokens novos vão em **arquivos novos** (sem risco de sobrescrever). O `CedarTheme.kt` reescrito vai por anexo, para você aplicar por cima do seu |
| Restrições de `commonMain` (KMP) — `Math.`, `java.*` não existem no iOS | Tokens são só `Color`/`Dp`/`TextStyle`; sem risco. Mas a varredura vale ao mexer nas telas |
| `lazy` dentro de `lazy` estoura em runtime e o CI não pega | O sheet arrastável da Home é exatamente onde esse erro nasce. Varrer antes de entregar a Fase 2 |
| O alvo iOS não é compilado por nenhum job de CI | Fechar esse buraco antes da Fase 2, senão o redesign quebra o iOS em silêncio |
| `MatchDetailStep.kt` com 33KB e `MatchDetailStepModel.kt` com 26KB | Quebrar em seções **antes** de repaginar, não durante |
| Modo escuro é derivação minha, não do Figma | Revisar tela a tela na Fase 4, ou desligar o modo escuro até haver design |

---

## 7b. O que a Fase 2 revelou no código

| Achado | Estado |
|---|---|
| **`System.currentTimeMillis()` em `commonMain`** (`PlayerProfileStep.formatRatingDate`). É `java.lang.System` — o alvo iOS não compila, e nenhum job de CI compila iOS, então nada avisava. Mesma família do `Math.toRadians` que já tinha quebrado o `NearbyMatch` | ✅ corrigido — datas agora por `formatShortDate` do `core` |
| O botão "voltar" do detalhe **não fazia nada**: `onClick` com lambda vazia e um comentário dizendo que o Navigator resolvia. No Android o gesto do sistema disfarçava; no iOS não havia saída da tela | ✅ corrigido |
| **A tela de detalhe não rolava** — tudo num `Column` sem `verticalScroll`, com um `LazyColumn` de 240dp fixos dentro. Num 390×844 o botão de entrar ficava abaixo da dobra | ✅ corrigido — a tela rola e o CTA foi para o rodapé fixo |
| O título do detalhe era `Text("Match Details")`, e a tela misturava "Duration"/"Price"/"Participants" com "Sair"/"Cancelar Partida" | ✅ corrigido — `MatchDetailStrings` |
| No mapa, a lista de próximas lia `sport.name` (o nome do enum: `FUTEBOL`) em vez de `sport.label` (`Futebol`) | ✅ corrigido |
| O painel de filtros da busca era 100% placeholder, com strings pt-BR hardcodadas, embora `SearchFilters` já suportasse esporte | ✅ esporte agora funciona |
| A busca mostrava "Nenhuma partida encontrada para ''" **antes** de você digitar | ✅ corrigido |
| Estrelas desenhadas com `"⭐ ".repeat(n)` — um leitor de tela anuncia isso como uma fila de nomes de emoji | ✅ corrigido — `RatingStars` com `contentDescription` |
| `Game` não tem campo de nível, e o Figma mostra "Intermediário" em 3 das 5 telas | ⏳ decisão de produto |
| `Game` não tem código de partida; o "SP-4821" do Figma não existe nos dados | ⏳ `MatchConfirmedStep` esconde o bloco quando `matchCode` é null |

## 7c. O que a Fase 3 revelou no código

| Achado | Estado |
|---|---|
| **O ✕ do cabeçalho de Notificações chamava `Refresh`.** Tocar em fechar recarregava a lista e deixava a sheet aberta | ✅ corrigido — fecha a sheet |
| **`onDelete` era passado para a linha da notificação e nunca usado**, então `NotificationHistoryEvent.Delete` era inalcançável pela UI | ✅ corrigido — botão de apagar |
| **`formatTimeAgo` devolvia inglês hardcoded** — "2 hours ago" numa tela em português | ✅ corrigido — rótulos em `NotificationHistoryStrings` |
| `MyMatchCard` lia `game.sport.name` (o nome do enum: `FUTEBOL`). **Segunda ocorrência** do mesmo bug do mapa | ✅ corrigido |
| Botão de ação ("Sair"/"Cancelar") aparecia em partida já cancelada ou finalizada | ✅ escondido quando não há o que cancelar |
| Badge de papel era um `AssistChip` **desabilitado** — leitor de tela anuncia botão desabilitado | ✅ virou `CedarTag` |
| Avatar de jogador era um círculo cinza vazio quando não havia foto — que é a maioria dos perfis | ✅ `PlayerAvatar` com iniciais, em busca e perfil |
| "✓" e "✕" como rótulo de `TextButton` — sem nome acessível | ✅ viraram `IconButton` com `contentDescription` |
| **`CreateMatchStep` nunca usou `CreateMatchStrings`.** Lia `strings.gameList` e escrevia cada rótulo em pt-BR direto no código, com o arquivo traduzido parado ao lado | ✅ ligado |
| **Criar partida mostrava o id do documento do Firestore ao usuário** — `"Match criada: ${matchId}"`, em duas snackbars seguidas | ✅ uma mensagem, sem id |
| `.onFailure { error.message }` na criação de partida jogava a mensagem da exceção na tela — em inglês e técnica | ✅ mensagem traduzida |
| Enviar o formulário de criação trocava tudo por um spinner centralizado: o usuário perdia de vista o que tinha preenchido | ✅ formulário desabilitado, botão carregando |
| Esporte era uma coluna de dez `FilterChip` de largura cheia — dez linhas antes do resto do formulário | ✅ `FlowRow` de pílulas |
| O diálogo de horário não tinha fundo: o `TimePicker` flutuava sobre o formulário | ✅ `Surface` |
| **Excluir conta era um `OutlinedButton` idêntico a "Restaurar compras" e "Sair"**, e a confirmação era um botão primário verde | ✅ texto em cor de erro, confirmação em vermelho |
| A tela de Perfil não rolava — um `Spacer(weight(1f))` numa `Column` de altura fixa | ✅ `verticalScroll` |
| **No paywall, "Assinar" dividia a linha meio a meio com "Restaurar compras"** | ✅ assinar ocupa a linha; restaurar virou texto |
| Login, cadastro e recuperação não rolavam: com o teclado aberto, o botão saía da tela | ✅ `verticalScroll` + `imePadding` |
| Os campos de e-mail não tinham `KeyboardType.Email` nem ação de IME | ✅ e-mail → senha → enviar |
| A mensagem de erro dos formulários aparecia calada para leitor de tela | ✅ `liveRegion` |
| Os planos do paywall eram `clickable` sem papel — dois botões idênticos para quem não enxerga | ✅ `selectableGroup` + `Role.RadioButton` |
| Confirmação de senha só reclamava depois de tocar em "Criar conta", e o erro saía longe do campo | ✅ erro no próprio campo, enquanto digita |

## 8. Estado atual e próximo passo

As Fases 0 a 4 estão gravadas no seu repositório: as 14 telas passaram, o design system está montado
e o tema novo entrou. O que o `CedarTheme` antigo levava embora, para registro:

- `ErrorBgDark = Color(0xFFF3A1D1B)` tinha **nove dígitos hexadecimais**. `Color(Long)` espera
  `0xAARRGGBB`; aquele valor estourava 32 bits e nunca produziu a cor que o nome prometia. Provável
  erro de digitação de `0xFF3A1D1B`.
- `onSurfaceVariant = 0xFFB6B6B6` sobre branco dava **2,03:1** — todo texto secundário do app
  reprovava no AA. É a cor de quase todo subtítulo e rótulo destas telas.
- `error = 0xFFFDA291` (salmão) sobre branco dava **2:1**. A mensagem de erro era o texto que mais
  precisa ser lido e o menos legível da tela.
- `CedarTypography` com um único estilo `venueName` e o objeto acessor que o servia: **zero chamadas**
  no app inteiro.

Para destravar o resto:

1. **Olhar o app com o tema novo.** O `CedarTheme.kt` foi gravado nesta sessão — a sua cópia local
   estava idêntica ao que está pushado, então deu para escrever com segurança. Esse é o commit que
   troca a cara do app inteiro: primária azul, verde só para vaga, tipografia nova, modo escuro real.
   Se a marca precisar continuar verde, é uma linha: `CedarTheme(brand = CedarBrand.Green)`.
2. **Regravar os goldens do Paparazzi.** As cinco telas de `products/identity` têm teste de
   screenshot (`LoginStepTest`, `SignUpStepTest`, `ForgotPasswordStepTest`, `ProfileStepTest`,
   `PaywallStepTest`, `PaywallComponentsTest`). As assinaturas dos composables não mudaram, então
   compila; as imagens mudaram, então falha até um `./gradlew :products:identity:screenshotTests:recordPaparazziDebug`.
3. **Rodar o build.** Nunca consegui compilar — o container não tem Android SDK. A verificação foi
   leitura, balanceamento de chaves e varredura de `java.`/`Math.`/`System.`. Um
   `./gradlew build` agora é o passo mais valioso da lista — foram dezenas de arquivos em sequência
   sem compilador.
4. **Fechar o buraco do iOS no CI.** Duas quebras de `commonMain` já passaram por lá sem ninguém ver
   (`Math.toRadians` no `NearbyMatch`, `System.currentTimeMillis` no perfil). Enquanto nenhum job
   compilar iOS, a terceira também passa.
5. Baixar a Inter (Google Fonts, OFL) para `cedarDS/src/commonMain/composeResources/font/` e passar a
   família para `CedarTheme(fontFamily = ...)`.
6. Ligar a navegação lista → detalhe e o call site do `MatchConfirmedStep`. Sem elas o `MatchCard`
   continua carregando o botão "Entrar" dentro do card (`TODO(fase 2)` nos dois call sites) e a tela de
   confirmação existe mas nunca aparece.

Sobra a **Fase 5** — ícone de app (Android adaptive e iOS), splash e a troca dos glifos de texto que
ainda sobraram por ícones vetoriais. É a única fase que precisa de arte, não de código.

O passo a passo para tocar isso pelo terminal está na **seção 9**.

---

## 9. Continuando pelo CLI

Desta sessão em diante o trabalho passa para o Claude Code rodando dentro do repositório. Esta seção é
o handoff: o que está no disco, o que verificar antes de qualquer coisa, e como pedir o resto.

### 9.1. Estado do repositório

Tudo das Fases 0 a 4 está **gravado e não commitado** em `~/Developer/mobile-match`. São 41 arquivos
modificados e 5 novos:

```
cedarDS/
  CedarTheme.kt                    tema novo (o commit que troca a cara do app)
  PasswordTextField.kt             rótulos do olho traduzíveis + KeyboardType.Password
  tokens/CedarPalette.kt           contraste dos contornos
  components/CedarButton.kt        defaultMinSize + rótulo em 2 linhas
  components/CedarSearchField.kt   defaultMinSize
  components/RatingStars.kt        estrela vazia visível
  components/CedarTag.kt           NOVO
  components/CedarStarPicker.kt    NOVO

products/games/    9 telas + 7 arquivos de strings (2 novos: MapStrings, MatchDetailStrings)
products/identity/ 5 telas + 5 arquivos de strings + AuthScaffold.kt (NOVO)
```

**Nada disso foi compilado.** Não havia Android SDK no ambiente da sessão; a verificação foi leitura,
balanceamento de chaves e parênteses, e varredura de `java.`/`Math.`/`System.` em `commonMain`.

### 9.2. A primeira coisa a rodar

```bash
cd ~/Developer/mobile-match
git switch -c redesign/cedar        # o trabalho está no seu branch atual, não commitado
./gradlew build
```

Se quebrar, é quase certo que seja uma destas três coisas — todas mecânicas:

| Sintoma | Causa provável |
|---|---|
| `Unresolved reference` num componente `Cedar*` | assinatura que mudou nesta sessão; ver 9.4 |
| `No value passed for parameter` em `PasswordOutlinedTextField` | os dois rótulos do olho agora são obrigatórios (3 call sites, todos já atualizados) |
| Teste de screenshot falhando | esperado; ver 9.3 |

Depois do build:

```bash
./gradlew :products:identity:screenshotTests:recordPaparazziDebug
```

### 9.3. Os testes de screenshot vão falhar — e devem

`products/identity/screenshotTests` tem golden de `LoginStep`, `SignUpStep`, `ForgotPasswordStep`,
`ProfileStep`, `PaywallStep` e `PaywallComponentsTest`. As **assinaturas dos composables não mudaram**
(conferi uma a uma), então compila; as imagens mudaram em todas, então falha até regravar. Olhe os
diffs antes de aceitar: é a melhor revisão visual gratuita que existe deste redesign.

### 9.4. APIs que mudaram nesta sessão

Se algum código seu fora do que foi tocado chamar isto, precisa ajustar:

- `PasswordOutlinedTextField` ganhou `showPasswordLabel` e `hidePasswordLabel` **obrigatórios**. Sem
  valor padrão de propósito: um padrão em inglês só esconderia o problema que a mudança resolve.
- `CedarTheme` ganhou `brand` e `fontFamily`, ambos com padrão. `CedarTheme { ... }` continua válido.
- `CedarTheme.typography.venueName` **não existe mais** (tinha zero chamadas).
- `EmptyPaywallState`, `SubscriptionInfoSection` e `DimensionAveragesCard` ganharam `modifier` no fim,
  com padrão.
- `CreateMatchStrings.submitting` foi removido (o botão mostra spinner, não texto).

### 9.5. Como pedir o resto ao Claude Code

Abra o Claude Code na raiz do repositório. Ele lê arquivos direto, então o fluxo é diferente do que
foi aqui — não precisa de anexo, precisa de escopo.

**Primeiro comando, para dar contexto:**

```
Leia PLANO_REDESIGN.md por inteiro. É o plano de repaginação que está em execução.
As Fases 0 a 4 já estão no disco, não commitadas. Rode ./gradlew build e me
mostre os erros de compilação, sem corrigir nada ainda.
```

Corrigir erro de compilação é a única tarefa em que vale deixar ele iterar sozinho: o compilador é o
teste. Para o resto, escopo estreito funciona melhor que pedido amplo — um arquivo ou uma tela por vez,
com o critério de pronto explícito.

**Para a Fase 5** (a única que sobrou):

```
Fase 5 do PLANO_REDESIGN.md. Comece pelos glifos de texto que ainda restam na UI:
procure em products/ por Text() cujo conteúdo seja um emoji ou símbolo usado como
ícone e troque por Icon() com contentDescription, ou por null se houver texto ao
lado dizendo a mesma coisa. Não invente string nova: use os arquivos *Strings.kt.
```

Ícone de app e splash precisam de arte, não de código — o Claude Code pode gerar o
`ic_launcher_foreground.xml` e o `Contents.json` do asset catalog depois que você tiver o SVG.

**O `CLAUDE.md` já está na raiz.** Foi escrito nesta sessão e cobre o que o Claude Code redescobriria
a cada conversa: as restrições de `commonMain`, as regras do `cedarDS` (só o `CedarPalette` pode ter
cor literal; o design system não tem camada de strings), o piso de acessibilidade, o padrão do
Lyricist, as armadilhas de Compose que apareceram aqui e os comandos de verificação. Ajuste conforme
o projeto andar — ele é o contexto permanente, este plano é o do momento.

### 9.6. O que ficou pendente, em ordem de valor

1. `./gradlew build` e regravar os goldens (9.2 e 9.3).
2. ~~**Ligar a navegação lista → detalhe**~~ ✅ *feito (ver 9.7)* — falta ainda o call site do
   `MatchConfirmedStep`: a tela de confirmação existe mas nunca aparece.
3. **Consertar o CI.** `.github/workflows/pull-request.yml` roda
   `:products:bible:testDebugUnitTest` e `:products:bible:compileDebugKotlinAndroid` — e
   **`:products:bible` não existe**, saiu quando o projeto foi derivado do mobile-lexis. Enquanto
   isso, `:products:games`, que é o produto, não é compilado nem testado em nenhum job, e
   `ios-release.yml` só dispara em tag `v*`. Trocar aqueles dois alvos por `:products:games` e
   acrescentar um `compileKotlinIosSimulatorArm64` fecha o buraco do iOS de uma vez.
4. Inter (Google Fonts, OFL) em `cedarDS/src/commonMain/composeResources/font/`, passada em
   `CedarTheme(fontFamily = ...)`. Enquanto não entrar, Android e iOS parecem dois apps.
5. Fase 5 — ícone, splash, glifos.
6. Teste de usabilidade com 5 participantes (seção 6). O roteiro está pronto.

### 9.7. Navegação lista → detalhe (feito nesta sessão)

O botão "Entrar no jogo" saiu dos cards da **home** (`GameListStep`) e da **busca** (`SearchStep`).
O card inteiro agora é clicável e abre o `MatchDetailStep`, que já era o dono legítimo do
`JoinGameUseCase` — entrar na partida passou a ser uma decisão da tela de detalhe, não do card.

Foi feito 100% no padrão MVI, espelhando `PlayerSearchStep`/`PlayerSearchStepModel`, e materializou a
separação **UI / UiModel** que agora é regra no `CLAUDE.md` (seção Arquitetura):

- **Navegação pelo model.** Toque → `onEvent(SelectGame(id))` → o `*StepModel` emite
  `NavigateToMatchDetail(id)` num `Channel` → o `*Step` coleta o efeito e faz `navigator.push`.
  Nenhum `navigator.push()` solto em composable de conteúdo.
- **Strings pelo State.** `GameListStepModel`/`SearchStepModel` resolvem os textos a partir do
  `GamesStringsHolder` (injetado por Koin) e os entregam no `*State` (`state.strings`, `state.cardStrings`).
  Como o holder só é preenchido no `DisposableEffect` de `ProvideGamesStrings` — depois da primeira
  composição — as strings são re-carimbadas no primeiro update de dados.
- **UI stateless.** `GameListContent(state, onEvent, …)` e `SearchContent(state, onEvent, …)` não
  conhecem `navigator` nem o model; são o que os testes de screenshot chamam.

Limpeza junto: `joinSuccess`/`joinError`/`joinButton` (GameList) e `joinSuccess`/`joinError` (Search)
ficaram órfãos quando o botão saiu do card e foram removidos das `data class` e das instâncias
`...StringsEn`/`...StringsPt`. `SearchStepModel` perdeu a dependência de `JoinGameUseCase` (e o
`joinGame = get()` do `GamesUiModule`). O `JoinGameUseCase` continua vivo no fluxo do detalhe.

Verde em `:products:games` e `:products:identity` (`compileKotlinIosSimulatorArm64`) e em
`:products:games:compileDebugKotlinAndroid`. Pendente aqui: o call site do `MatchConfirmedStep`.
