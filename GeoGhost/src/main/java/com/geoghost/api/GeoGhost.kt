package com.geoghost.api

import android.content.Context
import android.content.Intent
import com.geoghost.internals.GeoGhostService
import com.geoghost.internals.SetupGuard
import com.geoghost.ui.GeoGhostSetupActivity

/**
 * Central entry point for interacting with the GeoGhost library.
 *
 * Provides methods to:
 * - Launch the setup flow for selecting spoofing routes.
 * - Start and stop the foreground spoofing service.
 * - Reset any spoofing configuration or state.
 * - Check whether spoofing is currently active.
 *
 * This object should be used by the host app to manage GeoGhost behavior programmatically.
 */
object GeoGhost {
    internal lateinit var appContext: Context
    private var hostAppMainActivityName: String? = null

    internal fun startSetupUI(context: Context, activityName: String) {
        appContext = context
        val intent = Intent(context, GeoGhostSetupActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        hostAppMainActivityName = activityName;
        context.startActivity(intent)
    }

    internal fun finishAndLaunch(context: Context) {
        val activityClassName = hostAppMainActivityName
        if (activityClassName == null) {
            throw IllegalStateException("Host activity name not set")
        }

        val activityClass = try {
            Class.forName(activityClassName)
        } catch (e: Exception) {
            throw RuntimeException("Unable to resolve host activity class", e)
        }

        val intent = Intent(context, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    /**
     * Starts the GeoGhost spoofing service in the foreground.
     * This begins simulating location based on the selected route.
     */
    fun start() {
        val intent = Intent(appContext, GeoGhostService::class.java)
        appContext.startForegroundService(intent)
    }

    /**
     * Stops the GeoGhost spoofing service gracefully.
     * Sends an intent to stop ongoing spoofing and disable mock mode.
     */
    fun stop() {
        val intent = Intent(appContext, GeoGhostService::class.java).apply {
            action = GeoGhostService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    /**
     * Resets any persistent setup flags or spoofing state.
     * Useful for re-triggering the setup flow or clearing app state.
     */
    fun resetLocation() {
        SetupGuard.reset()
    }

    /**
     * Returns true if GPS spoofing is currently active, based on shared state.
     */
    fun isSpoofing(): Boolean {
        return GeoGhostService.getSpoofMode()
    }
}