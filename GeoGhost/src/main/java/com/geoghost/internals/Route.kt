package com.geoghost.internals

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.annotations.PolylineOptions
import okhttp3.*
import java.io.IOException

internal data class Route(
    val start: LatLng,
    val end: LatLng? = null,
    val mode: TravelMode = TravelMode.WALK
) {
    /**
     * Fetches a route path (as a list of LatLngs) from OSRM using the start and end coordinates.
     * Calls [onResult] with the decoded route.
     */
    fun fetchRealRoute(onResult: (List<LatLng>) -> Unit) {
        if (end == null) {
            onResult(emptyList())
            return
        }

        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
                "?overview=full&geometries=geojson"

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("GeoGhost", "OSRM route fetch failed", e)
                    Handler(Looper.getMainLooper()).post {
                        onResult(emptyList())
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: return onResult(emptyList())
                    val json = JSONObject(body)
                    val coords = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")

                    val points = mutableListOf<LatLng>()
                    for (i in 0 until coords.length()) {
                        val coord = coords.getJSONArray(i)
                        val lng = coord.getDouble(0)
                        val lat = coord.getDouble(1)
                        points.add(LatLng(lat, lng))
                    }

                    Handler(Looper.getMainLooper()).post {
                        onResult(points)
                    }
                }
            })
    }

    /**
     * Draws a polyline on the given map using the provided route points.
     */
    fun drawRouteOnMap(map: MapLibreMap, points: List<LatLng>) {
        map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(4f)
        )
    }

    /**
     * Convenience function to both fetch and draw the route.
     */
    fun fetchAndDraw(map: MapLibreMap, onDone: (() -> Unit)? = null) {
        fetchRealRoute { routePoints ->
            drawRouteOnMap(map, routePoints)
            onDone?.invoke()
        }
    }
}

internal enum class TravelMode {
    STATIC,
    WALK,
    DRIVE
}