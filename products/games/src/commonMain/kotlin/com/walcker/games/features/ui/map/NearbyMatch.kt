package com.walcker.games.features.ui.map

import com.walcker.games.features.domain.model.Game

/**
 * Uma partida com a distância até o usuário, em quilômetros.
 *
 * O cálculo **não** mora aqui. Este arquivo tinha uma `calculateDistance`
 * própria, e ela estava errada de três formas ao mesmo tempo:
 *
 * 1. Usava `Math.toRadians`, que é `java.lang.Math` — só existe na JVM. Bastava
 *    isso para o alvo iOS não compilar, e o CI não pegava porque nenhum job
 *    compila iOS (ver `.github/workflows/pull-request.yml`).
 * 2. Fechava a fórmula de haversine com `2 * acos(sqrt(a))` em vez de
 *    `2 * atan2(sqrt(h), sqrt(1-h))`. Duas partidas na mesma quadra davam
 *    **20.015 km**, e a lista de próximas saía na ordem exatamente inversa —
 *    a mais distante em primeiro.
 * 3. Duplicava `com.walcker.match.core.geo.distanceKm`, que já existia, já
 *    estava correta e já era usada pelo resto do app. A própria tela do mapa
 *    já importava o `formatDistance` do mesmo pacote — formatava com capricho
 *    um número errado.
 *
 * Use `distanceKm` de `core.geo`. Uma só implementação de haversine no projeto.
 */
internal data class NearbyMatch(
    val game: Game,
    val distanceKm: Double,
)
