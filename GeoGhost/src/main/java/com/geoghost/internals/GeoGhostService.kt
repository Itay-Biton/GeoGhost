package com.geoghost.internals

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import com.geoghost.api.GeoGhost
import com.geoghost.api.GeoGhostState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.maplibre.android.geometry.LatLng
import androidx.core.content.edit

internal class GeoGhostService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var spoofThread: Thread? = null
    @Volatile private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setSpoofMode(true)
        if (intent?.action == ACTION_STOP) {
            stopSpoofing()
            stopSelf()
            return START_NOT_STICKY
        }

        val route = GeoGhostState.route ?: return START_NOT_STICKY

        isRunning = true
        spoofThread = Thread {
            when (route.mode) {
                TravelMode.STATIC -> spoofLocation(route.start)
                TravelMode.WALK, TravelMode.DRIVE -> simulateRoute(route)
            }
        }.also { it.start() }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSpoofing()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun spoofLocation(latLng: LatLng) {
        val mockLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
            accuracy = 1f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        try {
            if (!hasPermission()) return
            fusedClient.setMockMode(true)
            fusedClient.setMockLocation(mockLocation)
        } catch (e: SecurityException) {
            Log.e("GeoGhost", "SecurityException: missing location permissions", e)
        } catch (e: Exception) {
            Log.e("GeoGhost", "Failed to spoof location", e)
        }
    }

    private fun simulateRoute(route: Route) {
        route.fetchRealRoute { routePoints ->
            if (routePoints.isEmpty()) return@fetchRealRoute

            spoofThread = Thread {
                try {
                    var forward = true
                    while (isRunning) {
                        val points = if (forward) routePoints else routePoints.asReversed()
                        for (point in points) {
                            if (!isRunning) break
                            spoofLocation(point)
                            val sleepTime = when (route.mode) {
                                TravelMode.WALK -> 1000L
                                TravelMode.DRIVE -> 200L
                                else -> 1000L
                            }
                            Thread.sleep(sleepTime)
                        }
                        forward = !forward
                    }
                } catch (e: InterruptedException) {
                    Log.d("GeoGhost", "Spoofing thread interrupted during real route.")
                }
            }.also { it.start() }
        }
    }

    private fun stopSpoofing() {
        setSpoofMode(false)
        isRunning = false
        spoofThread?.interrupt()
        spoofThread = null
        disableMocking()
    }

    private fun disableMocking() {
        try {
            if (!hasPermission()) return
            fusedClient.setMockMode(false)
        } catch (e: SecurityException) {
            Log.e("GeoGhost", "SecurityException: can't disable mock mode", e)
        } catch (e: Exception) {
            Log.e("GeoGhost", "Failed to disable mock mode", e)
        }
    }

    private fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotification() {
        val channelId = "GeoGhostChannel"
        val stopIntent = Intent(this, GeoGhostService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "GeoGhost Spoofing", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("GeoGhost Spoofing")
            .setContentText("Sending mock GPS data...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .build()

        startForeground(1, notification)
    }

    companion object {
        const val ACTION_STOP = "com.geoghost.action.STOP_SPOOFING"
        private const val PREFS = "GeoGhostPrefs"
        private const val KEY_SPOOFING = "isSpoofing"

        fun getSpoofMode(): Boolean {
            return GeoGhost.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SPOOFING, false)
        }

        private fun setSpoofMode(value: Boolean) {
            GeoGhost.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit { putBoolean(KEY_SPOOFING, value) }
        }
    }
}