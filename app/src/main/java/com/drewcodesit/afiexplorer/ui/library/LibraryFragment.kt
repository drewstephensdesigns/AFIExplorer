package com.drewcodesit.afiexplorer.ui.library

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.favorites.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FragmentLibraryBinding
import com.maxkeppeler.sheets.core.ButtonStyle
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.lottie.LottieAnimation
import com.maxkeppeler.sheets.lottie.withCoverLottieAnimation
import com.rajat.pdfviewer.PdfViewerActivity
import androidx.core.net.toUri
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.objects.LineDividerItemDecoration
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDAO
import kotlinx.coroutines.launch


class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private var favesAdapter: LibraryAdapter? = null

    private var selectedSortOption = 0 // 0 = Title, 1 = Number
    private val PREFS_NAME = "library_prefs"
    private val PREF_SORT_OPTION = "sort_option"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val favoriteViewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory(FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        selectedSortOption = getSavedSortOption()
        favoriteViewModel.updateSortOption(selectedSortOption) // Send to ViewModel
        initMenu()
        initUI()
        fetchFaves()

        return binding.root
    }

    // Initializes the menu for the by inflating a menu resource and setting up a MenuProvider
    // to handle menu creation, visibility, and item selection.
    private fun initMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_fave_actions, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filter_faves -> { filterOptions(); true }
                    R.id.action_clear_database -> { nukeDatabase(); true }
                    else -> { false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initUI() {
        binding.rvFavorites.layoutManager = LinearLayoutManager(context)
        binding.rvFavorites.setHasFixedSize(true)
        binding.rvFavorites.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                LineDividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL,
                    36
                )
            )
        }
    }

    private fun fetchFaves() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                favoriteViewModel.favorites.collect { sortedFavorites ->
                    favesAdapter = LibraryAdapter(
                        requireContext(),
                        sortedFavorites.toMutableList(),
                        onSelectItemClick = { entity -> openFavorite(entity) },
                        onDeleteClick = { entity -> deleteFavorite(entity) }
                    )
                    binding.rvFavorites.adapter = favesAdapter

                    if (sortedFavorites.isEmpty()) {
                        binding.emptyFavesInfoImg.visibility = View.VISIBLE
                        binding.emptyFavesInfoText.visibility = View.VISIBLE
                        binding.emptyFavesInfoText.text = getString(R.string.no_results_found_db)
                    } else {
                        binding.emptyFavesInfoImg.visibility = View.GONE
                        binding.emptyFavesInfoText.visibility = View.GONE
                    }
                }
            }
        }
    }

    //  Prompts user for confirmation before deleting the app's database using an InfoSheet dialog
    // with informative content and a caution animation.
    private fun deleteFavorite(favorite: FavoriteEntity) {
        favoriteViewModel.deleteFavorite(favorite)
        Config.showToast(
            requireContext(),
            getString(R.string.delete_hint, favorite.pubNumber),
            ToastType.INFO,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error)
        )
    }

    private fun nukeDatabase() {
        InfoSheet().show(requireContext()) {
            style(SheetStyle.DIALOG)
            title("Delete Database?")
            content(R.string.action_nuke_database)
            withCoverLottieAnimation(LottieAnimation {
                setupAnimation {
                    setAnimation(R.raw.caution_anim)
                }
            })
            onNegative("Not Yet") { }
            onPositive("Ok") {
                favoriteViewModel.deleteAllFavorites()
                Config.showToast(
                    requireContext(),
                    "Database cleared!",
                    ToastType.INFO,
                    AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error)
                )
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
    private fun openFavorite(favorite: FavoriteEntity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(favorite.pubDocumentUrl.toUri(), "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                PdfViewerActivity.launchPdfFromUrl(
                    requireContext(),
                    favorite.pubDocumentUrl,
                    favorite.pubNumber,
                    "",
                    enableDownload = true
                )
            )
        }
    }


    private fun filterOptions() {
        InputSheet().show(requireContext()) {
            style(SheetStyle.BOTTOM_SHEET)
            title("Sort By")
            with(InputRadioButtons {
                options(mutableListOf("Publication Title", "Publication Number"))
                changeListener { value ->
                    selectedSortOption = value
                    saveSortOption(value)
                    favoriteViewModel.updateSortOption(value)
                }
            })
        }
    }

    private fun saveSortOption(option: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(PREF_SORT_OPTION, option) }
        Log.d("LibraryFragment", "Saved sort option: $option")
    }

    private fun getSavedSortOption(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getInt(PREF_SORT_OPTION, 0)
        Log.d("LibraryFragment", "Loaded saved sort option: $value")
        return value // default to 0 (Title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}