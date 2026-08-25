package com.walcker.match.cedar.tokens

import androidx.compose.ui.graphics.Color

/**
 * Raw colour values extracted from the Figma redesign
 * ("SportsMatch — Mobile Marketplace Redesign", nodes 1:2, 1:33, 1:56, 1:80, 1:92).
 *
 * This is the only file in the app allowed to hold literal colours. Screens read
 * semantic colours from [androidx.compose.material3.MaterialTheme.colorScheme] or
 * from `CedarTokens.colors`.
 *
 * Naming follows the usual tonal convention: higher number = darker in light mode.
 * Values marked "figma" came straight from the file; the rest are derived to fill
 * gaps the mockup left open (dark mode, disabled states, outlines).
 */
internal object CedarPalette {

    // ── Ink — text and icons ──────────────────────────────────────────────────

    /** figma #091729 — headings and body text. 18:1 on white. */
    val Ink900: Color = Color(0xFF091729)

    /** Derived — secondary text that still needs weight. 9.5:1 on white. */
    val Ink700: Color = Color(0xFF3D4A5C)

    /** figma #637385 — metadata, field labels. 4.6:1 on Canvas: AA, no headroom. */
    val Ink500: Color = Color(0xFF637385)

    /** Derived — disabled text and icons. Never for content that must be read. */
    val Ink300: Color = Color(0xFF94A3B8)

    // ── Blue — action ─────────────────────────────────────────────────────────

    /** Derived — pressed state and blue text on light surfaces. 6.2:1 with white. */
    val Blue700: Color = Color(0xFF0F5AD1)

    /** figma #146bf2 — primary actions, links, selected states. 4.8:1 with white. */
    val Blue600: Color = Color(0xFF146BF2)

    /** Derived — primary in dark mode, where the 600 is too heavy. */
    val Blue400: Color = Color(0xFF5B9BFF)

    /** Derived — tinted container (selected chip, informational card). */
    val Blue100: Color = Color(0xFFDCE9FE)

    /** Derived — dark-mode container. */
    val Blue900: Color = Color(0xFF0A2E6B)

    // ── Green — availability, not action ──────────────────────────────────────
    //
    // In the redesign green means "there are slots left" and nothing else. Using it
    // for buttons too is what made the old theme unreadable: the user could not tell
    // "press me" from "hurry up".

    /**
     * Derived — green as *text* on a light surface, and green as `primary` under
     * [CedarBrand.Green]. 5.4:1 with white either way.
     *
     * The figma green (#29d178) is 2.0:1 as text on white: it works as a background
     * and nowhere else. The old theme's #06C167 was 2.4:1, same problem.
     */
    val Green700: Color = Color(0xFF087A41)

    /** figma #29d178 — the availability pill and the confirmation CTA. */
    val Green500: Color = Color(0xFF29D178)

    /** Derived — availability in dark mode. */
    val Green400: Color = Color(0xFF34D983)

    /** Derived — soft availability container. */
    val Green100: Color = Color(0xFFDFF8EC)

    /** Derived — dark-mode container. */
    val Green900: Color = Color(0xFF06371F)

    // ── Surfaces ──────────────────────────────────────────────────────────────

    /** figma #f5faff — screen background. The faint blue is what gives white cards depth. */
    val Canvas: Color = Color(0xFFF5FAFF)

    /** figma #ffffff — cards, sheets, app bars. */
    val Surface: Color = Color(0xFFFFFFFF)

    /** figma #f7fcff — a card nested inside another card. */
    val SurfaceSubtle: Color = Color(0xFFF7FCFF)

    /** Derived — hairline dividers. Decorative, so the 3:1 rule does not apply. */
    val Outline: Color = Color(0xFFC9D9E8)

    /**
     * Derived — the boundary of a control: borda de campo, cartão selecionado,
     * estrela vazia.
     *
     * Era `#C9D9E8`, **1,44:1** com branco. WCAG 1.4.11 pede 3:1 para o contorno que
     * identifica um componente, e o contorno de um `OutlinedTextField` é exatamente
     * isso — sem ele não dá para ver onde o campo começa. Agora 3,58:1, ainda mais
     * claro que o `outline` padrão do Material (4,3:1).
     */
    val OutlineStrong: Color = Color(0xFF6F8AA6)

    // ── Map and media ─────────────────────────────────────────────────────────

    /** figma #e8f0f7 — map base while tiles load. */
    val MapBase: Color = Color(0xFFE8F0F7)

    /** figma #d6e0e8 — streets on the map placeholder. */
    val MapLine: Color = Color(0xFFD6E0E8)

    /** figma #b8c7d1 — venue photo placeholder. */
    val ImagePlaceholder: Color = Color(0xFFB8C7D1)

    // ── Error ─────────────────────────────────────────────────────────────────
    //
    // Replaces the old ErrorBgDark = Color(0xFFF3A1D1B) — nine hex digits, which is
    // not the colour anyone meant.

    val Red600: Color = Color(0xFFD92D20)
    val Red100: Color = Color(0xFFFEE4E2)
    val Red900: Color = Color(0xFF7A271A)
    val Red400: Color = Color(0xFFFF8A80)

    // ── Dark mode ─────────────────────────────────────────────────────────────
    //
    // The Figma has no dark mode. These are derived by keeping the blue-leaning
    // neutral of the light theme and inverting the ramp. Review screen by screen
    // (Phase 4) before shipping.

    val CanvasDark: Color = Color(0xFF0B1220)
    val SurfaceDark: Color = Color(0xFF131E30)
    val SurfaceSubtleDark: Color = Color(0xFF1A2739)
    val OutlineDark: Color = Color(0xFF2C3A50)

    /** Era `#44566F`, 2,23:1 sobre `SurfaceDark`. Agora 3,68:1. */
    val OutlineStrongDark: Color = Color(0xFF627893)

    val InkDark900: Color = Color(0xFFE8EEF7)
    val InkDark500: Color = Color(0xFF9FB0C4)
    val InkDark300: Color = Color(0xFF64748B)

    val MapBaseDark: Color = Color(0xFF16233A)
    val MapLineDark: Color = Color(0xFF243248)
    val ImagePlaceholderDark: Color = Color(0xFF2B3B50)
}
