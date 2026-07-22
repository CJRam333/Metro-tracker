package com.example.hmrcompanion.domain

import kotlinx.serialization.Serializable

@Serializable
data class Station(
    val seq: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val interchange: String?
)

@Serializable
data class MetroLine(
    val key: String,
    val name: String,
    val terminals: List<String>,
    val stations: List<Station>
)

@Serializable
data class RouteSegment(
    val stations: List<Station>,
    val lineKey: String,
    val lineName: String
)

@Serializable
data class PlannedRoute(
    val segments: List<RouteSegment>,
    val interchangeStations: List<String>,
    val totalStations: Int
) {
    fun flatStations(): List<Station> {
        val result = mutableListOf<Station>()
        for ((index, segment) in segments.withIndex()) {
            if (index == 0) {
                result.addAll(segment.stations)
            } else {
                // Drop the first station of subsequent segments because it's the interchange
                // station already included at the end of the previous segment.
                result.addAll(segment.stations.drop(1))
            }
        }
        return result
    }

    fun lineKeyAtIndex(index: Int): String {
        var currentIndex = 0
        for (segment in segments) {
            val segmentSize = if (segment == segments.last()) segment.stations.size else segment.stations.size - 1
            if (index < currentIndex + segmentSize) {
                return segment.lineKey
            }
            currentIndex += segmentSize
        }
        return segments.last().lineKey
    }

    fun transferHintAt(index: Int): String? {
        val flat = flatStations()
        if (index < 0 || index >= flat.size - 1) return null

        val currentStation = flat[index]
        if (interchangeStations.contains(currentStation.name)) {
            // Find which segment this interchange concludes
            var accumulatedStations = 0
            for (i in 0 until segments.size - 1) { // We only care about segments that transition to another
                val segment = segments[i]
                // A segment contributes `size` stations, but if it's not the first, the first station is already counted
                val contribution = if (i == 0) segment.stations.size else segment.stations.size - 1
                accumulatedStations += contribution

                // If the current flat index is exactly at the end of this segment's contribution
                // it's the transfer point to the next segment
                if (index == accumulatedStations - 1) {
                    val nextSegment = segments[i + 1]
                    return "Change to ${nextSegment.lineName}"
                }
            }
        }
        return null
    }
}
