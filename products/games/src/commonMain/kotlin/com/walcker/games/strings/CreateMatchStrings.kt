package com.walcker.games.strings

import com.walcker.games.features.domain.shared.model.RecurrenceOption

internal data class CreateMatchStrings(
    val title: String,
    val subtitle: String,
    val editTitle: String,
    val editSubtitle: String,
    val backContentDescription: String,
    val saveChanges: String,
    val updateSuccess: String,
    val sectionWhen: String,
    val sectionDetails: String,
    val matchNameLabel: String,
    val matchNamePlaceholder: String,
    val sportLabel: String,
    val neighborhoodLabel: String,
    val cityLabel: String,
    val addressLabel: String,
    val locationLabel: String,
    val chooseLocationLabel: String,
    val confirmLocationLabel: String,
    val pickLocationHint: String,
    val resolvingLocation: String,
    val locationNotResolved: String,
    val searchAddressPlaceholder: String,
    val addressNotFound: String,
    val dateLabel: String,
    val datePlaceholder: String,
    val timeLabel: String,
    val timePlaceholder: String,
    val durationLabel: String,
    val durationValue: (minutes: Int) -> String,
    val playersLabel: String,
    val playersValue: (count: Int) -> String,
    val recurrenceLabel: String,
    val recurrenceOptionLabel: (RecurrenceOption) -> String,
    val recurrenceAutoCreateNotice: String,
    val priceLabel: String,
    val pricePlaceholder: String,
    val priceHelper: String,
    val submit: String,
    val validationError: String,
    val genericError: String,
    val notLoggedIn: String,
    val success: String,
    val confirm: String,
    val cancel: String,
)

internal val createMatchStringsPt =
    CreateMatchStrings(
        title = "Criar partida",
        subtitle = "Preencha os dados da sua partida",
        editTitle = "Editar partida",
        editSubtitle = "Atualize os dados da sua partida",
        backContentDescription = "Voltar",
        saveChanges = "Salvar alterações",
        updateSuccess = "Partida atualizada com sucesso!",
        sectionWhen = "Quando vai ser",
        sectionDetails = "Detalhes da partida",
        matchNameLabel = "Nome da partida",
        matchNamePlaceholder = "Ex: Pelada de sexta",
        sportLabel = "Esporte",
        neighborhoodLabel = "Bairro",
        cityLabel = "Cidade",
        addressLabel = "Endereço",
        locationLabel = "Localização",
        chooseLocationLabel = "Escolher localização",
        confirmLocationLabel = "Confirmar localização",
        pickLocationHint = "Arraste o mapa até o local da partida — pode ser em outra cidade",
        resolvingLocation = "Identificando o endereço…",
        locationNotResolved = "Não foi possível identificar o endereço deste ponto.",
        searchAddressPlaceholder = "Buscar endereço",
        addressNotFound = "Endereço não encontrado. Tente outra busca.",
        dateLabel = "Data",
        datePlaceholder = "Escolher data",
        timeLabel = "Horário",
        timePlaceholder = "Escolher horário",
        durationLabel = "Duração",
        durationValue = { minutes -> "$minutes minutos" },
        playersLabel = "Total de jogadores",
        playersValue = { count -> "$count jogadores" },
        recurrenceLabel = "Repetir",
        recurrenceOptionLabel = { option ->
            when (option) {
                RecurrenceOption.NONE -> "Não repetir"
                RecurrenceOption.DAILY -> "Todo dia"
                RecurrenceOption.WEEKLY -> "Toda semana"
                RecurrenceOption.MONTHLY -> "Todo mês"
                RecurrenceOption.YEARLY -> "Todo ano"
            }
        },
        recurrenceAutoCreateNotice =
            "A próxima partida dessa série é criada automaticamente " +
                "cerca de 1 mês antes da data, com o mesmo local e horário.",
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

internal val createMatchStringsEn =
    CreateMatchStrings(
        title = "Create match",
        subtitle = "Fill in your match details",
        editTitle = "Edit match",
        editSubtitle = "Update your match details",
        backContentDescription = "Back",
        saveChanges = "Save changes",
        updateSuccess = "Match updated successfully!",
        sectionWhen = "When it happens",
        sectionDetails = "Match details",
        matchNameLabel = "Match name",
        matchNamePlaceholder = "e.g. Friday pickup game",
        sportLabel = "Sport",
        neighborhoodLabel = "Neighborhood",
        cityLabel = "City",
        addressLabel = "Address",
        locationLabel = "Location",
        chooseLocationLabel = "Choose location",
        confirmLocationLabel = "Confirm location",
        pickLocationHint = "Drag the map to the match location — it can be in another city",
        resolvingLocation = "Looking up the address…",
        locationNotResolved = "Could not identify the address for this point.",
        searchAddressPlaceholder = "Search address",
        addressNotFound = "Address not found. Try another search.",
        dateLabel = "Date",
        datePlaceholder = "Pick a date",
        timeLabel = "Time",
        timePlaceholder = "Pick a time",
        durationLabel = "Duration",
        durationValue = { minutes -> "$minutes minutes" },
        playersLabel = "Total players",
        playersValue = { count -> "$count players" },
        recurrenceLabel = "Repeat",
        recurrenceOptionLabel = { option ->
            when (option) {
                RecurrenceOption.NONE -> "Don't repeat"
                RecurrenceOption.DAILY -> "Every day"
                RecurrenceOption.WEEKLY -> "Every week"
                RecurrenceOption.MONTHLY -> "Every month"
                RecurrenceOption.YEARLY -> "Every year"
            }
        },
        recurrenceAutoCreateNotice =
            "The next match in this series is created automatically " +
                "about 1 month before its date, with the same place and time.",
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
