# Match Point Sports

Sim. Agora que definimos o modelo, eu montaria o prompt pensando em MVP real, já preparado para crescer, mas sem transformar a primeira versão num Frankenstein de funcionalidades.

A regra de negócio central será:

Jogador gratuito → Organizador cria a partida → vagas abertas → sistema encontra jogadores → match → confirmação → pagamento → avaliação.

E a assinatura será voltada principalmente ao organizador, não ao jogador.

Abaixo está o prompt completo para você colocar no Lovable:

Escrita

PROMPT MASTER — APLICATIVO DE CONEXÃO ESPORTIVA

Crie um aplicativo web responsivo, mobile-first, moderno e profissional para conectar pessoas que querem praticar esportes com partidas que possuem vagas disponíveis.

O aplicativo deve começar com foco em FUTEBOL e FUTSAL, mas sua arquitetura precisa permitir expansão futura para outros esportes e outros países.

CONCEITO CENTRAL

O aplicativo resolve uma dor simples:

“Tem um jogo acontecendo, mas faltam jogadores.”

O jogo já existe. A quadra já está reservada. O organizador já criou a partida. O problema é encontrar as pessoas que faltam.

O aplicativo funciona como um marketplace de conexão esportiva.

A lógica é:

ORGANIZADOR “Tenho 2 vagas.”

↓

PLATAFORMA “Vou encontrar jogadores disponíveis.”

↓

JOGADOR “Quero jogar.”

↓

MATCH

↓

JOGO COMPLETO.

A experiência deve ser inspirada na simplicidade de plataformas como Uber/99, sem copiar identidade visual ou interface.

1. PRIMEIRO MERCADO

O primeiro teste será realizado localmente, começando pela região do União dos Cegos e utilizando partidas no Green Ball como laboratório inicial.

NÃO criar inicialmente uma plataforma nacional.

O objetivo é validar o conceito localmente.

Estratégia:

UNIÃO DOS CEGOS ↓ BAIRROS PRÓXIMOS ↓ OUTRAS REGIÕES ↓ OUTRAS CIDADES ↓ OUTROS ESTADOS ↓ OUTROS PAÍSES

A arquitetura deve permitir essa expansão posteriormente.

2. MODELO DE NEGÓCIO

IMPORTANTE:

O JOGADOR NÃO DEVE TER ASSINATURA OBRIGATÓRIA.

O jogador deve poder utilizar gratuitamente a plataforma para:

Criar perfil

Encontrar partidas

Receber oportunidades

Entrar em jogos

Informar disponibilidade

Avaliar partidas e jogadores

A monetização principal será direcionada ao ORGANIZADOR.

ORGANIZADOR

O organizador poderá começar gratuitamente e posteriormente assinar um plano.

Plano gratuito

Criar partidas

Abrir vagas

Encontrar jogadores

Gerenciar uma quantidade limitada de partidas

Confirmar jogadores

Plano Organizador

Assinatura mensal.

Recursos:

Mais partidas

Gestão avançada de jogadores

Lista de espera

Controle de pagamentos

Histórico

Notificações

Relatórios

Avaliações

Gestão de partidas recorrentes

Ferramentas de organização

Plano Profissional

Para organizadores que administram muitas partidas ou grupos.

Recursos avançados:

Mais partidas

Gestão de múltiplos grupos

Relatórios

Controle financeiro

Histórico completo

Gestão de jogadores

Recursos avançados de comunicação

A assinatura deve ser implementada de forma modular para que preços e limites possam ser alterados posteriormente.

3. POSSÍVEL TAXA POR TRANSAÇÃO

A arquitetura deve permitir futuramente cobrar uma pequena taxa sobre pagamentos realizados pela plataforma.

Exemplo:

Partida: R$ 20,00

Taxa da plataforma: R$ 1,50

Total: R$ 21,50

NÃO tornar essa cobrança obrigatória no primeiro MVP.

Criar estrutura preparada para:

Valor da partida

Taxa da plataforma

Valor total

Pagamento

Reembolso

Status da transação

A taxa deverá ser configurável pelo administrador.

4. PERFIS DE USUÁRIO

Criar três tipos principais:

JOGADOR

ORGANIZADOR

ARENA / EMPRESA ESPORTIVA

