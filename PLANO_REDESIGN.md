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

### Fase 2 — As 5 telas do Figma (6-8h) — 2 de 5 entregues

| Tela | Arquivo | Estado |
|---|---|---|
| Busca / Filtros | `features/ui/search/SearchStep.kt` | ✅ Título que rola, `CedarSearchField`, contador de resultados, filtro de esporte funcionando (multi-seleção), estado "ainda não buscou" separado do "não achou" |
| Lista de vagas | `gamelist/GameListStep.kt` | ✅ Mesma linguagem: canvas, chips com alvo de 48dp, tokens no lugar dos `dp` soltos |
| Confirmação | *nova* `matchdetail/MatchConfirmedStep.kt` | ✅ Criada — `CedarSuccessScreen` + `CedarCodeBlock`. Falta o call site: hoje entrar numa partida só mostra um snackbar |
| Home / Mapa | `features/ui/map/MapStep.kt` | ⛔ **Bloqueada** — trabalho seu não commitado |
| Detalhe | `features/ui/matchdetail/MatchDetailStep.kt` | ⛔ **Bloqueada** — trabalho seu não commitado; e 33KB, precisa quebrar antes |
| Perfil | `features/ui/playerprofile/PlayerProfileStep.kt` | ⛔ **Bloqueada** — trabalho seu não commitado |

**O bloqueio.** `MapStep`, `MapStepModel`, `NearbyMatch`, `MatchDetailStep`, `MatchDetailStepModel` e
`PlayerProfileStep` estão à frente do que está pushado — de 800 bytes a 4KB cada. A ponte de arquivos não
consegue ler nada tão fundo quanto `products/*/src/commonMain/kotlin/com/walcker/...` (dei um jeito de pedir
a pasta `features/ui` como raiz própria e mesmo assim falha: o limite é sobre o caminho absoluto, não sobre
a raiz conectada), e o `device_bash` está fora do ar. Então eu não consigo nem ler nem sobrescrever com
segurança.

**Como destravar, em ordem de preferência:**

1. `git add -A && git commit && git push` na `developer` — eu re-clono e sigo.
2. Reiniciar o app desktop do Claude, o que costuma trazer o `device_bash` de volta.

### Fase 3 — As 9 telas por herança (6-8h)

Criar partida · Minhas partidas · Notificações · Busca de jogadores · Perfil de jogador ·
Avaliações · Form de avaliação · Denúncia · Login/cadastro.
Migrar `MyMatchCard`, `PlayerSearchResultCard`, `RatingCard`, `DimensionAveragesCard` para o `cedarDS`.

### Fase 4 — Acessibilidade e polimento (3-4h)

- [ ] Varredura de contraste nos dois modos
- [ ] Alvos de toque ≥ 48dp em tudo que é clicável
- [ ] `contentDescription` em todo ícone informativo
- [ ] Teste a 200% de escala de fonte nas 5 telas principais
- [ ] Modo escuro revisado tela a tela (o Figma não tem modo escuro — é derivação minha)

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

| Achado | Onde |
|---|---|
| O botão "voltar" do detalhe da partida **não faz nada** — `TextButton(onClick = { /* handled by Navigator */ })` com lambda vazia | `MatchDetailStep.kt:103` |
| O título do detalhe é `Text("Match Details")` — inglês hardcoded numa tela em português | `MatchDetailStep.kt:101` |
| O painel de filtros da busca era 100% placeholder: três textos "(em breve)" e strings pt-BR hardcodadas. O estado (`SearchFilters`) já suporta esporte, data e preço | corrigido: esporte agora funciona |
| A busca mostrava "Nenhuma partida encontrada para ''" **antes** de você digitar qualquer coisa | corrigido |
| `Game` não tem campo de nível, e o Figma mostra "Intermediário" em 3 das 5 telas | decisão de produto |
| `Game` não tem código de partida; o "SP-4821" do Figma não existe nos dados | `MatchConfirmedStep` esconde o bloco quando `matchCode` é null, em vez de mostrar id do Firestore |

## 8. Estado atual e próximo passo

Fases 0 e 1 estão gravadas no seu repositório. O projeto compila e roda sem nenhuma ação sua: os
componentes leem `CedarTokens`, que tem defaults funcionais, então espaçamento, raio e o verde de
disponibilidade já estão certos. O que ainda não mudou é o esquema de cores do Material e a tipografia
— isso vem no `CedarTheme.kt`.

Para destravar o resto:

1. **Aplicar o `CedarTheme.kt`** (anexo). É o único arquivo que não gravei, porque o seu local está
   466 bytes à frente do que está pushado e eu não consigo ler a sua versão pela ponte. Depois disso o
   app inteiro troca de cara. Enquanto não aplicar, o modo escuro usa as cores claras.
2. Baixar a Inter (Google Fonts, OFL) para `cedarDS/src/commonMain/composeResources/font/` e passar a
   família para `CedarTheme(fontFamily = ...)`.
3. Decidir sobre o azul (seção 2) — a troca é uma linha.
4. Ligar a navegação lista → detalhe da partida. Sem ela o `MatchCard` continua carregando o botão
   "Entrar" dentro do card (há um `TODO(fase 2)` nos dois call sites).
