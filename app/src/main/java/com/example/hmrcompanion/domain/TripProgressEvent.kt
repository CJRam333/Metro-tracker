package com.example.hmrcompanion.domain

sealed class TripProgressEvent {
    data class Travelling(val nextStation: Station, val distanceMeters: Double) : TripProgressEvent()
    data class ApproachingIntermediate(val station: Station) : TripProgressEvent()
    data class ApproachingDestination(val station: Station) : TripProgressEvent()
    object RouteComplete : TripProgressEvent()
}
