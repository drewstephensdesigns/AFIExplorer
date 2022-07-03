package com.drewcodesit.afiexplorer.main

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.android.volley.toolbox.JsonArrayRequest
import com.drewcodesit.afiexplorer.MyApplication
import com.drewcodesit.afiexplorer.MyApplication.Companion.TAG
import com.drewcodesit.afiexplorer.Pubs
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.R.*
import com.drewcodesit.afiexplorer.SettingsActivity
import com.drewcodesit.afiexplorer.adapters.MainAdapter
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.favorites.FavoritesActivity
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

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(true)
            title = resources.getString(string.app_name)
        }

        recyclerView = findViewById<View?>(id.recycler_view) as RecyclerView
        recyclerView!!.setHasFixedSize(true)
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

        favoriteDatabase = Room.databaseBuilder(
            applicationContext, FavoriteDatabase::class.java,
            "myfavdb"
        ).allowMainThreadQueries().build()

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
            setIconifiedByDefault(false)
            maxWidth = Int.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    //adapter?.filter?.filter(newText!!)
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

    // Fetches JSON from API
    //pubJSON is companion object below/web api for pubs
    private fun fetchPubs() {
        loading.visibility = View.VISIBLE
        request = JsonArrayRequest(pubJSON, { it ->
            val items: List<Pubs> =
                Gson().fromJson(it.toString(), object : TypeToken<List<Pubs>>() {}.type)

            val sortedItems = items.sortedWith(compareBy { it.Number })

            pubsList?.clear()
            pubsList?.addAll(sortedItems)
            recyclerView?.recycledViewPool?.clear()
            adapter?.notifyDataSetChanged()
            loading.visibility = View.GONE
            setupData(pubsList!!)

        }) {
            println(it.printStackTrace())
            Toasty.error(applicationContext, "Error: " + it.message, Toast.LENGTH_SHORT, true)
                .show()
        }
        MyApplication.instance.addToRequestQueue(request!!)
    }

    private fun setupData(pubsList: ArrayList<Pubs>) {
        adapter = MainAdapter(applicationContext, pubsList, this)
        recyclerView?.adapter = adapter
    }

    companion object {
        // API currently used to display Departmental level AFIs/Pubs
        private const val pubJSON = "https://api.afiexplorer.com/v2/"

        // Database to save frequent pubs
        var favoriteDatabase: FavoriteDatabase? = null
    }
}