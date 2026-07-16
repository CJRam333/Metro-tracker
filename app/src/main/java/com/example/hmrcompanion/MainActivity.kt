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
                        onTripStarted = { lineKey, fromStation, toStation ->
                            val serviceIntent = Intent(this@MainActivity, TripService::class.java).apply {
                                putExtra("destination", toStation)
                                putExtra("nextStop", fromStation)
                                putExtra("lineKey", lineKey)
                                putExtra("fromStation", fromStation)
                                putExtra("toStation", toStation)
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
