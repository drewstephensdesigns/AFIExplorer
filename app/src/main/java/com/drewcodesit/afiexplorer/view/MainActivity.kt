package com.drewcodesit.afiexplorer.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.drewcodesit.afiexplorer.MyApplication
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.R.drawable
import com.drewcodesit.afiexplorer.R.font
import com.drewcodesit.afiexplorer.R.id
import com.drewcodesit.afiexplorer.R.string
import com.drewcodesit.afiexplorer.databinding.MainActivityBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.option.DisplayMode
import com.maxkeppeler.sheets.option.Option
import com.maxkeppeler.sheets.option.OptionSheet
import es.dmoral.toasty.Toasty

class MainActivity : AppCompatActivity() {

    // Migration to View Binding from Kotlin Synthetics
    // Source: https://medium.com/codex/android-viewbinding-migration-16070e24b752
    private lateinit var _binding: MainActivityBinding

    // Shared Prefs
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        setSupportActionBar(_binding.mainToolBar)

        val navView: BottomNavigationView = _binding.content.navView
        val navHostFragment =
            supportFragmentManager.findFragmentById(id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                id.navigation_featured,
                id.navigation_home,
                id.navigation_favorites,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        toastyConfig()
        setupMenu()
    }

    //Moving back to list fragment by clicking on back arrow button
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(id.navigation_home)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // called when the configuration of the device changes. It is typically used
    // to handle changes in device orientation or changes in other device-specific
    // configurations such as the language or screen size.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (application as MyApplication).applyTheme()
        getOrientation()
        recreate()
    }

    private fun getOrientation() {
        requestedOrientation =
            if (Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                ) == 1
            ) {
                //Auto Rotate is on, so don't lock
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                //Auto Rotate is off, so lock
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
    }

    private fun toastyConfig() {
        val typeface: Typeface? = ResourcesCompat.getFont(applicationContext, font.ibm_plex_sans)
        Toasty.Config.getInstance()
            .setTextSize(14)
            .setToastTypeface(typeface!!)
            .supportDarkTheme(true)
            .apply()
    }

    private fun setupMenu(){
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId){
                    id.action_feedback -> {
                        startActivity(
                            Intent(
                                this@MainActivity,
                                AboutActivity::class.java
                                /*
                                    this@MainActivity,
                                    SettingsActivity::class.java

                                     */
                            )
                        )
                        true
                    }

                    id.action_change_theme -> {
                        OptionSheet().show(this@MainActivity) {
                            style(SheetStyle.DIALOG)
                            displayToolbar(true)
                            title("Set Theme")
                            displayMode(DisplayMode.LIST)
                            with(
                                Option(drawable.ic_light_mode, "Light"),
                                Option(drawable.ic_dark_mode, "Dark"),
                                Option(drawable.ic_follow_system, "Follow System")
                            )
                            onPositive { index: Int, _: Option ->
                                when (index) {
                                    0 -> {
                                        sharedPreferences.edit().putInt(
                                            getString(string.pref_key_mode_night),
                                            MODE_NIGHT_NO
                                        ).apply()
                                        setDefaultNightMode(MODE_NIGHT_NO)
                                    }
                                    1 -> {
                                        sharedPreferences.edit().putInt(
                                            getString(string.pref_key_mode_night),
                                            MODE_NIGHT_YES
                                        ).apply()
                                        setDefaultNightMode(MODE_NIGHT_YES)
                                    }
                                    2 -> {
                                        sharedPreferences.edit().putInt(
                                            getString(string.pref_key_mode_night),
                                            MODE_NIGHT_FOLLOW_SYSTEM
                                        ).apply()
                                        setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                                    }
                                }
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        })
    }
}