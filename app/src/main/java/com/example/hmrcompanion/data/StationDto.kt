package com.example.hmrcompanion.data

import kotlinx.serialization.Serializable

@Serializable
data class StationDto(
    val seq: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val interchange: String? = null
)

@Serializable
data class LineDto(
    val name: String,
    val terminals: List<String>,
    val stations: List<StationDto>
)

@Serializable
data class StationResponseDto(
    val _meta: MetaDto,
    val lines: Map<String, LineDto>
)

@Serializable
data class MetaDto(
    val source: String,
    val accuracy_note: String,
    val total_stations: Int,
    val lines: List<String>
)
