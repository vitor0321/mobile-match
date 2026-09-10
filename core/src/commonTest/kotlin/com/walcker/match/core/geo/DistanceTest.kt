package com.walcker.match.core.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistanceTest {
    @Test
    fun `haversine symmetry`() {
        val a = Coordinates(lat = -23.5505, lng = -46.6333)
        val b = Coordinates(lat = -22.9068, lng = -43.1729)
        val d1 = distanceKm(a, b)
        val d2 = distanceKm(b, a)
        assertEquals(d1, d2)
    }

    @Test
    fun `known distances`() {
        val equator = Coordinates(lat = 0.0, lng = 0.0)
        val ninetyEast = Coordinates(lat = 0.0, lng = 90.0)
        val d = distanceKm(equator, ninetyEast)
        assertTrue(d > 10000.0 && d < 10020.0)

        val pole = Coordinates(lat = 90.0, lng = 0.0)
        val d2 = distanceKm(equator, pole)
        assertTrue(d2 > 9990.0 && d2 < 10010.0)
    }
}
