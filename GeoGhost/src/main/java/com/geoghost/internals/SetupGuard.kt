package com.geoghost.internals

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.geoghost.api.GeoGhost.appContext

internal object SetupGuard {
    private const val PREF_NAME = "GeoGhostPrefs"
    private const val KEY_ALREADY_SHOWN = "setup_done"

    fun hasLaunchedBefore(): Boolean {
        return getPrefs().getBoolean(KEY_ALREADY_SHOWN, false)
    }

    fun markAsDone() {
        getPrefs().edit { putBoolean(KEY_ALREADY_SHOWN, true) }
    }

    fun reset() {
        getPrefs().edit { putBoolean(KEY_ALREADY_SHOWN, false) }
        //getPrefs().edit { clear() }
    }

    private fun getPrefs(): SharedPreferences {
        return appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}