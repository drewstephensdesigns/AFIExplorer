package com.drewcodesit.afiexplorer.view

import android.annotation.SuppressLint
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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.drewcodesit.afiexplorer.MyApplication
import com.drewcodesit.afiexplorer.MyApplication.Companion.TAG
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.R.*
import com.drewcodesit.afiexplorer.adapters.MainAdapter
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.databinding.MainActivityBinding
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.MyDividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty
import java.util.*


// PubsAdapterListener opens publications
class MainActivity : AppCompatActivity(), MainAdapter.PubsAdapterListener {

    // Migration to View Binding from Kotlin Synthetics
    // Source: https://medium.com/codex/android-viewbinding-migration-16070e24b752
    private lateinit var _binding: MainActivityBinding

    private var pubsList: ArrayList<Pubs>? = null
    private var adapter: MainAdapter? = null
    private var searchView: SearchView? = null
    private var request: JsonArrayRequest? = null
    private var exit: Boolean = false

    // Shared Prefs
    private lateinit var sharedPreferences: SharedPreferences

    // BottomSheet for Filtering
    private lateinit var bottomSheetDialog: BottomSheetDialog

    // Radio Buttons for Filtering
    private lateinit var cbALL: RadioButton
    private lateinit var cbHAF: RadioButton
    private lateinit var cbLeMayCenter: RadioButton
    private lateinit var cbACC: RadioButton
    private lateinit var cbAMC: RadioButton
    private lateinit var cbPACAF: RadioButton
    private lateinit var cbAFMC: RadioButton
    private lateinit var cbAFSOC: RadioButton
    private lateinit var cbUSAFE: RadioButton
    private lateinit var cbAFRC: RadioButton
    private lateinit var cbAFGSC: RadioButton
    private lateinit var cbAETC: RadioButton
    private lateinit var cbTravel: RadioButton

