package com.drewcodesit.afiexplorer.view

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.adapters.FavoriteAdapter
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FavesActivityBinding
import com.drewcodesit.afiexplorer.utils.MyDividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty.info

// FavAdapterListener opens saved publications
// ItemListener deletes saved publications
class FavoritesActivity : AppCompatActivity(),
    FavoriteAdapter.FavAdapterListener,
    FavoriteAdapter.ItemListener{

    private lateinit var rv: RecyclerView
    private lateinit var favAdapter: FavoriteAdapter
    private lateinit var searchView: SearchView

    // BottomSheet for Filtering
    private lateinit var bottomSheetDialog: BottomSheetDialog

    // Radio Buttons for Filtering
    private lateinit var cbSortByTitle: RadioButton
    private lateinit var cbSortByNumber: RadioButton
    private lateinit var _binding: FavesActivityBinding

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FavesActivityBinding.inflate(layoutInflater)

        setContentView(_binding.root)

        setSupportActionBar(_binding.toolbar2)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = resources.getString(R.string.app_faves)
        }

        rv = _binding.contentFaves.rvFavorites
        rv.layoutManager = LinearLayoutManager(this)
        rv.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                MyDividerItemDecoration(
                    this@FavoritesActivity,
                    DividerItemDecoration.VERTICAL,
                    36
                )
            )
        }
        getFaves()
        setUpBottomSheet()
    }

    private fun getFaves() {
        val favorites =
            FavoriteDatabase.getDatabase(applicationContext).favoriteDAO()!!.getFavoriteData()

        Log.e("FAVORITES", "$favorites")

        favAdapter = FavoriteAdapter(this, favorites, this, this)
        rv.adapter = favAdapter

        // Show or Hide Empty State
        if (favorites!!.isEmpty()) {
            // Kotlin View Binding
            _binding.contentFaves.emptyInfoImg.visibility = View.VISIBLE
            _binding.contentFaves.emptyInfo.visibility = View.VISIBLE
            _binding.contentFaves.emptyInfo.text = getString(R.string.no_results_found_db)

            _binding.fabFilterFaves.hide()
        } else {
            Log.i("FAVORITES", "Current Size: ${favorites.size}")
            // Kotlin View Binding
            _binding.contentFaves.emptyInfoImg.visibility = View.GONE
            _binding.contentFaves.emptyInfo.visibility = View.GONE
            _binding.fabFilterFaves.show()

            favAdapter.filterByTitle()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_faves, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu.findItem(R.id.action_search)?.actionView as SearchView

        searchView.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            setIconifiedByDefault(false)
            maxWidth = Int.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    favAdapter.filter.filter(newText!!)
                    Log.i("MAIN_ACTIVITY", newText)
                    return false
                }
            })
        }
        return true
    }

    // Returns to Main Activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_clear_database -> {
                AlertDialog.Builder(this).apply {
                    setTitle("Delete Favorites")
                    setMessage("This will clear the database, are you sure you want to continue?")
                    setPositiveButton("DELETE") { _, _ ->
                        FavoriteDatabase.getDatabase(applicationContext).favoriteDAO()!!.deleteAll()
                        favAdapter.notifyDataSetChanged()
                        info(
                            applicationContext,
                            "Database Cleared!",
                            Toast.LENGTH_SHORT,
                            true
                        ).show()
                        finish()
                        overridePendingTransition(0, 0)
                        startActivity(intent)
                        overridePendingTransition(0, 0)

                    }
                    setNegativeButton("CANCEL", null)
                }.create().show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Opens PDF, if Google Drive/PDF reader is installed
    // PDF will open there, if not, PDF will open in Pdf-Viewer
    // Library Source: https://github.com/afreakyelf/Pdf-Viewer
    override fun onFavsSelected(fav: FavoriteEntity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(fav.DocumentUrl), "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: incase of JAVA
                    applicationContext,
                    "${fav.DocumentUrl}",        // PDF URL in String format
                    "${fav.Number}",            // PDF Name/Title in String format
                    "",                    // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true               // This param is true by default.
                )
            )
        }
    }

    @SuppressLint("InflateParams", "NotifyDataSetChanged")
    private fun setUpBottomSheet(){
        _binding.fabFilterFaves.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_filter_faves, null)

            // radio buttons in bottom_sheet_filter.xml
            cbSortByTitle = dialogView.findViewById(R.id.cbSortByTitle)
            cbSortByNumber = dialogView.findViewById(R.id.cbSortByNumber)

            bottomSheetDialog = BottomSheetDialog(this@FavoritesActivity)
            bottomSheetDialog.setContentView(dialogView)
            bottomSheetDialog.show()

            cbSortByTitle.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean->
                if(isChecked){
                    favAdapter.filterByTitle()
                    cbSortByTitle.isChecked = true
                    cbSortByNumber.isChecked = false
                    favAdapter.notifyDataSetChanged()
                }
            }

            cbSortByNumber.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean->
                if(isChecked){
                    favAdapter.filterByNumber()
                    cbSortByNumber.isChecked = true
                    cbSortByTitle.isChecked = false
                    favAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    // Deletes Single Items from Database
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onItemClicked(favsListToDelete: FavoriteEntity, position: Int) {
        FavoriteDatabase.getDatabase(applicationContext).favoriteDAO()!!.delete(favsListToDelete)
        info(
            applicationContext,
            "You deleted ${favsListToDelete.Number}! ",
            Toast.LENGTH_SHORT,
            true
        ).show()
        overridePendingTransition(0, 0)
        startActivity(intent)
        rv.recycledViewPool.clear()
        favAdapter.notifyItemRemoved(position)
        favAdapter.filterByNumber()
        overridePendingTransition(0, 0)
        finish()
    }
}
