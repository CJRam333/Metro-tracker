package com.example.hmrcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import com.example.hmrcompanion.data.AndroidAssetReader
import com.example.hmrcompanion.data.StationRepository
import com.example.hmrcompanion.data.TripService
import com.example.hmrcompanion.ui.TripPlannerScreen
import com.example.hmrcompanion.ui.TripPlannerViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetReader = AndroidAssetReader(this)
        val stationRepository = StationRepository(assetReader)

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TripPlannerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TripPlannerViewModel(stationRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val tripPlannerViewModel: TripPlannerViewModel = viewModel(factory = factory)
                    TripPlannerScreen(
                        viewModel = tripPlannerViewModel,
                        onTripStarted = { plannedRoute, alertDistanceMeters ->
                            val serviceIntent = Intent(this@MainActivity, TripService::class.java).apply {
                                putExtra("flatStationsJson", Json.encodeToString(plannedRoute.flatStations()))
                                putExtra("interchangeStationsJson", Json.encodeToString(plannedRoute.interchangeStations))
                                putExtra("plannedRouteJson", Json.encodeToString(plannedRoute))
                                putExtra("alertDistanceMeters", alertDistanceMeters)
                                putExtra("destination", plannedRoute.flatStations().last().name)
                                putExtra("nextStop", plannedRoute.flatStations().first().name)
                            }
                            startForegroundService(serviceIntent)
                        },
                        onTripStopped = {
                            val serviceIntent = Intent(this@MainActivity, TripService::class.java)
                            stopService(serviceIntent)
                        }
                    )
                }
            }
        }
    }
}
