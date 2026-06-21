/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer

import android.content.SharedPreferences
import android.graphics.Typeface
import java.util.concurrent.TimeUnit
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.drewcodesit.afiexplorer.databinding.ActivityMainBinding
import com.drewcodesit.afiexplorer.utils.worker.UpdateWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import es.dmoral.toasty.Toasty

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Builds a background task request that runs your UpdateWorker periodically.
        // Sets the interval to 15 minutes (Note: 15 minutes is the strict minimum interval allowed by Android's WorkManager API).
        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
            15, TimeUnit.MINUTES
        ).build()

        // Schedules the work with the system's WorkManager instance.
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            // A unique name string used to identify and manage this specific periodic task across the app
            "pub_update_check",

            // ExistingPeriodicWorkPolicy.KEEP ensures that if a sync job with this name is already queued,
            // it will keep the existing one and discard this new request (avoids resetting the 15-minute timer on app restarts).
            ExistingPeriodicWorkPolicy.KEEP,

            // The configured work configuration request created above
            workRequest
        )

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_browse,
                R.id.navigation_library,
                R.id.navigation_options_menu
            )
        )

        // ActionBar is managed by the activity
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        toastyConfig()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Toasty Configurable Items
    private fun toastyConfig(){
        val typeface: Typeface? = ResourcesCompat.getFont(applicationContext, R.font.roboto)
        Toasty.Config.getInstance()
            .setTextSize(14)
            .setToastTypeface(typeface!!)
            .supportDarkTheme(true)
            .apply()
    }
}