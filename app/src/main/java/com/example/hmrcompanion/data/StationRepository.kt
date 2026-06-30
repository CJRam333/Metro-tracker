package com.example.hmrcompanion.data

import com.example.hmrcompanion.domain.MetroLine
import com.example.hmrcompanion.domain.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StationRepository(private val assetReader: AssetReader) {

    private var cachedLines: List<MetroLine>? = null
    private val mutex = Mutex()

    private val json = Json { ignoreUnknownKeys = true }

    @Throws(Exception::class)
    suspend fun getAllLines(): List<MetroLine> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedLines?.let { return@withContext it }

            val jsonString = try {
                assetReader.readAsset("hmr_stations.json")
            } catch (e: Exception) {
                throw Exception("Failed to read hmr_stations.json asset: ${e.message}", e)
            }

            val parsedData = try {
                json.decodeFromString<List<StationResponseDto>>(jsonString)
            } catch (e: SerializationException) {
                throw Exception("Failed to parse hmr_stations.json: malformed data", e)
            }

            if (parsedData.isEmpty()) {
                throw Exception("hmr_stations.json is empty")
            }

            val linesMap = parsedData.first().lines

            val lines = linesMap.map { (key, lineDto) ->
                MetroLine(
                    key = key,
                    name = lineDto.name,
                    terminals = lineDto.terminals,
                    stations = lineDto.stations.map { stationDto ->
                        Station(
                            seq = stationDto.seq,
                            name = stationDto.name,
                            lat = stationDto.lat,
                            lng = stationDto.lng,
                            interchange = stationDto.interchange
                        )
                    }
                )
            }

            cachedLines = lines
            lines
        }
    }

    suspend fun getLine(key: String): MetroLine? {
        val lines = getAllLines()
        return lines.find { it.key == key }
    }
}
