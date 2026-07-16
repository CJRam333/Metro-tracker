package com.example.hmrcompanion.domain

data class Station(
    val seq: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val interchange: String?
)

data class MetroLine(
    val key: String,
    val name: String,
    val terminals: List<String>,
    val stations: List<Station>
)
