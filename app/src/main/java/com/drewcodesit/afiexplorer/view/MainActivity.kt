package com.drewcodesit.afiexplorer.view

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.JsonArrayRequest
import com.drewcodesit.afiexplorer.MyApplication
import com.drewcodesit.afiexplorer.MyApplication.Companion.TAG
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.R.*
import com.drewcodesit.afiexplorer.adapters.MainAdapter
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.MyDividerItemDecoration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.main_activity.*
import java.util.*


// PubsAdapterListener opens publications
class MainActivity : AppCompatActivity(), MainAdapter.PubsAdapterListener {

    private var pubsList: ArrayList<Pubs>? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: MainAdapter? = null
    private var searchView: SearchView? = null
    private var request: JsonArrayRequest? = null

    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(layout.main_activity)

        toastyConfig()

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(true)
            title = resources.getString(string.app_name)
        }

        recyclerView = findViewById<View?>(id.recycler_view) as RecyclerView
        //recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager = LinearLayoutManager(
            this
        )
        recyclerView?.apply {

            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                MyDividerItemDecoration(
                    this@MainActivity,
                    DividerItemDecoration.VERTICAL,
                    36
                )
            )
        }

        pubsList = ArrayList()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        favoriteDatabase = FavoriteDatabase.getDatabase(applicationContext)

        fetchPubs()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: --------")
    }

    // SearchView
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu.findItem(id.action_search)?.actionView as SearchView

        searchView?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            setIconifiedByDefault(true)
            maxWidth = Int.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter?.filter?.filter(newText!!.lowercase(Locale.getDefault())) {
                        when (adapter?.itemCount) {
                            0 -> {
                                no_results_found.visibility = View.VISIBLE
                                no_results_found.text = getString(string.no_results_found, newText)
                            }
                            else -> {
                                no_results_found.visibility = View.GONE
                            }
                        }
                    }
                    Log.i("MAIN_ACTIVITY", newText!!)
                    return false
                }
            })

            /*
             * Hides other menu items when searchview is active
             * Source: https://stackoverflow.com/questions/32840576/actionbar-hiding-all-menu-items-except-search-field
             */
            setOnQueryTextFocusChangeListener { _, _ ->
                menu.findItem(R.id.action_bookmark).isVisible = !hasFocus()
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

           // Search function for looking up publications
           id.action_search -> {true}

            /*
            * Open Source Licenses
            * App Store Listing for Rating
            * Social Media Links (Github, Instagram, LinkedIn)
            * Developer Email
             */

            id.action_feedback -> {
                startActivity(
                    Intent(
                        this@MainActivity,
                        SettingsActivity::class.java
                    )
                )
                true
            }
            // Change App Theme (Light, Dark, System Follow)
            id.action_change_theme -> {
                val themeSelections = getThemeSelections()
                AlertDialog.Builder(this)
                    .setTitle("Change Theme")
                    .setSingleChoiceItems(
                        themeSelections.first,
                        themeSelections.second
                    ) { dialog, which ->
                        // Respond to item chosen
                        when (which) {
                            0 -> {
                                sharedPreferences.edit().putInt(
                                    getString(string.pref_key_mode_night),
                                    AppCompatDelegate.MODE_NIGHT_NO
                                ).apply()
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            }
                            1 -> {
                                sharedPreferences.edit().putInt(
                                    getString(string.pref_key_mode_night),
                                    AppCompatDelegate.MODE_NIGHT_YES
                                ).apply()
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            }
                            2 -> {
                                sharedPreferences.edit().putInt(
                                    getString(string.pref_key_mode_night),
                                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                ).apply()
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            }
                        }
                        dialog.dismiss()
                    }
                    .show()
                true
            }


            // View Saved Publications
            id.action_bookmark -> {
                startActivity(
                    Intent(
                        this@MainActivity,
                        FavoritesActivity::class.java
                    )
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // App Theme Selection (Light, Dark, and System Follow)
    private fun getThemeSelections(): Pair<Array<String>, Int> {
        val modeNight = sharedPreferences.getInt(
            getString(string.pref_key_mode_night),
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        val themeSelection =
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> {
                    if (modeNight != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) 0 else 2
                }
                Configuration.UI_MODE_NIGHT_YES -> {
                    if (modeNight != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) 1 else 2
                }
                else -> 2
            }

        return Pair(arrayOf("Light", "Dark", "System"), themeSelection)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (application as MyApplication).applyTheme()
        getOrientation()
        recreate()
    }

    // ClickListener in MainAdapter.kt, allows user to open publication
    // in installed PDF viewer or defaults to PDF-Viewer (bitmap converter so lower quality)
    override fun onPubsSelected(pubs: Pubs) {
        try{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(pubs.DocumentUrl), "application/pdf")
            startActivity(intent)

        } catch (e:ActivityNotFoundException){
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: incase of JAVA
                    applicationContext,
                    "${pubs.DocumentUrl}",     // PDF URL in String format
                    "${pubs.Title}",        // PDF Name/Title in String format
                    "",                 // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true              // This param is true by default.
                )
            )
        }
    }

    /**
     * Bug Fix for Version 1.3.2 / Version Code 18
     * Auto rotate issue even when turned off.
     * https://www.reddit.com/user/goldfishfollies
     */
    private fun getOrientation(){
        requestedOrientation =
            if (Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1){
                //Auto Rotate is on, so don't lock
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else{
                //Auto Rotate is off, so lock
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
    }

    // Fetches JSON from API
    //pubJSON is companion object below/web api for pubs
    private fun fetchPubs() {
        loading.visibility = View.VISIBLE

        request = JsonArrayRequest(Config.BASE_URL, { it ->
            val items: List<Pubs> =
                Gson().fromJson(it.toString(), object : TypeToken<List<Pubs>>() {}.type)

            val sortedItems = items.sortedWith(compareBy { it.Number })

            pubsList?.clear()
            pubsList?.addAll(sortedItems)

            /**
             * Hardcoded pubs (seperate source from api.afiexplorer.com
             * Current API Stops at 12 for the side loaded pub IDs
             * 13 Blue Book | 14 Brown Book | 15 Airman Blueprint | 16 CSAF Action Orders
             * Epoch time courtesy of https://www.epochconverter.com
             **/

            // Adds Blue Book
            pubsList?.add(
                Pubs(13,
                getString(string.blue_book),
                getString(string.blue_book_summary),
                "NA",
                "1652659402000",
                getString(string.blue_book_url),
                "AF/A1"
                )
            )

            // Adds Brown Book
            pubsList?.add(
                Pubs(14,
                getString(string.brown_book),
                getString(string.brown_book_summary),
                "NA",
                "1652659402000",
                getString(string.brown_book_url),
                "AF/A1"

                )
            )

            // Adds Airman Blueprint
            pubsList?.add(
                Pubs(15,
                getString(string.airman_blueprint),
                getString(string.airman_blueprint_summary),
                "NA",
                "1650614400000",
                getString(string.blueprint_url),
                "AF/A1"

                )
            )

            // Adds CSAF Action Orders
            pubsList?.add(
                Pubs(16,
                    getString(string.action_orders),
                    getString(string.action_orders_summary),
                    "NA",
                    "1644220800000",
                    getString(string.action_orders_url),
                    "AF/CC"
                )
            )

            /**
             * Update for Version 1.3.2 / Version Code 18
             * Adds AF Doctrine Pub 1
             * https://www.reddit.com/user/USAFDoctrine/
             */
            pubsList?.add(
                Pubs(17,
                getString(string.AFDP1),
                getString(string.AFDP1_summary),
                "NA",
                "1615406400000",
                getString(string.AFDP1_url),
                "LeMay Center for Doctrine Development"
                )
            )

            recyclerView?.recycledViewPool?.clear()
            adapter?.notifyDataSetChanged()
            loading.visibility = View.GONE
            setupData(pubsList!!)

        }) {
            println(it.printStackTrace())
            Toasty.error(applicationContext, getString(string.no_internet), Toast.LENGTH_SHORT, true).show()
        }
        MyApplication.instance.addToRequestQueue(request!!)
    }

    private fun setupData(pubsList: ArrayList<Pubs>) {
        adapter = MainAdapter(applicationContext, pubsList, this)
        recyclerView?.adapter = adapter
    }

    // Configures Toasty Library
    private fun toastyConfig(){
        val typeface: Typeface? = ResourcesCompat.getFont(applicationContext, font.ibm_plex_sans)

        Toasty.Config.getInstance()
            .setTextSize(15)
            .setToastTypeface(typeface!!)
            .supportDarkTheme(true)
            .apply()
    }

    companion object {

        // Database to save frequent pubs
        var favoriteDatabase: FavoriteDatabase? = null
    }
}
