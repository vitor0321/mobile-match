package com.walcker.match.cedar.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale derived from the Figma redesign.
 *
 * The mockup uses ten sizes (10, 11, 12, 13, 14, 15, 16, 18, 24, 28) with
 * `line-height: normal` everywhere, which renders differently on Android and iOS.
 * Collapsed here into eight roles with explicit line heights.
 *
 * Two deliberate departures from the file:
 * - **12sp floor.** The Figma sets badge text at 10sp and 11sp. Below 12sp the
 *   text stops being readable at arm's length on a phone held over a pitch.
 * - **No serif.** The old `bodyVerse` (serif, 17sp, 1.75 line height) was a
 *   leftover from the previous product and had nothing to do with this one.
 *
 * @param fontFamily Inter, once packaged in `composeResources/font/`. Until then
 *   the platform default is used, which is why Android and iOS still look like
 *   two different apps.
 */
public fun cedarTypography(fontFamily: FontFamily = FontFamily.Default): Typography =
    Typography(
        // "Temos Jogo!" — the one celebratory moment in the app.
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp,
        ),
        // Screen titles: "Buscar partidas", "Meu perfil".
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.4).sp,
        ),
        // The single most important fact on a screen: "Hoje, 20:30".
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.2).sp,
        ),
        // Section headers: "Estatísticas", "Participantes", "Filtros".
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        ),
        // Search field text and other input-sized content.
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        // Card title: venue name.
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        // Default body: "R$ 25 por jogador", "⚽ Futebol · Intermediário".
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        // Card metadata: "Hoje · 20:30".
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        // Buttons.
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        ),
        // Field labels above a value: "Esporte", "Horário", "Nível".
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp,
        ),
        // Badges: "2 vagas". Raised from the Figma's 10sp.
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.2.sp,
        ),
    )
