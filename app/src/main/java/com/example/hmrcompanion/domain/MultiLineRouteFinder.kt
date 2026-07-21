package com.example.hmrcompanion.domain

class MultiLineRouteFinder(private val allLines: Map<String, MetroLine>) {

    private val interchanges = listOf(
        Interchange("Ameerpet", setOf("red", "blue")),
        Interchange("MG Bus Station (MGBS)", setOf("red", "green")),
        Interchange("Parade Ground", setOf("blue", "green"))
    )

    private data class Interchange(val name: String, val lines: Set<String>)

    fun findRoute(fromStationName: String, toStationName: String): PlannedRoute {
        val fromLines = findLinesForStation(fromStationName)
        val toLines = findLinesForStation(toStationName)

        if (fromLines.isEmpty()) throw IllegalArgumentException("Station not found in any line: $fromStationName")
        if (toLines.isEmpty()) throw IllegalArgumentException("Station not found in any line: $toStationName")

        val possibleRoutes = mutableListOf<PlannedRoute>()

        // 0-hop: shared lines
        val sharedLines = fromLines.intersect(toLines)
        for (lineKey in sharedLines) {
            val line = allLines[lineKey]!!
            val routeStations = RouteFinder(line).findRoute(fromStationName, toStationName)
            possibleRoutes.add(
                PlannedRoute(
                    segments = listOf(RouteSegment(routeStations, lineKey, line.name)),
                    interchangeStations = emptyList(),
                    totalStations = routeStations.size
                )
            )
        }

        // If a direct route exists, prefer it
        if (possibleRoutes.isNotEmpty()) {
            return possibleRoutes.minByOrNull { it.totalStations }!!
        }

        // 1-hop: single interchange
        for (fromLineKey in fromLines) {
            for (toLineKey in toLines) {
                if (fromLineKey == toLineKey) continue

                val interchange = interchanges.find {
                    it.lines.contains(fromLineKey) && it.lines.contains(toLineKey)
                }

                if (interchange != null) {
                    val segment1Stations = RouteFinder(allLines[fromLineKey]!!).findRoute(fromStationName, interchange.name)
                    val segment2Stations = RouteFinder(allLines[toLineKey]!!).findRoute(interchange.name, toStationName)

                    val routeSegments = listOf(
                        RouteSegment(segment1Stations, fromLineKey, allLines[fromLineKey]!!.name),
                        RouteSegment(segment2Stations, toLineKey, allLines[toLineKey]!!.name)
                    )

                    possibleRoutes.add(
                        PlannedRoute(
                            segments = routeSegments,
                            interchangeStations = listOf(interchange.name),
                            totalStations = segment1Stations.size + segment2Stations.size - 1
                        )
                    )
                }
            }
        }

        if (possibleRoutes.isNotEmpty()) {
            return possibleRoutes.minByOrNull { it.totalStations }!!
        }

        // 2-hop: two interchanges
        // E.g., Red -> Green -> Blue
        for (fromLineKey in fromLines) {
            for (toLineKey in toLines) {
                // Find intermediate lines that connect fromLineKey to some other line, and that line to toLineKey
                val allLineKeys = allLines.keys
                val intermediateLines = allLineKeys.filter { it != fromLineKey && it != toLineKey }

                for (midLineKey in intermediateLines) {
                    val interchange1 = interchanges.find { it.lines.contains(fromLineKey) && it.lines.contains(midLineKey) }
                    val interchange2 = interchanges.find { it.lines.contains(midLineKey) && it.lines.contains(toLineKey) }

                    if (interchange1 != null && interchange2 != null) {
                        val seg1 = RouteFinder(allLines[fromLineKey]!!).findRoute(fromStationName, interchange1.name)
                        val seg2 = RouteFinder(allLines[midLineKey]!!).findRoute(interchange1.name, interchange2.name)
                        val seg3 = RouteFinder(allLines[toLineKey]!!).findRoute(interchange2.name, toStationName)

                        val routeSegments = listOf(
                            RouteSegment(seg1, fromLineKey, allLines[fromLineKey]!!.name),
                            RouteSegment(seg2, midLineKey, allLines[midLineKey]!!.name),
                            RouteSegment(seg3, toLineKey, allLines[toLineKey]!!.name)
                        )

                        possibleRoutes.add(
                            PlannedRoute(
                                segments = routeSegments,
                                interchangeStations = listOf(interchange1.name, interchange2.name),
                                totalStations = seg1.size + seg2.size + seg3.size - 2
                            )
                        )
                    }
                }
            }
        }

        if (possibleRoutes.isNotEmpty()) {
            return possibleRoutes.minByOrNull { it.totalStations }!!
        }

        throw IllegalArgumentException("No valid route exists between $fromStationName and $toStationName")
    }

    private fun findLinesForStation(stationName: String): Set<String> {
        val lines = mutableSetOf<String>()
        for ((key, line) in allLines) {
            if (line.stations.any { it.name.equals(stationName, ignoreCase = true) }) {
                lines.add(key)
            }
        }
        return lines
    }
}
