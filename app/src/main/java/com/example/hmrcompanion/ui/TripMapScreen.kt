package com.example.hmrcompanion.ui

import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.hmrcompanion.domain.PlannedRoute
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Overlay
import kotlinx.coroutines.delay

@Composable
fun TripMapScreen(
    plannedRoute: PlannedRoute,
    currentLatLng: Pair<Double, Double>?,
    lastConfirmedIndex: Int?,
    onStopTrip: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            controller.setZoom(14.0)

            // Center map initially on the From station
            val startStation = plannedRoute.flatStations().firstOrNull()
            if (startStation != null) {
                controller.setCenter(GeoPoint(startStation.lat, startStation.lng))
            }
        }
    }

    var lastUserInteractionTime by remember { mutableStateOf(0L) }

    // Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Interaction tracking overlay
    DisposableEffect(mapView) {
        val touchOverlay = object : Overlay() {
            override fun onTouchEvent(event: MotionEvent?, mapView: MapView?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_MOVE) {
                    lastUserInteractionTime = System.currentTimeMillis()
                }
                return super.onTouchEvent(event, mapView)
            }
        }
        mapView.overlays.add(touchOverlay)
        onDispose {
            mapView.overlays.remove(touchOverlay)
        }
    }

    // Auto-camera re-center logic
    LaunchedEffect(currentLatLng) {
        if (currentLatLng != null) {
            val now = System.currentTimeMillis()
            if (now - lastUserInteractionTime > 5000) {
                mapView.controller.animateTo(GeoPoint(currentLatLng.first, currentLatLng.second))
            }
        }
    }

    // Route Overlays
    LaunchedEffect(plannedRoute, currentLatLng, lastConfirmedIndex) {
        // Clear all overlays except our touch interceptor
        mapView.overlays.removeAll { it is Marker || it is Polyline }

        val flatStations = plannedRoute.flatStations()

        // Draw Polyline
        val polyline = Polyline()
        polyline.outlinePaint.color = Color.parseColor("#3F51B5") // indigo
        polyline.outlinePaint.strokeWidth = 5f
        polyline.outlinePaint.strokeCap = Paint.Cap.ROUND
        polyline.setPoints(flatStations.map { GeoPoint(it.lat, it.lng) })
        mapView.overlays.add(polyline)

        // Draw station markers
        val nextUpcomingIndex = (lastConfirmedIndex ?: -1) + 1

        for ((index, station) in flatStations.withIndex()) {
            val isInterchange = plannedRoute.interchangeStations.contains(station.name)
            val isFinal = index == flatStations.size - 1
            val isPassed = lastConfirmedIndex != null && index <= lastConfirmedIndex
            val isNext = index == nextUpcomingIndex

            val marker = Marker(mapView)
            marker.position = GeoPoint(station.lat, station.lng)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Marker styling rules
            when {
                isPassed -> {
                    marker.title = station.name
                    marker.icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    marker.icon.setTint(Color.GRAY)
                    // Make it smaller by scaling
                    marker.icon.setBounds(0, 0, marker.icon.intrinsicWidth / 2, marker.icon.intrinsicHeight / 2)
                }
                isFinal -> {
                    // Red marker (default OSMDroid marker is often red/orange, but we tint it red if needed, or rely on default)
                    marker.title = station.name
                    marker.icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    marker.icon.setTint(Color.RED)
                }
                isNext -> {
                    marker.title = station.name
                    marker.icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    marker.icon.setTint(Color.BLUE)
                }
                isInterchange -> {
                    marker.title = "${station.name} (Change line)"
                    marker.icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    marker.icon.setTint(Color.YELLOW)
                }
                else -> {
                    // Regular upcoming station
                    marker.icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    marker.icon.setTint(Color.GRAY)
                    // No title means no label bubble
                }
            }

            mapView.overlays.add(marker)
        }

        // Draw live user position
        if (currentLatLng != null) {
            val userMarker = Marker(mapView)
            userMarker.position = GeoPoint(currentLatLng.first, currentLatLng.second)
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            userMarker.icon = context.getDrawable(android.R.drawable.presence_online) // Built-in cyan/green dot
            userMarker.title = "You are here"
            mapView.overlays.add(userMarker)
        }

        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = onStopTrip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("Stop Trip")
        }
    }
}
