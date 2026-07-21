package com.example.hmrcompanion.domain

class TripProgressManager(
    private val route: List<Station>,
    private val alertThresholdMeters: Int = 400
) {
    internal var lastConfirmedIndex: Int? = null

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
