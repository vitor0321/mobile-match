package com.walcker.games.features.domain.model

internal enum class RatingDimension(
    val wireName: String,
) {
    PUNCTUALITY("punctuality"),
    RESPECT("respect"),
    FAIR_PLAY("fairPlay"),
    BEHAVIOR("behavior"),
    ;

    val averageField: String get() = "${wireName}Average"
}

internal data class DimensionAverage(
    val average: Float,
    val count: Int,
)

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

    val isComplete: Boolean get() = RatingDimension.entries.all { it in answers }

    operator fun get(dimension: RatingDimension): Int? = answers[dimension]

    fun with(dimension: RatingDimension, stars: Int?): RatingDimensions = RatingDimensions(
        answers = if (stars == null) answers - dimension else answers + (dimension to stars),
    )

    companion object {
        val VALID_RANGE = 1..5
        val None = RatingDimensions()
    }
}
