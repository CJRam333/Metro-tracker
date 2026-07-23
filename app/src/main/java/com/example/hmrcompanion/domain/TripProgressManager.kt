package com.example.hmrcompanion.domain

class TripProgressManager(
    private val route: List<Station>,
    private val alertThresholdMeters: Int = 400
) {
    internal var lastConfirmedIndex: Int? = null

    fun initializeFromCurrentPosition(lat: Double, lng: Double) {
        if (route.isEmpty()) return

        var closestIndex = 0
        var minDistance = Double.MAX_VALUE

        for ((index, station) in route.withIndex()) {
            val dist = distanceMeters(lat, lng, station.lat, station.lng)
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = index
            }
        }

        if (closestIndex == route.size - 1) {
            return
        }

        if (closestIndex == 0) {
            lastConfirmedIndex = null
        } else {
            lastConfirmedIndex = closestIndex - 1
        }
    }

    fun onLocationUpdate(lat: Double, lng: Double): TripProgressEvent {
        if (route.isEmpty()) return TripProgressEvent.RouteComplete

        // Get the next station
        val nextStation = RouteFinder(MetroLine("", "", emptyList(), emptyList())).nextStation(route, lastConfirmedIndex)
            ?: return TripProgressEvent.RouteComplete

        val distance = distanceMeters(lat, lng, nextStation.lat, nextStation.lng)

        // Auto-advance if we pass very close to it
        if (distance <= 150.0) {
            lastConfirmedIndex = route.indexOf(nextStation)
        }

        val isFinalDestination = route.last() == nextStation

        return when {
            distance <= alertThresholdMeters && isFinalDestination -> {
                TripProgressEvent.ApproachingDestination(nextStation)
            }
            distance <= alertThresholdMeters && !isFinalDestination -> {
                TripProgressEvent.ApproachingIntermediate(nextStation)
            }
            else -> {
                TripProgressEvent.Travelling(nextStation, distance)
            }
        }
    }

    fun reset() {
        lastConfirmedIndex = null
    }
}
