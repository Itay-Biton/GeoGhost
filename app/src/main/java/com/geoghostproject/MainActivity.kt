package com.geoghostproject

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.geoghost.api.GeoGhost
import com.google.android.gms.location.*
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraUpdateFactory

/**
 * Basic demo activity for the GeoGhost library.
 *
 * Shows current location on a MapLibre map and toggles GPS spoofing using GeoGhost.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var toggleButton: Button
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var map: MapLibreMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        toggleButton = findViewById(R.id.BTN)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it
            it.setStyle("https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json")
            startLocationUpdates()
        }

        // Toggle spoofing on button press
        toggleButton.setOnClickListener {
            if (GeoGhost.isSpoofing()) GeoGhost.stop() else GeoGhost.start()
        }

        // Listen for location updates (real or spoofed)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val latLng = LatLng(location.latitude, location.longitude)
                map?.apply {
                    clear()
                    addMarker(MarkerOptions().position(latLng))
                    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
                }
            }
        }

        // Reset spoofing state on launch
        GeoGhost.resetLocation()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}