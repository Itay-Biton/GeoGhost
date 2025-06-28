package com.geoghost.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.geoghost.R
import com.geoghost.api.GeoGhost
import com.geoghost.api.GeoGhostState
import com.geoghost.internals.PermissionsHelper
import com.geoghost.internals.Route
import com.geoghost.internals.SetupGuard
import com.geoghost.internals.TravelMode
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

internal class GeoGhostSetupActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var startButton: Button
    private lateinit var zoomInButton: Button
    private lateinit var zoomOutButton: Button
    private var map: MapLibreMap? = null

    private lateinit var modeGroup: MaterialButtonToggleGroup
    private lateinit var radioStatic: MaterialButton
    private lateinit var radioWalk: MaterialButton
    private lateinit var radioDrive: MaterialButton

    private var requiredPins = 1
    private val selectedLocations = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_geo_ghost_setup)

        if (!PermissionsHelper.hasLocationPermissions()) {
            PermissionsHelper.requestPermissions(this)
        }

        mapView = findViewById(R.id.mapView)
        startButton = findViewById(R.id.startAppButton)
        zoomInButton = findViewById(R.id.btnZoomIn)
        zoomOutButton = findViewById(R.id.btnZoomOut)

        modeGroup = findViewById(R.id.modeGroup)
        radioStatic = findViewById(R.id.radioStatic)
        radioWalk = findViewById(R.id.radioWalk)
        radioDrive = findViewById(R.id.radioDrive)

        radioStatic.isChecked = true

        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            requiredPins = if (checkedId == R.id.radioStatic) 1 else 2
            selectedLocations.clear()
            map?.clear()
            startButton.isEnabled = false
        }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            this.map = map
            map.setStyle("https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json")
            map.uiSettings.apply {
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isTiltGesturesEnabled = true
                isRotateGesturesEnabled = true
            }
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(31.6, 34.8))
                .zoom(12.0)
                .build()

            map.addOnMapClickListener { latLng ->
                if (selectedLocations.size >= requiredPins) {
                    selectedLocations.clear()
                    map.clear()
                }

                selectedLocations.add(latLng)
                map.addMarker(MarkerOptions().position(latLng))
                startButton.isEnabled = selectedLocations.size == requiredPins

                if (selectedLocations.size == requiredPins) {
                    val previewRoute = Route(selectedLocations[0], selectedLocations.getOrNull(1))
                    previewRoute.fetchAndDraw(map)
                }

                true
            }
        }

        zoomInButton.setOnClickListener {
            map?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomIn())
        }

        zoomOutButton.setOnClickListener {
            map?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomOut())
        }

        startButton.setOnClickListener {
            if (!PermissionsHelper.hasLocationPermissions()) {
                Toast.makeText(this, "Please enable permissions before starting", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val start = selectedLocations.getOrNull(0)
            val end = selectedLocations.getOrNull(1)

            val route = Route(
                start = start!!,
                end = if (requiredPins == 2) end else null,
                mode = when {
                    radioWalk.isChecked -> TravelMode.WALK
                    radioDrive.isChecked -> TravelMode.DRIVE
                    else -> TravelMode.STATIC
                }
            )

            GeoGhostState.route = route
            SetupGuard.markAsDone()
            GeoGhost.start()
            GeoGhost.finishAndLaunch(this)
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}