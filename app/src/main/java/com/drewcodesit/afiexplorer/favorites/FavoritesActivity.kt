

package com.drewcodesit.afiexplorer.favorites

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.adapters.FavoriteAdapter
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.main.MainActivity
import com.drewcodesit.afiexplorer.utils.MyDividerItemDecoration
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty.info
import kotlinx.android.synthetic.main.content_faves.*
import kotlinx.android.synthetic.main.faves_activity.*


// FavAdapterListener opens saved publications
// ItemListener deletes saved publications
class FavoritesActivity : AppCompatActivity(),
    FavoriteAdapter.FavAdapterListener,
    FavoriteAdapter.ItemListener {

    private lateinit var rv: RecyclerView
    private lateinit var favAdapter: FavoriteAdapter
    private var favorites: MutableList<FavoriteEntity?> =
        MainActivity.favoriteDatabase?.favoriteDAO()?.getFavoriteData()!!
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.faves_activity)
        setSupportActionBar(toolbar2)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = resources.getString(R.string.app_faves)
        }

        rv = findViewById<View?>(R.id.rv_favorites) as RecyclerView
        rv.setHasFixedSize(true)
        rv.layoutManager = LinearLayoutManager(this)

        rv.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(MyDividerItemDecoration(this@FavoritesActivity, DividerItemDecoration.VERTICAL, 36))
        }
        getFaves()
    }

    private fun getFaves(){
        favAdapter = FavoriteAdapter(this,favorites, this, this)
        rv.adapter = favAdapter

        // Show or Hide Empty State
        if (favorites.isEmpty()){
            emptyInfoImg.visibility = View.VISIBLE
            emptyInfo.visibility = View.VISIBLE
            emptyInfo.text = getString(R.string.no_results_found_db)

        } else {
            emptyInfoImg.visibility = View.GONE
            emptyInfo.visibility = View.GONE

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean{
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
        return when (item.itemId){
            android.R.id.home -> {
                onBackPressed()
                true
            }

            R.id.action_clear_database -> {
                AlertDialog.Builder(this).apply {
                    setTitle("Delete Favorites")
                    setMessage("This will clear the database, are you sure you want to continue?")
                    setPositiveButton("DELETE") {_,_ ->
                        MainActivity.favoriteDatabase!!.favoriteDAO()!!.deleteAll()
                        favAdapter.notifyDataSetChanged()
                        info(applicationContext, "Database Cleared!", Toast.LENGTH_SHORT, true).show()
                        finish()
                        overridePendingTransition(0,0)
                        startActivity(intent)
                        overridePendingTransition(0,0)

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
        try{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(fav.DocumentUrl), "application/pdf")
            startActivity(intent)
        } catch (e:ActivityNotFoundException){
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: incase of JAVA
                    applicationContext,
                    "${fav.DocumentUrl}",   // PDF URL in String format
                    "${fav.Number}",       // PDF Name/Title in String format
                    "",               // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true          // This param is true by default.
                )
            )
        }
    }

    // Click event for trashcan icon in
    // favorites_list_item to delete faved pubs
    override fun onItemClicked(favsListToDelete: FavoriteEntity, position:Int) {
        MainActivity.favoriteDatabase!!.favoriteDAO()!!.delete(favsListToDelete)
        info(applicationContext, "You deleted ${favsListToDelete.Number}! ", Toast.LENGTH_SHORT, true).show()

        // When deleting a faved pub, allows screen to
        // refresh changes without being visible to user
        finish()
        overridePendingTransition( 0, 0)
        startActivity(intent)
        rv.recycledViewPool.clear()
        favAdapter.notifyItemRemoved(position)
        overridePendingTransition( 0, 0)
    }
}
