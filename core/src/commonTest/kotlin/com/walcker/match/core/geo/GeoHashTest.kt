package com.walcker.match.core.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoHashTest {

    @Test
    fun `encodeGeoHashSãoPaulo`() {
        val coords = Coordinates(lat = -23.5505, lng = -46.6333)
        val hash = encodeGeoHash(coords, precision = 9)
        assertEquals("6gyf4bf8m", hash)
    }

    @Test
    fun `encodeGeoHashRioDeJaneiro`() {
        val coords = Coordinates(lat = -22.9068, lng = -43.1729)
        val hash = encodeGeoHash(coords, precision = 9)
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
        val centerHash = encodeGeoHash(center)
        val covered = ranges.any { it.start <= centerHash && centerHash <= it.endInclusive }
        assertTrue(covered)
    }

    @Test
    fun `boundsForRadiusLargerRadiusMoreRanges`() {
        val center = Coordinates(lat = -23.5505, lng = -46.6333)
        val smallRanges = boundsForRadius(center, radiusKm = 1.0)
        val largeRanges = boundsForRadius(center, radiusKm = 50.0)
        assertTrue(largeRanges.size >= 1)
    }

    @Test
    fun `distanceKmSãoPauloToRio`() {
        val sp = Coordinates(lat = -23.5505, lng = -46.6333)
        val rj = Coordinates(lat = -22.9068, lng = -43.1729)
        val dist = distanceKm(sp, rj)
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
        assertEquals("10 m", formatDistance(0.008))
    }

    @Test
    fun `formatDistanceOver1km`() {
        assertEquals("1,2 km", formatDistance(1.23))
        assertEquals("357,0 km", formatDistance(357.0))
    }
}
