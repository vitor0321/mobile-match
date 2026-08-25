package com.walcker.games.strings

/**
 * Textos da criação de partida.
 *
 * Este arquivo já existia, completo e traduzido, e a tela nunca o usou: ela
 * carregava `strings.gameList` e escrevia cada rótulo em pt-BR direto no código.
 * O que faltava aqui eram só os textos que o código inventava inline — os
 * placeholders dos seletores, os botões dos diálogos e as mensagens de resultado.
 */
internal data class CreateMatchStrings(
    val title: String,
    val subtitle: String,
    /** Cabeçalho do primeiro bloco do formulário: onde a partida acontece. */
    val sectionVenue: String,
    /** Cabeçalho do segundo bloco: data, horário e duração. */
    val sectionWhen: String,
    /** Cabeçalho do terceiro bloco: jogadores e preço. */
    val sectionDetails: String,
    val venueNameLabel: String,
    val venueNamePlaceholder: String,
    val sportLabel: String,
    val neighborhoodLabel: String,
    val neighborhoodPlaceholder: String,
    val cityLabel: String,
    val cityPlaceholder: String,
    val addressLabel: String,
    val addressPlaceholder: String,
    val dateLabel: String,
    /** Texto do campo de data enquanto nada foi escolhido. */
    val datePlaceholder: String,
    val timeLabel: String,
    /** Texto do campo de horário enquanto nada foi escolhido. */
    val timePlaceholder: String,
    val durationLabel: String,
    /** Valor da duração já escolhido: "90 minutos". */
    val durationValue: (minutes: Int) -> String,
    val playersLabel: String,
    /** Valor do slider de jogadores: "10 jogadores". */
    val playersValue: (count: Int) -> String,
    val priceLabel: String,
    val pricePlaceholder: String,
    /** Diz que dá para deixar em branco — o campo é opcional e não parecia. */
    val priceHelper: String,
    val submit: String,
    val validationError: String,
    val genericError: String,
    /** Sem sessão não dá para saber de quem é a partida. */
    val notLoggedIn: String,
    val success: String,
    /** Confirmação dos diálogos de data e horário. */
    val confirm: String,
    /** Cancelamento dos diálogos de data e horário. */
    val cancel: String,
)

internal val createMatchStringsPt = CreateMatchStrings(
    title = "Criar partida",
    subtitle = "Preencha os dados da sua partida",
    sectionVenue = "Onde vai ser",
    sectionWhen = "Quando vai ser",
    sectionDetails = "Detalhes da partida",
    venueNameLabel = "Nome da quadra",
    venueNamePlaceholder = "Ex: Arena Central",
    sportLabel = "Esporte",
    neighborhoodLabel = "Bairro",
    neighborhoodPlaceholder = "Ex: Bela Vista",
    cityLabel = "Cidade",
    cityPlaceholder = "Ex: São Paulo",
    addressLabel = "Endereço",
    addressPlaceholder = "Rua, número",
    dateLabel = "Data",
    datePlaceholder = "Escolher data",
    timeLabel = "Horário",
    timePlaceholder = "Escolher horário",
    durationLabel = "Duração",
    durationValue = { minutes -> "$minutes minutos" },
    playersLabel = "Total de jogadores",
    playersValue = { count -> "$count jogadores" },
    priceLabel = "Preço por jogador",
    pricePlaceholder = "Ex: 25,00",
    priceHelper = "Deixe em branco se a partida for gratuita.",
    submit = "Criar e publicar",
    validationError = "Preencha todos os campos obrigatórios para publicar.",
    genericError = "Não foi possível criar a partida. Tente de novo.",
    notLoggedIn = "Entre na sua conta para criar uma partida.",
    success = "Partida criada com sucesso!",
    confirm = "OK",
    cancel = "Cancelar",
)

internal val createMatchStringsEn = CreateMatchStrings(
    title = "Create match",
    subtitle = "Fill in your match details",
    sectionVenue = "Where it happens",
    sectionWhen = "When it happens",
    sectionDetails = "Match details",
    venueNameLabel = "Venue name",
    venueNamePlaceholder = "e.g. Central Arena",
    sportLabel = "Sport",
    neighborhoodLabel = "Neighborhood",
    neighborhoodPlaceholder = "e.g. Downtown",
    cityLabel = "City",
    cityPlaceholder = "e.g. São Paulo",
    addressLabel = "Address",
    addressPlaceholder = "Street, number",
    dateLabel = "Date",
    datePlaceholder = "Pick a date",
    timeLabel = "Time",
    timePlaceholder = "Pick a time",
    durationLabel = "Duration",
    durationValue = { minutes -> "$minutes minutes" },
    playersLabel = "Total players",
    playersValue = { count -> "$count players" },
    priceLabel = "Price per player",
    pricePlaceholder = "e.g. 25.00",
    priceHelper = "Leave it empty if the match is free.",
    submit = "Create and publish",
    validationError = "Fill in every required field to publish.",
    genericError = "Could not create the match. Please try again.",
    notLoggedIn = "Sign in to create a match.",
    success = "Match created successfully!",
    confirm = "OK",
    cancel = "Cancel",
)
