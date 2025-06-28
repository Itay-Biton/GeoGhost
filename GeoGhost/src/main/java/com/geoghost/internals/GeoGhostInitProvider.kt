package com.geoghost.internals

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import com.geoghost.api.GeoGhost

internal class GeoGhostInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        GeoGhost.appContext = context.applicationContext

        val app = context.applicationContext as Application
        val launcherActivityName = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.component?.className
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity::class.java.name == launcherActivityName && !SetupGuard.hasLaunchedBefore()) {
                    activity.finish()
                    if (launcherActivityName != null)
                        GeoGhost.startSetupUI(context, launcherActivityName)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        return true
    }

    override fun insert(uri: android.net.Uri, values: ContentValues?): android.net.Uri? = null
    override fun query(uri: android.net.Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun update(uri: android.net.Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun delete(uri: android.net.Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun getType(uri: android.net.Uri): String? = null
}