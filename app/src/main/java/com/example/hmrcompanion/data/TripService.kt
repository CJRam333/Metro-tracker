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
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class TripService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var localBroadcastManager: LocalBroadcastManager

    companion object {
        private const val CHANNEL_ID = "hmr_trip_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_LOCATION_UPDATE = "com.hmr.LOCATION_UPDATE"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val intent = Intent(ACTION_LOCATION_UPDATE).apply {
                        putExtra(EXTRA_LAT, location.latitude)
                        putExtra(EXTRA_LNG, location.longitude)
                    }
                    localBroadcastManager.sendBroadcast(intent)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val destination = intent?.getStringExtra("destination") ?: "Unknown"
        val nextStop = intent?.getStringExtra("nextStop") ?: "Unknown"

        createNotificationChannel()
        val notification = createNotification(destination, nextStop)

        startForeground(NOTIFICATION_ID, notification)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trip Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(destination: String, nextStop: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HMR Companion")
            .setContentText("Travelling to: $destination\nNext stop: $nextStop")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Travelling to: $destination\nNext stop: $nextStop"))
            // We don't have an icon setup yet, just providing a dummy fallback
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }
}
