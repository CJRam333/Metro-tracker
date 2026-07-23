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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.hmrcompanion.ui.TripMapScreen
import com.example.hmrcompanion.ui.TripPlannerScreen
import com.example.hmrcompanion.ui.TripPlannerViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    private lateinit var locationReceiver: BroadcastReceiver
    private var tripPlannerViewModel: TripPlannerViewModel? = null

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

        locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == TripService.ACTION_LOCATION_UPDATE) {
                    val lat = intent.getDoubleExtra(TripService.EXTRA_LAT, 0.0)
                    val lng = intent.getDoubleExtra(TripService.EXTRA_LNG, 0.0)
                    val accuracy = if (intent.hasExtra(TripService.EXTRA_ACCURACY)) {
                        intent.getFloatExtra(TripService.EXTRA_ACCURACY, 0f)
                    } else null
                    val index = intent.getIntExtra(TripService.EXTRA_LAST_CONFIRMED_INDEX, -1)
                    val resolvedIndex = if (index == -1) null else index

                    tripPlannerViewModel?.updateLocation(lat, lng, accuracy)
                    tripPlannerViewModel?.updateLastConfirmedIndex(resolvedIndex)
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationReceiver,
            IntentFilter(TripService.ACTION_LOCATION_UPDATE)
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TripPlannerViewModel = viewModel(factory = factory)
                    tripPlannerViewModel = viewModel

                    val uiState by viewModel.uiState.collectAsState()

                    if (uiState.isTrackingActive && viewModel.getPlannedRoute() != null) {
                        TripMapScreen(
                            plannedRoute = viewModel.getPlannedRoute()!!,
                            currentLatLng = uiState.currentLatLng,
                            locationAccuracy = uiState.locationAccuracy,
                            lastConfirmedIndex = uiState.lastConfirmedIndex,
                            onStopTrip = {
                                viewModel.setTrackingActive(false)
                                val serviceIntent = Intent(this@MainActivity, TripService::class.java)
                                stopService(serviceIntent)
                            }
                        )
                    } else {
                        TripPlannerScreen(
                            viewModel = viewModel,
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

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }
}
