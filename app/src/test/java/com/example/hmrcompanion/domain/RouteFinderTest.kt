package com.example.hmrcompanion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RouteFinderTest {

    private lateinit var redLine: MetroLine
    private lateinit var routeFinder: RouteFinder

    @Before
    fun setup() {
        val stations = listOf(
            Station(11, "Ameerpet", 17.4374, 78.4482, "Blue Line"),
            Station(12, "Punjagutta", 17.4254, 78.4525, null),
            Station(13, "Irrum Manzil", 17.4198, 78.4584, null),
            Station(14, "Khairatabad", 17.4119, 78.4609, null),
            Station(15, "Lakdi-ka-pul", 17.4047, 78.4655, null),
            Station(16, "Assembly", 17.4022, 78.4737, null),
            Station(17, "Nampally", 17.3947, 78.4762, null),
            Station(18, "Gandhi Bhavan", 17.3886, 78.4818, null),
            Station(19, "Osmania Medical College", 17.3811, 78.4845, null),
            Station(20, "MG Bus Station (MGBS)", 17.3753, 78.4825, "Green Line"),
            Station(21, "Malakpet", 17.3724, 78.4988, null),
            Station(22, "New Market", 17.3717, 78.5045, null),
            Station(23, "Musarambagh", 17.3692, 78.5126, null),
            Station(24, "Dilsukhnagar", 17.3687, 78.5247, null)
        )
        redLine = MetroLine("red", "Red Line", listOf("Miyapur", "LB Nagar"), stations)
        routeFinder = RouteFinder(redLine)
    }

    @Test
    fun `test findRoute forward`() {
        val route = routeFinder.findRoute("Ameerpet", "Dilsukhnagar")
        assertEquals(14, route.size)
        assertEquals("Ameerpet", route.first().name)
        assertEquals("Dilsukhnagar", route.last().name)
        assertEquals("Punjagutta", route[1].name)
    }

    @Test
    fun `test findRoute backward`() {
        val route = routeFinder.findRoute("Dilsukhnagar", "Ameerpet")
        assertEquals(14, route.size)
        assertEquals("Dilsukhnagar", route.first().name)
        assertEquals("Ameerpet", route.last().name)
        assertEquals("Musarambagh", route[1].name)
    }

    @Test
    fun `test findRoute unknown from station throws exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            routeFinder.findRoute("Unknown Station", "Ameerpet")
        }
        assertEquals(true, exception.message?.contains("Station not found on this line: Unknown Station"))
    }

    @Test
    fun `test findRoute unknown to station throws exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            routeFinder.findRoute("Ameerpet", "Unknown Station")
        }
        assertEquals(true, exception.message?.contains("Station not found on this line: Unknown Station"))
    }

    @Test
    fun `test nextStation with null lastConfirmedIndex`() {
        val route = routeFinder.findRoute("Ameerpet", "Dilsukhnagar")
        val next = routeFinder.nextStation(route, null)
        assertEquals("Punjagutta", next?.name)
    }

    @Test
    fun `test nextStation with valid lastConfirmedIndex`() {
        val route = routeFinder.findRoute("Ameerpet", "Dilsukhnagar")
        val next = routeFinder.nextStation(route, 5) // Last confirmed Assembly
        assertEquals("Nampally", next?.name)
    }

    @Test
    fun `test nextStation at end of route`() {
        val route = routeFinder.findRoute("Ameerpet", "Dilsukhnagar")
        val next = routeFinder.nextStation(route, 13) // Last confirmed Dilsukhnagar
        assertNull(next)
    }

    @Test
    fun `test nextStation on single station route`() {
        val route = routeFinder.findRoute("Ameerpet", "Ameerpet")
        val next = routeFinder.nextStation(route, null)
        assertNull(next)
    }

    @Test
    fun `test nextStation on empty route`() {
        val next = routeFinder.nextStation(emptyList(), null)
        assertNull(next)
    }
}