O administrador terá acesso separado.

5. CADASTRO DO JOGADOR

Campos:

Nome

Foto

E-mail

Telefone

WhatsApp

Data de nascimento

Localização

Esportes

Posição

Nível de habilidade

Disponibilidade

Preferências

Histórico

Avaliações

Criar perfil público limitado.

Mostrar:

Nome Foto Esporte Posição Nível Avaliação Quantidade de partidas

6. DISPONIBILIDADE DO JOGADOR

Criar recurso:

“ESTOU DISPONÍVEL PARA JOGAR”

O usuário poderá ativar sua disponibilidade.

Exemplo:

🟢 Disponível

Esporte: Futsal

Dia: Hoje

Horário: 18h às 22h

Distância: Até 5 km

Quando existir uma partida compatível, o sistema deverá identificar o jogador.

7. SISTEMA DE MATCH

O sistema deverá cruzar:

Esporte

Data

Horário

Localização

Distância

Disponibilidade

Nível

Preferências

Exemplo:

Partida:

Futsal Green Ball 20h 2 vagas

Encontrar jogadores:

Próximos

Disponíveis

Interessados em futsal

Compatíveis com horário

Criar uma pontuação de compatibilidade.

Exemplo:

MATCH 95%

O sistema deve priorizar jogadores com maior compatibilidade.

8. ORGANIZADOR CRIA UMA PARTIDA

Fluxo:

Criar partida

↓

Escolher esporte

↓

Informar local

↓

Informar quadra

↓

Informar data

↓

Informar horário

↓

Quantidade máxima de jogadores

↓

Quantidade atual

↓

Vagas disponíveis

↓

Valor da participação

↓

Publicar

9. EXEMPLO REAL

O organizador criou:

⚽ FUTSAL

📍 Green Ball

🕐 Hoje às 20h

👥 12/14 jogadores

🟢 2 vagas

💰 R$ 20

O sistema deverá mostrar:

2 VAGAS DISPONÍVEIS

[ ENTRAR NO JOGO ]

10. TELA INICIAL DO JOGADOR

Criar uma Home muito simples.

Mostrar:

“Olá, [Nome] 👋”

“Quer jogar hoje?”

Botões:

⚽ Futebol ⚽ Futsal 🏐 Vôlei 🏀 Basquete 🎾 Tênis 🏸 Outros

Localização:

📍 Minha localização

Depois:

JOGOS PERTO DE VOCÊ

Cards das partidas.

Cada card deve apresentar:

Esporte

Local

Distância

Horário

Vagas

Valor

Nível

Avaliação do organizador

Botão:

[ QUERO JOGAR ]

11. NOTIFICAÇÃO AUTOMÁTICA

Quando surgir uma partida compatível, enviar:

PUSH

“⚽ Encontramos um jogo para você!”

Futsal Green Ball Hoje às 20h 2 vagas disponíveis

[ ENTRAR ]

Preparar também integração com WhatsApp.

12. WHATSAPP

Preparar arquitetura para integração oficial com WhatsApp Business/API.

Quando existir uma oportunidade:

“⚽ NOVA VAGA DISPONÍVEL

Futsal Green Ball Hoje às 20h 2 vagas

Você quer jogar?

[ENTRAR NO JOGO]”

Não utilizar soluções não oficiais.

13. CONFIRMAÇÃO DO JOGADOR

Quando o jogador entrar:

Mostrar:

VOCÊ ESTÁ CONFIRMADO! ✅

Futsal Green Ball Hoje 20h

Sua posição: 13º jogador

Status:

🟢 Confirmado

Mostrar também:

Endereço

Mapa

Horário

Valor

Regras da partida

Contato do organizador, conforme permissões

14. JOGO COMPLETO

Quando atingir o número máximo:

🟢 JOGO COMPLETO

14/14 jogadores

Bloquear novas entradas.

Se alguém sair:

🔔 VAGA DISPONÍVEL

O sistema poderá procurar automaticamente o próximo jogador compatível da lista de espera.

15. LISTA DE ESPERA

Criar recurso:

ENTRAR NA LISTA DE ESPERA

Se a partida estiver cheia:

“Quer ser avisado caso apareça uma vaga?”

