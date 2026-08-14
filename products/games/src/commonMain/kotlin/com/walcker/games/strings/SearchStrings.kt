package com.walcker.games.strings

internal data class SearchStrings(
    val title: String,
    val subtitle: String,
    val placeholder: String,
    val emptyForQuery: (String) -> String,
    val joinSuccess: String,
    val joinError: String,
)

internal val searchStringsEn = SearchStrings(
    title = "Search matches",
    subtitle = "Search by venue or sport",
    placeholder = "Search by court, neighborhood or sport",
    emptyForQuery = { q -> "No matches found for \"$q\"." },
    joinSuccess = "You joined the match.",
    joinError = "Could not join the match.",
)

internal val searchStringsPt = SearchStrings(
    title = "Buscar partidas",
    subtitle = "Pesquise por local ou esporte",
    placeholder = "Buscar por quadra, bairro ou esporte",
    emptyForQuery = { q -> "Nenhuma partida encontrada para \"$q\"." },
    joinSuccess = "Você entrou no jogo.",
    joinError = "Não foi possível entrar no jogo.",
)
