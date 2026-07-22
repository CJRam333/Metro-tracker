package com.example.hmrcompanion.data

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.hmrcompanion.domain.RouteFinder
import com.example.hmrcompanion.domain.PlannedRoute
import com.example.hmrcompanion.domain.Station
import com.example.hmrcompanion.domain.TripProgressEvent
import com.example.hmrcompanion.domain.TripProgressManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TripService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var tripProgressManager: TripProgressManager? = null
    private var plannedRoute: PlannedRoute? = null

    private var hasAlertedDestination = false
    private var destinationName: String = "Unknown"

    companion object {
        private const val CHANNEL_ID = "hmr_trip_channel"
        private const val ALERT_CHANNEL_ID = "hmr_alert_channel"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2
        const val ACTION_LOCATION_UPDATE = "com.hmr.LOCATION_UPDATE"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_LAST_CONFIRMED_INDEX = "lastConfirmedIndex"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    tripProgressManager?.let { manager ->
                        val event = manager.onLocationUpdate(location.latitude, location.longitude)
                        handleTripEvent(event)
                    }

                    val intent = Intent(ACTION_LOCATION_UPDATE).apply {
                        putExtra(EXTRA_LAT, location.latitude)
                        putExtra(EXTRA_LNG, location.longitude)
                        putExtra(EXTRA_LAST_CONFIRMED_INDEX, tripProgressManager?.lastConfirmedIndex ?: -1)
                    }
                    localBroadcastManager.sendBroadcast(intent)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val flatStationsJson = intent?.getStringExtra("flatStationsJson")
        val plannedRouteJson = intent?.getStringExtra("plannedRouteJson")
        val alertDistanceMeters = intent?.getIntExtra("alertDistanceMeters", 400) ?: 400

        destinationName = intent?.getStringExtra("destination") ?: "Unknown"

        createNotificationChannels()
        val notification = createNotification(destinationName, "Loading...")

        // startForeground MUST be called unconditionally if started via startForegroundService,
        // even if the user has revoked notification permissions, otherwise the app crashes
        // with ForegroundServiceDidNotStartInTimeException. The OS will just silently drop
        // the notification visually if the permission is missing.
        startForeground(NOTIFICATION_ID, notification)

        if (flatStationsJson != null && plannedRouteJson != null) {
            try {
                val flatStations = Json.decodeFromString<List<Station>>(flatStationsJson)
                plannedRoute = Json.decodeFromString<PlannedRoute>(plannedRouteJson)

                tripProgressManager = TripProgressManager(flatStations, alertDistanceMeters)

                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setMinUpdateIntervalMillis(3000L)
                    .build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                // Handle failure gracefully in a real app
            }
        }

        return START_NOT_STICKY
    }

    private fun handleTripEvent(event: TripProgressEvent) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        when (event) {
            is TripProgressEvent.Travelling -> {
                val distanceKm = event.distanceMeters / 1000.0
                val distanceStr = String.format("%.1f km", distanceKm)

                val index = (tripProgressManager?.lastConfirmedIndex ?: -1) + 1
                val hint = plannedRoute?.transferHintAt(index)

                val nextStopText = if (hint != null) "${event.nextStation.name} ($hint) ($distanceStr)" else "${event.nextStation.name} ($distanceStr)"

                val notification = createNotification(destinationName, nextStopText)
                if (canNotify) notificationManager.notify(NOTIFICATION_ID, notification)
            }
            is TripProgressEvent.ApproachingIntermediate -> {
                val index = (tripProgressManager?.lastConfirmedIndex ?: -1) + 1
                val hint = plannedRoute?.transferHintAt(index)

                val nextStopText = if (hint != null) "${event.station.name} ($hint)" else event.station.name
                val notification = createNotification(destinationName, nextStopText)
                if (canNotify) notificationManager.notify(NOTIFICATION_ID, notification)
            }
            is TripProgressEvent.ApproachingDestination -> {
                val notification = createNotification(destinationName, event.station.name)
                if (canNotify) notificationManager.notify(NOTIFICATION_ID, notification)

                if (!hasAlertedDestination) {
                    hasAlertedDestination = true
                    val alertNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                        .setContentTitle("Your stop is coming up!")
                        .setContentText("Get ready — ${event.station.name} is approaching")
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    if (canNotify) notificationManager.notify(ALERT_NOTIFICATION_ID, alertNotification)
                }
            }
            is TripProgressEvent.RouteComplete -> {
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tripChannel = NotificationChannel(
                CHANNEL_ID,
                "Trip Tracking",
                NotificationManager.IMPORTANCE_LOW
            )

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Station Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(tripChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    private fun createNotification(destination: String, nextStop: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HMR Companion")
            .setContentText("Travelling to: $destination\nNext stop: $nextStop")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Travelling to: $destination\nNext stop: $nextStop"))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }
}
