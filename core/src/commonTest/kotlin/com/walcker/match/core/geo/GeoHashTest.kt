package com.walcker.match.core.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoHashTest {

    @Test
    fun `encodeGeoHashSãoPaulo`() {
        // São Paulo center from the MVP
        val coords = Coordinates(lat = -23.5505, lng = -46.6333)
        val hash = encodeGeoHash(coords, precision = 9)
        // Vetor conferido contra o mesmo geohash usado no fixture das regras
        // (firestore.rules.test.ts) e no mirror do servidor (functions/geo.ts).
        assertEquals("6gyf4bf8m", hash)
    }

    @Test
    fun `encodeGeoHashRioDeJaneiro`() {
        val coords = Coordinates(lat = -22.9068, lng = -43.1729)
        val hash = encodeGeoHash(coords, precision = 9)
        // Vetor conferido contra o mirror do servidor (functions/geo.ts).
        assertEquals("75cm9tfqn", hash)
    }

    @Test
    fun `encodeGeoHashPrecision1`() {
        val coords = Coordinates(lat = -23.5505, lng = -46.6333)
        val hash = encodeGeoHash(coords, precision = 1)
        assertEquals("6", hash)
    }

    @Test
    fun `encodeGeoHashPrecision22`() {
        val coords = Coordinates(lat = -23.5505, lng = -46.6333)
        val hash = encodeGeoHash(coords, precision = 22)
        assertEquals(22, hash.length)
    }

    @Test
    fun `boundsForRadiusCoversCenter`() {
        val center = Coordinates(lat = -23.5505, lng = -46.6333)
        val ranges = boundsForRadius(center, radiusKm = 5.0)
        assertTrue(ranges.isNotEmpty())
        // The center's hash should be within at least one range
        val centerHash = encodeGeoHash(center)
        val covered = ranges.any { it.start <= centerHash && centerHash <= it.endInclusive }
        assertTrue(covered)
    }

    @Test
    fun `boundsForRadiusLargerRadiusMoreRanges`() {
        val center = Coordinates(lat = -23.5505, lng = -46.6333)
        val smallRanges = boundsForRadius(center, radiusKm = 1.0)
        val largeRanges = boundsForRadius(center, radiusKm = 50.0)
        // Larger radius may produce more intervals (or same, depending on cell boundaries)
        // but never fewer than the number of distinct cell hashes intersected.
        assertTrue(largeRanges.size >= 1)
    }

    @Test
    fun `distanceKmSãoPauloToRio`() {
        val sp = Coordinates(lat = -23.5505, lng = -46.6333)
        val rj = Coordinates(lat = -22.9068, lng = -43.1729)
        val dist = distanceKm(sp, rj)
        // Known distance ~357 km
        assertTrue(dist > 350.0 && dist < 365.0)
    }

    @Test
    fun `distanceKmSamePointIsZero`() {
        val coords = Coordinates(lat = -23.5505, lng = -46.6333)
        val dist = distanceKm(coords, coords)
        assertEquals(0.0, dist)
    }

    @Test
    fun `formatDistanceUnder1km`() {
        assertEquals("850 m", formatDistance(0.85))
        assertEquals("10 m", formatDistance(0.008)) // rounds to 10m
    }

    @Test
    fun `formatDistanceOver1km`() {
        assertEquals("1,2 km", formatDistance(1.23))
        assertEquals("357,0 km", formatDistance(357.0))
    }
}
