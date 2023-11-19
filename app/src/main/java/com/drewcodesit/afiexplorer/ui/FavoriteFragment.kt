package com.drewcodesit.afiexplorer.ui

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.adapters.FavesAdapter
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FragmentFavesBinding
import com.drewcodesit.afiexplorer.utils.FavesClickListener
import com.drewcodesit.afiexplorer.utils.MyDividerItemDecoration
import com.maxkeppeler.sheets.core.ButtonStyle
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.lottie.LottieAnimation
import com.maxkeppeler.sheets.lottie.withCoverLottieAnimation
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty

class FavoriteFragment : Fragment(),
    FavesClickListener,
    MenuProvider {

    private var _binding: FragmentFavesBinding? = null
    private val binding get() = _binding!!
    private var favesAdapter: FavesAdapter? = null
    private var searchViewFaves: SearchView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFavesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuFaves()
        initUI()
        fetchFaves()
        setupBottomMenu()

    }

    private fun setupMenuFaves(){
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            this,           // your Fragment implements MenuProvider, so we use this here
            viewLifecycleOwner,     // Only show the Menu when your Fragment's View exists
            Lifecycle.State.RESUMED // And when the Fragment is Created
        )
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_faves, menu)
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchViewFaves = menu.findItem(R.id.action_search_faves)?.actionView as SearchView
        searchViewFaves?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            setIconifiedByDefault(true)
            maxWidth = Int.MAX_VALUE

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchViewFaves?.clearFocus()
                    return false
                }

                // Source: https://stackoverflow.com/questions/61570173/display-no-results-found
                override fun onQueryTextChange(newText: String?): Boolean {
                    favesAdapter?.filter?.filter(newText){i ->
                        if (i == 0){
                            binding.rvFavorites.visibility = View.GONE

                            // Displays the lottie animation
                            binding.noResultsFound.visibility = View.VISIBLE
                            binding.noResultsFoundText.visibility = View.VISIBLE
                            binding.noResultsFoundText.text = resources.getString(R.string.no_results_found, newText)
                        }else{
                            binding.rvFavorites.visibility = View.VISIBLE

                            // Hides the lottie animation
                            binding.noResultsFound.visibility = View.GONE
                            binding.noResultsFound.visibility = View.GONE
                            binding.noResultsFoundText.visibility = View.GONE

                        }
                    }
                    return false
                }
            })
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId){
            R.id.action_search_faves ->{
                true
            }
            R.id.action_clear_database ->{
                nukeDatabase()
                true
            }
            else -> false
        }
    }

    private fun initUI(){
        binding.rvFavorites.layoutManager = LinearLayoutManager(context)
        binding.rvFavorites.setHasFixedSize(true)
        binding.rvFavorites.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                MyDividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL,
                    36
                )
            )
        }
    }

    private fun fetchFaves(){
        val favorites = FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()?.getFavoriteData()

        Log.i("---- FETCHING FAVORITES", "$favorites")

        favesAdapter = FavesAdapter(requireContext(), favorites!! , this, this)
        binding.rvFavorites.adapter = favesAdapter

        // Show or Hide Empty State
        if (favorites.isEmpty()) {
            // Kotlin View Binding
            binding.emptyFavesInfoImg.visibility = View.VISIBLE
            binding.emptyFavesInfoText.visibility = View.VISIBLE
            binding.emptyFavesInfoText.text = getString(R.string.no_results_found_db)
            binding.fabFilterFaves.hide()

        } else {
            Log.i("FAVORITES", "Current Size: ${favorites.size}")
            // Kotlin View Binding
            binding.emptyFavesInfoImg.visibility = View.GONE
            binding.emptyFavesInfoText.visibility = View.GONE
            if(favorites.size > 5){
                binding.fabFilterFaves.show()
            } else {
                binding.fabFilterFaves.hide()
            }
            favesAdapter?.filterByTitle()
        }
    }

    // Allows user to sort favorites by title or number
    private fun setupBottomMenu() {
        binding.fabFilterFaves.setOnClickListener {
            InputSheet().show(requireContext()){
                title("Sort By")
                with(InputRadioButtons {
                    options(mutableListOf("Publication Title", "Publication Number"))
                    changeListener { value ->
                        when(value){
                            0 -> {favesAdapter?.filterByTitle()}
                            1 -> {favesAdapter?.filterByNumber()}
                        }
                    }
                })
            }
        }
    }

    // Delete Entire Database
    private fun nukeDatabase(){
        InfoSheet().show(requireContext()){
            style(SheetStyle.DIALOG)
           // withIconButton(IconButton( R.drawable.ic_error)){}
            title("Delete Database?")
            content(R.string.action_nuke_database)
            withCoverLottieAnimation(LottieAnimation {
                setupAnimation {
                    setAnimation(R.raw.caution_anim)
                }
            })
            onNegative(
                "Not Yet",
            ) { /* Set listener when negative button is clicked. */ }
            onPositive("Ok") {
                FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()?.deleteAll()
                fetchFaves()
                showDeleteToast("Database cleared!")
            }
            positiveButtonStyle(ButtonStyle.OUTLINED)
            negativeButtonStyle(ButtonStyle.NORMAL)
            displayNegativeButton(true)
            displayPositiveButton(true)
        }
    }

    // Opens document from the Favorites Screen
    // If PDF Reader installed the doc will open natively
    // Else falls back to the PDFViewer Activity
    override fun onFavesSelectedListener(faves: FavoriteEntity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(faves.DocumentUrl), "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(      //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: incase of JAVA
                    requireContext(),
                    faves.DocumentUrl,       // PDF URL in String format
                    faves.Number,           // PDF Name/Title in String format
                    "",                     // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true                // This param is true by default.
                )
            )
        }
    }

    // Deletes single row from database and refreshes the list
    override fun onFavesDeletedListener(faveSingleItemDelete: FavoriteEntity, position: Int) {
        FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()?.delete(faveSingleItemDelete)
        showDeleteToast("You deleted ${faveSingleItemDelete.Number}")
        favesAdapter?.notifyItemRemoved(position)
        //favesAdapter?.notifyItemChanged(position)
        fetchFaves()
    }

    private fun showDeleteToast(message: String){
        Toasty.info(requireContext(), message, R.drawable.ic_error).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
