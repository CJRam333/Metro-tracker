package com.example.hmrcompanion.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun `test distance between Ameerpet and Dilsukhnagar`() {
        // Coordinates from hmr_stations.json
        // Ameerpet: 17.4374, 78.4482
        // Dilsukhnagar: 17.3687, 78.5247
        val lat1 = 17.4374
        val lng1 = 78.4482
        val lat2 = 17.3687
        val lng2 = 78.5247

        val distance = distanceMeters(lat1, lng1, lat2, lng2)

        // Expected distance roughly 11.14 km
        assertEquals(11146.0, distance, 50.0)
    }

    @Test
    fun `test distance between JBS Parade Ground and MGBS`() {
        // Coordinates from hmr_stations.json
        // JBS Parade Ground: 17.4516, 78.4992
        // MGBS: 17.3753, 78.4825
        val lat1 = 17.4516
        val lng1 = 78.4992
        val lat2 = 17.3753
        val lng2 = 78.4825

        val distance = distanceMeters(lat1, lng1, lat2, lng2)

        // Expected distance roughly 8.66 km
        assertEquals(8660.0, distance, 50.0)
    }

    @Test
    fun `test zero distance`() {
        val lat = 17.4374
        val lng = 78.4482
        val distance = distanceMeters(lat, lng, lat, lng)
        assertEquals(0.0, distance, 0.001)
    }
}