[ ENTRAR NA LISTA ]

Se alguém cancelar, notificar automaticamente o próximo jogador.

16. PAGAMENTOS

Preparar integração com plataforma de pagamentos.

Estados:

Pendente Aguardando pagamento Pago Cancelado Reembolsado

Registrar:

Usuário

Partida

Organizador

Valor

Taxa

Data

Status

Não armazenar dados sensíveis de cartão diretamente no aplicativo.

17. REEMBOLSO

Criar política configurável.

Possibilidades:

Partida cancelada

Quadra indisponível

Organizador cancelou

Problema operacional

Pagamento duplicado

Situações previstas nos termos da plataforma

Criar painel administrativo para processar ou acompanhar reembolsos.

18. AVALIAÇÕES

Após a partida:

COMO FOI?

Avaliar:

⭐ Pontualidade ⭐ Respeito ⭐ Fair Play ⭐ Comportamento

Criar avaliação geral.

Mostrar:

⭐ 4,9

Também permitir avaliação do organizador.

19. SEGURANÇA

Criar:

Verificação de e-mail

Verificação de telefone

Verificação de WhatsApp

Verificação de identidade

Denúncias

Bloqueio

Avaliações

Histórico

Moderação

Preparar arquitetura para integração futura com serviços de verificação de antecedentes criminais, quando legalmente permitido e aplicável.

Respeitar LGPD e princípios de minimização de dados.

Não expor informações pessoais sensíveis publicamente.

20. DENÚNCIAS

Criar:

DENUNCIAR

Motivos:

Agressão

Assédio

Discriminação

Violência

Fraude

Golpe

Perfil falso

Não comparecimento

Comportamento inadequado

Outro

Criar painel administrativo para análise.

21. SISTEMA DE BANIMENTO

Criar níveis:

Advertência

Suspensão temporária

Suspensão prolongada

Banimento definitivo

Casos graves poderão resultar em suspensão imediata.

Criar histórico de penalidades.

Evitar que usuários criem novas contas simplesmente para burlar punições.

Permitir recurso administrativo quando aplicável.

22. ORGANIZADOR

Dashboard:

MEUS JOGOS

Mostrar:

Próximas partidas Jogadores Vagas Pagamentos Lista de espera Cancelamentos

Exemplo:

FUTSAL

Green Ball

Hoje 20h

12/14

🟢 2 vagas

[ ABRIR VAGAS ]

[ GERENCIAR JOGADORES ]

[ PAGAMENTOS ]

23. ORGANIZADOR: ASSINATURA

Criar tela:

ESCOLHA SEU PLANO

GRATUITO

R$ 0

Recursos básicos.

ORGANIZADOR PRO

R$ XX/mês

Recursos avançados.

PROFISSIONAL

R$ XX/mês

Para organizadores com grande volume.

Os valores devem ser facilmente configuráveis pelo administrador.

24. ARENA / EMPRESA

Criar perfil empresarial posteriormente.

A arena poderá:

Cadastrar quadras

Cadastrar endereço

Informar modalidades

Informar horários

Divulgar disponibilidade

Gerenciar partidas

Gerenciar reservas

Visualizar jogadores

Criar promoções

Criar plano profissional para arenas.

25. MAPA

Criar mapa de partidas.

Marcadores:

⚽ Futsal ⚽ Futebol 🏐 Vôlei

Ao clicar:

Green Ball Futsal 20h 2 vagas R$ 20

[ ENTRAR ]

26. MENU DO JOGADOR

Criar navegação mobile:

🏠 Início

🔎 Encontrar jogo

➕ Criar jogo

📅 Meus jogos

🔔 Notificações

👤 Perfil

27. ADMINISTRADOR

Criar painel administrativo.

Dashboard:

Usuários Jogadores Organizadores Arenas Partidas Vagas Pagamentos Reembolsos Assinaturas Denúncias Banimentos Avaliações

Permitir:

Bloquear

Suspender

Banir

Reativar

Editar partidas

Cancelar partidas

Gerenciar planos

Alterar taxas

Analisar denúncias

Gerenciar reembolsos

28. BANCO DE DADOS

Estruturar banco de dados com:

users profiles players organizers arenas courts sports matches match_slots match_players availability waitlists payments refunds subscriptions ratings reports bans notifications locations

