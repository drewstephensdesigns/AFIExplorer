package com.drewcodesit.afiexplorer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.drewcodesit.afiexplorer.utils.DelegatesExt

class AFIExplorerApplication : Application() {

    companion object {
        var instance: AFIExplorerApplication by DelegatesExt.notNullSingleValue()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applyTheme()
    }

    /**
     * Applies the App's Theme from sharedPrefs
     */
    private fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val modeNight = sharedPreferences.getInt(
            getString(R.string.pref_key_mode_night),
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        AppCompatDelegate.setDefaultNightMode(modeNight)
    }
}