package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DropTargetResolverTest {
    private val teamA = Rect(offset = Offset(0f, 0f), size = Size(100f, 100f))
    private val teamB = Rect(offset = Offset(0f, 200f), size = Size(100f, 100f))
    private val sectionBounds = mapOf(0 to teamA, 1 to teamB)

    @Test
    fun `a point inside a section resolves to that section's index`() {
        assertEquals(0, resolveDropTarget(Offset(50f, 50f), sectionBounds))
        assertEquals(1, resolveDropTarget(Offset(50f, 250f), sectionBounds))
    }

    @Test
    fun `a point between sections resolves to no target`() {
        assertNull(resolveDropTarget(Offset(50f, 150f), sectionBounds))
    }

    @Test
    fun `a point outside every section resolves to no target`() {
        assertNull(resolveDropTarget(Offset(-10f, -10f), sectionBounds))
    }

    @Test
    fun `an empty section map always resolves to no target`() {
        assertNull(resolveDropTarget(Offset(50f, 50f), emptyMap()))
    }
}
