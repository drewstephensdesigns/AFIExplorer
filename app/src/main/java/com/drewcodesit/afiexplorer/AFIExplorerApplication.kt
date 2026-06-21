/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.drewcodesit.afiexplorer.utils.objects.DelegatesExt

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
        val modeNight = sharedPreferences.getInt(getString(R.string.pref_key_mode_night), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(modeNight)
    }
}