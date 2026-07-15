package com.example.hmrcompanion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripProgressManagerTest {

    private lateinit var route: List<Station>
    private lateinit var manager: TripProgressManager

    @Before
    fun setup() {
        route = listOf(
            Station(1, "Station A", 10.0, 10.0, null),
            Station(2, "Station B", 10.01, 10.01, null), // ~1.5km from A
            Station(3, "Station C", 10.02, 10.02, null)  // ~1.5km from B
        )
        manager = TripProgressManager(route, 400.0)
    }

    @Test
    fun `test Travelling event when far from next station`() {
        // Start near Station A, moving towards Station B.
        // Station B is at 10.01, 10.01. Let's place user at 10.005, 10.005
        val event = manager.onLocationUpdate(10.005, 10.005)
        assertTrue(event is TripProgressEvent.Travelling)
        val travellingEvent = event as TripProgressEvent.Travelling
        assertEquals("Station B", travellingEvent.nextStation.name)
        assertTrue(travellingEvent.distanceMeters > 400.0)
        assertEquals(null, manager.lastConfirmedIndex)
    }

    @Test
    fun `test ApproachingIntermediate event when near mid-route station`() {
        // Very close to Station B (but > 150m and <= 400m)
        // Let's place user at 10.008, 10.008 (~314m away)
        val event = manager.onLocationUpdate(10.008, 10.008)
        assertTrue(event is TripProgressEvent.ApproachingIntermediate)
        assertEquals("Station B", (event as TripProgressEvent.ApproachingIntermediate).station.name)
        assertEquals(null, manager.lastConfirmedIndex) // Not yet within 150m
    }

    @Test
    fun `test index auto-advances when within 150m`() {
        // Right on top of Station B
        val event = manager.onLocationUpdate(10.0101, 10.0101) // ~15m away
        assertTrue(event is TripProgressEvent.ApproachingIntermediate)
        assertEquals("Station B", (event as TripProgressEvent.ApproachingIntermediate).station.name)
        assertEquals(1, manager.lastConfirmedIndex)
    }

    @Test
    fun `test ApproachingDestination event when near final station`() {
        // Advance past Station B first
        manager.lastConfirmedIndex = 1

        // Near Station C (final station)
        val event = manager.onLocationUpdate(10.018, 10.018) // ~314m away
        assertTrue(event is TripProgressEvent.ApproachingDestination)
        assertEquals("Station C", (event as TripProgressEvent.ApproachingDestination).station.name)
    }

    @Test
    fun `test RouteComplete when past last station`() {
        manager.lastConfirmedIndex = 2 // Reached Station C
        val event = manager.onLocationUpdate(10.02, 10.02)
        assertEquals(TripProgressEvent.RouteComplete, event)
    }
}
