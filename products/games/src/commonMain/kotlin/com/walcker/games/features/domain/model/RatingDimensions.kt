package com.walcker.games.features.domain.model

/**
 * As quatro dimensões da avaliação pós-partida.
 *
 * Todas são **obrigatórias**: `parseRatingDimensions` em
 * `functions/src/moderation.ts` recusa payload sem qualquer uma delas, com a
 * mesma mensagem de valor fora da faixa — ausente e inválido são o mesmo erro,
 * de propósito, para não gravar avaliação pela metade.
 *
 * Como toda avaliação traz as quatro, elas não têm contador próprio: o
 * denominador é o `ratingCount` do perfil, o mesmo da nota principal.
 */
internal enum class RatingDimension(
    /**
     * Nome do campo no Firestore e na chamada da callable. Tem de bater
     * exatamente com `RATING_DIMENSIONS` em `functions/src/moderation.ts` — não dá
     * para derivar de [name], que é `FAIR_PLAY` e não `fairPlay`.
     */
    val wireName: String,
) {
    PUNCTUALITY("punctuality"),
    RESPECT("respect"),
    FAIR_PLAY("fairPlay"),
    BEHAVIOR("behavior"),
    ;

    /**
     * Média agregada em `profiles/{uid}`. Não existe `<dim>Count`: a contagem é
     * o `ratingCount` do perfil, compartilhado com a nota principal.
     */
    val averageField: String get() = "${wireName}Average"
}

/**
 * Agregado de uma dimensão no perfil. [count] é o `ratingCount` do perfil, e
 * não um contador por dimensão — como toda avaliação traz as quatro, elas
 * caminham juntas e um contador separado seria sempre igual.
 */
internal data class DimensionAverage(
    val average: Float,
    val count: Int,
)

/**
 * Respostas às dimensões enquanto o formulário está sendo preenchido. Dimensão
 * ausente é "ainda não respondeu" — estado intermediário legítimo na tela, mas
 * **não** payload válido: só [isComplete] pode ser enviado.
 *
 * A faixa 1..5 é validada aqui, na construção, e não só na tela: o mesmo
 * `parseRatingDimensions` das Functions recusa fora da faixa, e é melhor
 * estourar no cliente do que descobrir pelo `INVALID_ARGUMENT` da callable.
 */
internal data class RatingDimensions(
    val answers: Map<RatingDimension, Int> = emptyMap(),
) {
    init {
        val invalid = answers.filterValues { it !in VALID_RANGE }
        require(invalid.isEmpty()) {
            "Dimensões fora de 1..5: $invalid"
        }
    }

    val isEmpty: Boolean get() = answers.isEmpty()

    /**
     * As quatro respondidas. É a pré-condição para enviar: o servidor recusa
     * qualquer coisa menos que isso, então a tela trava o botão antes.
     */
    val isComplete: Boolean get() = RatingDimension.entries.all { it in answers }

    operator fun get(dimension: RatingDimension): Int? = answers[dimension]

    /**
     * Marca ou desmarca uma dimensão. `stars` nulo remove a resposta, que é o
     * que o toque na estrela já selecionada precisa fazer para o usuário
     * conseguir voltar atrás de "respondi sem querer".
     */
    fun with(dimension: RatingDimension, stars: Int?): RatingDimensions = RatingDimensions(
        answers = if (stars == null) answers - dimension else answers + (dimension to stars),
    )

    companion object {
        val VALID_RANGE = 1..5
        val None = RatingDimensions()
    }
}