    //The OnBackPressedDispatcher is a class that allows you
    // to register a OnBackPressedCallback to a LifecycleOwner
    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun handleOnBackPressed() {
            closeOrRefreshApp()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding
        _binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        toastyConfig()

        setSupportActionBar(_binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(true)
            title = resources.getString(string.app_name)
        }

        // View Binding
        //recyclerView = _binding.contentMain.recyclerView
       _binding.contentMain.recyclerView.layoutManager = LinearLayoutManager(this)
        _binding.contentMain.recyclerView.apply {
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
        setUpBottomSheet()

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
                    adapter?.filter?.filter(newText!!) {
                        when (adapter?.itemCount) {
                            0 -> {
                                // Kotlin View Binding
                                _binding.contentMain.noResultsFound.visibility = View.VISIBLE
                                _binding.contentMain.noResultsFound.text = getString(string.no_results_found, newText)
                            }
                            else -> {
                                _binding.contentMain.noResultsFound.visibility = View.GONE
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

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

           // Search function for looking up publications
           id.action_search -> {
               true
           }

           // App Feedback Links
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
            // Checks if Document URL from E-Pubs is restricted and shows Toast Message indicating
            // Official Source Doc can be found through E-Pubs Website
            if (pubs.DocumentUrl?.contains("generic_restricted.pdf") == true
                || (pubs.DocumentUrl?.contains("restricted_access.pdf")) == true
                || (pubs.DocumentUrl?.contains("for_official_use_only.pdf")) == true
                || (pubs.DocumentUrl?.contains("generic_fouo.pdf")) == true
                || (pubs.DocumentUrl?.contains("stocked_and_issued")) == true
                || (pubs.DocumentUrl?.contains("generic_opr1.pdf")) == true
                || (pubs.DocumentUrl?.contains("generic_opr.pdf")) == true) {
                Toasty.error(applicationContext, getString(string.pub_restricted), Toast.LENGTH_SHORT, false).show()
            } else{
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(pubs.DocumentUrl), "application/pdf")
                startActivity(intent)
            }

        } catch (e:ActivityNotFoundException){
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: incase of JAVA
                    applicationContext,
                    "${pubs.DocumentUrl}",     // PDF URL in String format
                    "${pubs.Title}",          // PDF Name/Title in String format
                    "",                  // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true             // This param is true by default.
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
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("NotifyDataSetChanged")
    private fun fetchPubs() {
        _binding.contentMain.loading.visibility = View.VISIBLE

        request = JsonArrayRequest(
            Request.Method.GET,
            Config.BASE_URL,
            null,{ response ->
                val items: List<Pubs> =
                    Gson().fromJson(response.toString(), object : TypeToken<List<Pubs>>() {}.type)

                //val sortedItems = items.sortedWith(compareBy { it.Number })
                val sortedItems = items.sortedByDescending {
                    it.getCertDate()
                }

                pubsList?.clear()
                pubsList?.addAll(sortedItems)

                // Hardcoded pubs moved to Publications Gitlab Repo
                // https://gitlab.com/afi-explorer/pubs

                _binding.contentMain.recyclerView.recycledViewPool.clear()
                adapter?.notifyDataSetChanged()
                _binding.contentMain.loading.visibility = View.GONE
                setupData()
            },
            {error ->
                println(error.printStackTrace())
                Toasty.error(applicationContext, getString(string.no_internet), Toast.LENGTH_SHORT, true).show()
            }
        )
        MyApplication.instance.addToRequestQueue(request!!)
    }

    private fun setupData(){
        adapter = MainAdapter(applicationContext, pubsList!!, this)
        _binding.contentMain.recyclerView.adapter = adapter
    }

    // Bottom Sheet Dialog for Filtering
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("InflateParams")
    private fun setUpBottomSheet(){
        _binding.fabFilter.setOnClickListener{
            val dialogView = layoutInflater.inflate(layout.bottom_sheet_filter, null)

            // radio buttons in bottom_sheet_filter.xml
            cbALL = dialogView.findViewById(id.cbALL)
            cbHAF = dialogView.findViewById(id.cbHAF)
            cbLeMayCenter = dialogView.findViewById(id.cbLeMayCenter)
            cbACC = dialogView.findViewById(id.cbACC)
            cbAMC = dialogView.findViewById(id.cbAMC)
            cbAETC = dialogView.findViewById(id.cbAETC)
            cbAFMC = dialogView.findViewById(id.cbAFMC)
            cbAFSOC = dialogView.findViewById(id.cbAFSOC)
            cbAFGSC = dialogView.findViewById(id.cbAFGSC)
            cbUSAFE = dialogView.findViewById(id.cbUSAFE)
            cbPACAF = dialogView.findViewById(id.cbPACAF)
            cbAFRC= dialogView.findViewById(id.cbAFRC)
            cbTravel = dialogView.findViewById(id.cbTravel)

            bottomSheetDialog = BottomSheetDialog(this@MainActivity)
            bottomSheetDialog.setContentView(dialogView)
            bottomSheetDialog.show()

            val filterMap = mapOf(
                cbALL to Pair("", resources.getString(string.app_name)),
                cbHAF to Pair("AF/", "HAF"),
                cbLeMayCenter to Pair("LeMay Center", "Doctrine"),
                cbACC to Pair("ACC", "ACC"),
                cbAMC to Pair("AMC", "AMC"),
                cbAETC to Pair("AETC", "AETC"),
                cbAFMC to Pair("AFMC", "AFMC"),
                cbAFSOC to Pair("AFSOC", "AFSOC"),
                cbAFGSC to Pair("AFGSC", "AFGSC"),
                cbUSAFE to Pair("USAFE-AFAFRICA", "USAFE-AFAFRICA"),
                cbPACAF to Pair("PACAF", "PACAF"),
                cbAFRC to Pair("AFRC", "AFRC"),
                cbTravel to Pair("DoD", "DoD")
            )

            for ((checkbox, filter) in filterMap) {
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        updateFilter(filter.first, filter.second)
                        bottomSheetDialog.dismiss()
                    }
                }
            }
        }
    }

    private fun updateFilter(org: String, title: String) {
        adapter?.filterByRescindOrg()?.filter(org)
        supportActionBar?.title = title
        bottomSheetDialog.dismiss()
    }

    private fun toastyConfig(){
        val typeface: Typeface? = ResourcesCompat.getFont(applicationContext, font.ibm_plex_sans)

        Toasty.Config.getInstance()
            .setTextSize(15)
            .setToastTypeface(typeface!!)
            .supportDarkTheme(true)
            .apply()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun closeOrRefreshApp(){
        supportActionBar?.title = getString(string.app_name)
        fetchPubs()
        if (exit){
            finish() // finish activity
        }else{
            Toasty.normal(this, getString(string.action_exit_app)).show()
            exit = true
            Handler(Looper.getMainLooper()).postDelayed({ exit = false }, 3 * 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: --------")
    }

    companion object {

        // Database to save frequent pubs
        var favoriteDatabase: FavoriteDatabase? = null
    }
}
