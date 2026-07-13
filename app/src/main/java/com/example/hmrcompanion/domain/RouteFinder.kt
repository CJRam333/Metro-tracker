package com.example.hmrcompanion.domain

class RouteFinder(private val metroLine: MetroLine) {

    fun findRoute(fromStationName: String, toStationName: String): List<Station> {
        val fromIndex = metroLine.stations.indexOfFirst { it.name.equals(fromStationName, ignoreCase = true) }
        val toIndex = metroLine.stations.indexOfFirst { it.name.equals(toStationName, ignoreCase = true) }

        if (fromIndex == -1) {
            throw IllegalArgumentException("Station not found on this line: $fromStationName")
        }
        if (toIndex == -1) {
            throw IllegalArgumentException("Station not found on this line: $toStationName")
        }

        return if (fromIndex <= toIndex) {
            metroLine.stations.subList(fromIndex, toIndex + 1)
        } else {
            metroLine.stations.subList(toIndex, fromIndex + 1).reversed()
        }
    }

    fun nextStation(route: List<Station>, lastConfirmedIndex: Int?): Station? {
        if (route.isEmpty()) return null

        return if (lastConfirmedIndex == null) {
            if (route.size > 1) route[1] else null
        } else {
            val nextIndex = lastConfirmedIndex + 1
            if (nextIndex < route.size) {
                route[nextIndex]
            } else {
                null
            }
        }
    }
}
