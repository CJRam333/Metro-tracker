package com.example.hmrcompanion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class MultiLineRouteFinderTest {

    private lateinit var allLines: Map<String, MetroLine>
    private lateinit var multiLineRouteFinder: MultiLineRouteFinder

    @Before
    fun setup() {
        val redStations = listOf(
            Station(1, "Miyapur", 0.0, 0.0, null),
            Station(11, "Ameerpet", 0.0, 0.0, "Blue Line"),
            Station(20, "MG Bus Station (MGBS)", 0.0, 0.0, "Green Line"),
            Station(27, "LB Nagar", 0.0, 0.0, null)
        )
        val redLine = MetroLine("red", "Red Line", emptyList(), redStations)

        val blueStations = listOf(
            Station(2, "HITEC City", 0.0, 0.0, null),
            Station(10, "Ameerpet", 0.0, 0.0, "Red Line"),
            Station(15, "Parade Ground", 0.0, 0.0, "Green Line"),
            Station(23, "Nagole", 0.0, 0.0, null)
        )
        val blueLine = MetroLine("blue", "Blue Line", emptyList(), blueStations)

        val greenStations = listOf(
            Station(2, "Parade Ground", 0.0, 0.0, "Blue Line"),
            Station(9, "Sultan Bazaar", 0.0, 0.0, null),
            Station(10, "MG Bus Station (MGBS)", 0.0, 0.0, "Red Line")
        )
        val greenLine = MetroLine("green", "Green Line", emptyList(), greenStations)

        allLines = mapOf("red" to redLine, "blue" to blueLine, "green" to greenLine)
        multiLineRouteFinder = MultiLineRouteFinder(allLines)
    }

    @Test
    fun `test same line route`() {
        // Miyapur -> LB Nagar (Red)
        val route = multiLineRouteFinder.findRoute("Miyapur", "LB Nagar")
        assertEquals(1, route.segments.size)
        assertEquals(0, route.interchangeStations.size)
        assertEquals(4, route.totalStations)
        assertEquals(4, route.flatStations().size)
    }

    @Test
    fun `test single interchange route`() {
        // Miyapur (Red) -> Nagole (Blue). Interchange at Ameerpet.
        val route = multiLineRouteFinder.findRoute("Miyapur", "Nagole")
        assertEquals(2, route.segments.size)
        assertEquals(listOf("Ameerpet"), route.interchangeStations)
        // Red (Miyapur -> Ameerpet) = 2 stations. Blue (Ameerpet -> Parade -> Nagole) = 3 stations.
        // Total = 2 + 3 - 1 = 4.
        assertEquals(4, route.totalStations)

        val flat = route.flatStations()
        assertEquals(4, flat.size)
        assertEquals(1, flat.count { it.name == "Ameerpet" })
    }

    @Test
    fun `test single interchange but on same line directly`() {
        // Miyapur (Red) -> MG Bus Station (MGBS) (Red+Green). Should resolve to 0 hops on Red.
        val route = multiLineRouteFinder.findRoute("Miyapur", "MG Bus Station (MGBS)")
        assertEquals(1, route.segments.size)
        assertEquals("red", route.segments.first().lineKey)
        assertEquals(0, route.interchangeStations.size)
        assertEquals(3, route.totalStations)
    }

    @Test
    fun `test double interchange route`() {
        // HITEC City (Blue) -> Sultan Bazaar (Green)
        val route = multiLineRouteFinder.findRoute("HITEC City", "Sultan Bazaar")
        // Blue (HITEC -> Ameerpet -> Parade Ground). Interchange at Parade Ground is enough for 1 hop!
        assertEquals(2, route.segments.size)
        assertEquals(listOf("Parade Ground"), route.interchangeStations)
    }

    @Test
    fun `test transfer hint logic`() {
        // Miyapur (Red) -> Nagole (Blue). Interchange at Ameerpet.
        val route = multiLineRouteFinder.findRoute("Miyapur", "Nagole")
        val flat = route.flatStations()
        val ameerpetIndex = flat.indexOfFirst { it.name == "Ameerpet" }

        assertEquals("Change to Blue Line", route.transferHintAt(ameerpetIndex))
        assertNull(route.transferHintAt(ameerpetIndex - 1)) // Miyapur
        assertNull(route.transferHintAt(flat.size - 1)) // Final destination
    }

    @Test
    fun `test transfer hint is null at final destination`() {
        val route = multiLineRouteFinder.findRoute("Miyapur", "LB Nagar")
        val flat = route.flatStations()
        assertNull(route.transferHintAt(flat.size - 1))
    }

    @Test
    fun `test invalid station name throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            multiLineRouteFinder.findRoute("Fake Station", "Miyapur")
        }
    }
}
