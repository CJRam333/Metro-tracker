package com.example.hmrcompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hmrcompanion.data.StationRepository
import com.example.hmrcompanion.domain.MetroLine
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
    val allLines: List<MetroLine> = emptyList(),
    val selectedLine: MetroLine? = null,
    val stationsForSelectedLine: List<Station> = emptyList(),
    val fromStation: Station? = null,
    val toStation: Station? = null
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
                val lines = repository.getAllLines()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allLines = lines
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load station data"
                )
            }
        }
    }

    fun selectLine(lineKey: String) {
        val currentLines = _uiState.value.allLines
        val line = currentLines.find { it.key == lineKey }
        _uiState.value = _uiState.value.copy(
            selectedLine = line,
            stationsForSelectedLine = line?.stations ?: emptyList(),
            fromStation = null,
            toStation = null
        )
    }

    fun selectFromStation(name: String) {
        val station = _uiState.value.stationsForSelectedLine.find { it.name == name }
        _uiState.value = _uiState.value.copy(fromStation = station)

        // Clear toStation if it's the same as the new fromStation
        if (_uiState.value.toStation?.name == name) {
            _uiState.value = _uiState.value.copy(toStation = null)
        }
    }

    fun selectToStation(name: String) {
        val station = _uiState.value.stationsForSelectedLine.find { it.name == name }
        _uiState.value = _uiState.value.copy(toStation = station)
    }

    fun canStartTrip(): Boolean {
        val state = _uiState.value
        return state.selectedLine != null &&
               state.fromStation != null &&
               state.toStation != null &&
               state.fromStation.name != state.toStation.name
    }
}
