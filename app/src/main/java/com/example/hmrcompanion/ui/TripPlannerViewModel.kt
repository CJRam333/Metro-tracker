package com.example.hmrcompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hmrcompanion.data.StationRepository
import com.example.hmrcompanion.domain.MetroLine
import com.example.hmrcompanion.domain.MultiLineRouteFinder
import com.example.hmrcompanion.domain.PlannedRoute
import com.example.hmrcompanion.domain.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripPlannerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val allLines: Map<String, MetroLine> = emptyMap(),
    val allStations: List<Station> = emptyList(),
    val fromStation: Station? = null,
    val toStation: Station? = null,
    val isTrackingActive: Boolean = false,
    val alertDistanceMeters: Int = 400
)

class TripPlannerViewModel(private val repository: StationRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TripPlannerUiState())
    val uiState: StateFlow<TripPlannerUiState> = _uiState.asStateFlow()

    init {
        loadLines()
    }

    private fun loadLines() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val linesList = repository.getAllLines()
                val linesMap = linesList.associateBy { it.key }
                val uniqueStations = linesList
                    .flatMap { it.stations }
                    .distinctBy { it.name }
                    .sortedBy { it.name }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allLines = linesMap,
                    allStations = uniqueStations
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load station data"
                )
            }
        }
    }

    fun selectFromStation(name: String) {
        val station = _uiState.value.allStations.find { it.name == name }
        _uiState.value = _uiState.value.copy(fromStation = station)

        // Clear toStation if it's the same as the new fromStation
        if (_uiState.value.toStation?.name == name) {
            _uiState.value = _uiState.value.copy(toStation = null)
        }
    }

    fun selectToStation(name: String) {
        val station = _uiState.value.allStations.find { it.name == name }
        _uiState.value = _uiState.value.copy(toStation = station)
    }

    fun setAlertDistance(meters: Int) {
        _uiState.value = _uiState.value.copy(alertDistanceMeters = meters)
    }

    fun canStartTrip(): Boolean {
        val state = _uiState.value
        return state.fromStation != null &&
               state.toStation != null &&
               state.fromStation.name != state.toStation.name &&
               !state.isTrackingActive
    }

    fun getPlannedRoute(): PlannedRoute? {
        val state = _uiState.value
        if (!canStartTrip()) return null

        return try {
            MultiLineRouteFinder(state.allLines).findRoute(
                fromStationName = state.fromStation!!.name,
                toStationName = state.toStation!!.name
            )
        } catch (e: Exception) {
            null
        }
    }

    fun setTrackingActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isTrackingActive = active)
    }
}