Criar relacionamentos e integridade referencial.

29. SEGURANÇA TÉCNICA

Implementar:

Autenticação segura

Controle de acesso por função

RLS/permissões no banco

Validação de dados

Proteção de endpoints

Logs administrativos

Proteção contra duplicidade

Controle de sessões

Não armazenar dados de cartão

Preparação para LGPD

Criar roles:

player organizer arena admin

30. DESIGN

Criar design:

Moderno Esportivo Minimalista Premium Mobile-first

A interface precisa ser rápida e extremamente simples.

Não sobrecarregar a Home.

O usuário precisa conseguir encontrar um jogo em poucos toques.

Usar:

Cards

Mapas

Ícones

Status de vagas

Indicadores visuais

Botões grandes

Navegação inferior mobile

Criar identidade visual própria.

Não copiar Uber, 99 ou outros aplicativos.

31. TELA PRINCIPAL

A Home deve priorizar:

“JOGOS PERTO DE VOCÊ”

E:

“VOCÊ ESTÁ DISPONÍVEL?”

[ ATIVAR DISPONIBILIDADE ]

Depois:

“ENCONTRAMOS UM JOGO PARA VOCÊ”

Partidas compatíveis.

32. FLUXO COMPLETO

ORGANIZADOR

Login ↓ Criar partida ↓ Selecionar esporte ↓ Local ↓ Horário ↓ Número de jogadores ↓ Vagas ↓ Valor ↓ Publicar ↓ Sistema encontra jogadores ↓ Jogadores entram ↓ Pagamento ↓ Jogo completo ↓ Avaliações

JOGADOR

Cadastro ↓ Localização ↓ Esporte ↓ Disponibilidade ↓ Match ↓ Notificação ↓ Visualizar partida ↓ Entrar ↓ Pagamento, se aplicável ↓ Confirmação ↓ Jogar ↓ Avaliar

33. PRINCIPAL DIFERENCIAL

Não posicionar o produto simplesmente como:

“Aplicativo para encontrar pelada.”

Posicionar como:

“Uma plataforma que conecta vagas esportivas a pessoas que querem jogar.”

A essência:

TEM UMA VAGA.

TEM ALGUÉM QUERENDO JOGAR.

A GENTE FAZ O MATCH.

34. MVP — NÃO CONSTRUIR TUDO DE UMA VEZ

A primeira versão deve priorizar:

Cadastro

Login

Perfil

Localização

Futebol

Futsal

Disponibilidade

Criar partida

Abrir vagas

Encontrar partida

Match

Entrar no jogo

Notificação

WhatsApp preparado

Confirmação

Pagamento preparado

Avaliação

Denúncia

Administração básica

Assinatura, pagamentos completos, verificação avançada e expansão para outros esportes podem ser ativados por etapas.

35. PRINCIPAL TESTE DO PRODUTO

O MVP precisa provar somente isto:

ORGANIZADOR:

“Faltam 2 jogadores.”

↓

SISTEMA:

“Encontramos jogadores compatíveis.”

↓

JOGADOR:

“Quero jogar.”

↓

MATCH

↓

PAGAMENTO/CONFIRMAÇÃO

↓

JOGO COMPLETO.

Se isso funcionar rapidamente no Green Ball, temos o núcleo do produto.

36. EXPERIÊNCIA FINAL

O aplicativo deve transmitir:

CONFIANÇA VELOCIDADE SEGURANÇA COMUNIDADE ESPORTE TECNOLOGIA

A experiência deve ser simples o suficiente para uma pessoa abrir o aplicativo e, em poucos segundos, entender:

“Onde tem jogo?” “Tem vaga?” “Quanto custa?” “Posso entrar?”

E para o organizador:

“Tenho vaga.” “Quero jogadores.” “O sistema encontrou.”

Construir primeiro o MVP funcional.

Priorizar a experiência mobile.

Criar arquitetura modular e escalável.

Não criar funcionalidades desnecessárias antes de validar o fluxo principal.

O produto deve nascer localmente, ser validado no União dos Cegos/Green Ball e posteriormente preparado para expansão nacional e internacional.

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/c423e6cc-bc12-4f0e-afd5-5a85dd27191e).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
