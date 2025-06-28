package com.geoghost.internals

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.geoghost.api.GeoGhost.appContext
import com.geoghost.ui.GeoGhostSetupActivity

internal object PermissionsHelper {

    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(activity: GeoGhostSetupActivity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }

    fun showFixDialog(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(appContext)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                }
                appContext.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { onDismiss?.invoke() }
            .show()
    }
}