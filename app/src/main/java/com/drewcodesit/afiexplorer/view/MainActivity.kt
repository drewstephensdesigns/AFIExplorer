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
import android.widget.CompoundButton
import android.widget.RadioButton
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
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.R.*
import com.drewcodesit.afiexplorer.adapters.MainAdapter
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.MyDividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.bottom_sheet_filter.*
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
        setUpBottomSheet()
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
    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun fetchPubs() {
        loading.visibility = View.VISIBLE

        request = JsonArrayRequest(Config.BASE_URL, { it ->
            val items: List<Pubs> =
                Gson().fromJson(it.toString(), object : TypeToken<List<Pubs>>() {}.type)

            val sortedItems = items.sortedWith(compareBy { it.Number })

            pubsList?.clear()
            pubsList?.addAll(sortedItems)

           // Hardcoded pubs moved to Publications Gitlab Repo
           // https://gitlab.com/afi-explorer/pubs

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

    /**
     *
     * @param pubsList ArrayList<Pubs>
     */
    private fun setupData(pubsList: ArrayList<Pubs>) {
        adapter = MainAdapter(applicationContext, pubsList, this)
        recyclerView?.adapter = adapter
    }

    // Bottom Sheet Dialog for Filtering
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("InflateParams", "NotifyDataSetChanged")
    private fun setUpBottomSheet(){
        fabFilter.setOnClickListener{
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

            bottomSheetDialog = BottomSheetDialog(this@MainActivity)
            bottomSheetDialog.setContentView(dialogView)
            bottomSheetDialog.show()

            // All Pubs (Reset)
            cbALL.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if(isChecked) {
                    fetchPubs()
                    supportActionBar?.title = resources.getString(string.app_name)
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // HAF Level Pubs (AF/ HAF/ SAF/)
            cbHAF.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if(isChecked) {
                    adapter?.filter?.filter(showListByOrg("AF/"))
                    supportActionBar?.title = "HAF Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // LeMay Center
            cbLeMayCenter.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("LeMay Center"))
                    supportActionBar?.title = "LeMay Center"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // ACC
            cbACC.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("ACC"))
                    supportActionBar?.title = "ACC Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // AMC
            cbAMC.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("AMC"))
                    supportActionBar?.title = "AMC Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // AETC
            cbAETC.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("AETC"))
                    supportActionBar?.title = "AETC Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // AFMC
            cbAFMC.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("AFMC"))
                    supportActionBar?.title = "AFMC Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // AFSOC
            cbAFSOC.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("AFSOC"))
                    supportActionBar?.title = "AFSOC Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // AFGSC
            cbAFGSC.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("AFGSC"))
                    supportActionBar?.title = "AFGSC Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // USAFE
            cbUSAFE.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("USAFE-AFAFRICA"))
                    supportActionBar?.title = "USAFE Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // PACAF
            cbPACAF.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("PACAF"))
                    supportActionBar?.title = "PACAF Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }

            // AFRC
            cbAFRC.setOnCheckedChangeListener{_: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    adapter?.filter?.filter(showListByOrg("AFRC"))
                    supportActionBar?.title = "AFRC Pubs"
                    bottomSheetDialog.dismiss()
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    /**
     * @param rescindOrg String
     * @return CharSequence
     */
    private fun showListByOrg(rescindOrg: String): CharSequence {
        when(rescindOrg){
            "AF/" ->{
                pubsList?.filter {
                    it.RescindOrg == "AF/" ||
                    it.RescindOrg == "SAF/" ||
                    it.RescindOrg == "AF/A" ||
                    it.RescindOrg == "HAF/" ||
                    it.RescindOrg == "HQ/" ||
                    it.RescindOrg == "DOD/"
                }
            }

            "ACC" ->{ pubsList?.filter { it.RescindOrg == "ACC" } }
            "AMC" ->{ pubsList?.filter { it.RescindOrg == "AMC" } }
            "AFGSC" ->{ pubsList?.filter { it.RescindOrg == "AFGSC" } }
            "AFRC" ->{ pubsList?.filter { it.RescindOrg == "AFRC" } }
            "USAFE-AFAFRICA" ->{ pubsList?.filter { it.RescindOrg == "USAFE-AFAFRICA" } }
            "PACAF" ->{ pubsList?.filter { it.RescindOrg == "PACAF" } }
            "AETC" ->{ pubsList?.filter { it.RescindOrg == "AETC" } }
            "AFMC" ->{ pubsList?.filter { it.RescindOrg == "AFMC" } }
            "AFSOC" ->{ pubsList?.filter { it.RescindOrg == "AFSOC" } }

            "LeMay Center || LeMay Center for Doctrine Development" -> { pubsList?.filter {
                it.RescindOrg == "LeMay Center" ||
                it.RescindOrg == "LeMay Center for Doctrine Development"}
            }
        }
        return rescindOrg
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

    // Refreshes filtered feed on back press
    // Exits app if back-pressed x2
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBackPressed() {
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

    companion object {

        // Database to save frequent pubs
        var favoriteDatabase: FavoriteDatabase? = null
    }
}
