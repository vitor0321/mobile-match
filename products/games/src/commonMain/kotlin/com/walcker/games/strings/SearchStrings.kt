package com.walcker.games.strings

internal data class SearchStrings(
    val title: String,
    val subtitle: String,
    val placeholder: String,
    val emptyForQuery: (String) -> String,
    /** Result count above the list: "8 partidas encontradas". */
    val resultsCount: (Int) -> String,
    /** Shown before the user types anything — the list is not empty, it has not started. */
    val idlePrompt: String,
    val filtersTitle: String,
    val filterSport: String,
    val filterDate: String,
    val filterPrice: String,
    val allSports: String,
    /** Placeholder for a filter with nothing chosen yet. */
    val filterAny: String,
    /** Filters that exist in the state but have no UI yet. */
    val comingSoon: String,
    val clearFilters: String,
    val applyFilters: String,
    // Accessibility labels. Every one of these used to be a hardcoded pt-BR literal
    // in the screen, or missing entirely.
    val openFiltersContentDescription: String,
    val clearQueryContentDescription: String,
)

internal val searchStringsEn = SearchStrings(
    title = "Search matches",
    subtitle = "Search by venue or sport",
    placeholder = "Search by court, neighborhood or sport",
    emptyForQuery = { q -> "No matches found for \"$q\"." },
    resultsCount = { n -> if (n == 1) "1 match found" else "$n matches found" },
    idlePrompt = "Search for a court, a neighborhood or a sport.",
    filtersTitle = "Filters",
    filterSport = "Sport",
    filterDate = "Time",
    filterPrice = "Price",
    allSports = "All",
    filterAny = "Any",
    comingSoon = "Coming soon",
    clearFilters = "Clear",
    applyFilters = "Apply filters",
    openFiltersContentDescription = "Open filters",
    clearQueryContentDescription = "Clear search",
)

internal val searchStringsPt = SearchStrings(
    title = "Buscar partidas",
    subtitle = "Pesquise por local ou esporte",
    placeholder = "Buscar por quadra, bairro ou esporte",
    emptyForQuery = { q -> "Nenhuma partida encontrada para \"$q\"." },
    resultsCount = { n -> if (n == 1) "1 partida encontrada" else "$n partidas encontradas" },
    idlePrompt = "Busque por uma quadra, um bairro ou um esporte.",
    filtersTitle = "Filtros",
    filterSport = "Esporte",
    filterDate = "Horário",
    filterPrice = "Preço",
    allSports = "Todos",
    filterAny = "Qualquer",
    comingSoon = "Em breve",
    clearFilters = "Limpar",
    applyFilters = "Aplicar filtros",
    openFiltersContentDescription = "Abrir filtros",
    clearQueryContentDescription = "Limpar busca",
)
