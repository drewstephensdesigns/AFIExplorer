package com.drewcodesit.afiexplorer.ui.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FragmentLibraryBinding
import com.drewcodesit.afiexplorer.ui.browse.BrowseViewModel
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.Config.deleteFavorite
import com.drewcodesit.afiexplorer.utils.Config.toPubs
import com.drewcodesit.afiexplorer.utils.objects.ActionsBottomSheet
import com.drewcodesit.afiexplorer.utils.objects.LibraryPrefs
import com.drewcodesit.afiexplorer.utils.objects.LibrarySortMode
import com.drewcodesit.afiexplorer.utils.objects.SortActionsBottomSheet
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private var favesAdapter: LibraryAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        initMenu()
        initUI()
        fetchFaves()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                LibraryPrefs.sortModeFlow(requireContext()).collect { mode ->
                    applySort(mode)
                }
            }
        }
    }

    // Controls sorting method
    private fun applySort(mode: LibrarySortMode){
        when (mode){
            LibrarySortMode.TITLE ->
                favesAdapter?.sortFavorites()

            LibrarySortMode.NUMBER ->
                favesAdapter?.sortFavoritesByNumber()
        }
        binding.rvFavorites.scrollToPosition(0)
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
                    R.id.action_filter_faves -> { showSortActionsBottomSheet(); true }
                    else -> {
                        false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initUI() {
        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
        }
    }

    // Loads favorites from local database
    // Initializes the adapter with item clock and action handlers
    // and toggles empty-state ui to the user
    private fun fetchFaves() {
        val dao = FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()
        val favorites = dao?.getFavoriteData()?.toMutableList() ?: mutableListOf()

        favesAdapter = LibraryAdapter(
            requireContext(),
            favorites,
            onSelectItemClick = { entity -> openFavorite(entity) },
            onLibraryActionsClick = { entity -> showActionsBottomSheetForFavorite(entity)}
        )
        binding.rvFavorites.adapter = favesAdapter

        // Show/hide empty state
        if (favorites.isEmpty()) {
            binding.emptyFavesInfoImg.visibility = View.VISIBLE
            binding.emptyFavesInfoText.visibility = View.VISIBLE
            binding.emptyFavesInfoText.text = getString(R.string.no_results_found_db)
        } else {
            binding.emptyFavesInfoImg.visibility = View.GONE
            binding.emptyFavesInfoText.visibility = View.GONE
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
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(requireContext(), favorite.pubDocumentUrl.toUri())
        }
    }

    // Displays an actions bottom sheet for a selected favorite publication.
    // This bottom sheet provides context-specific actions for a favorite item.
    // Since the item is already marked as a favorite, the "Save" action is hidden
    // and the "Delete" action is always shown.
    private fun showActionsBottomSheetForFavorite(entity: FavoriteEntity) {

        // Favorites ALWAYS show delete
        val sheet = ActionsBottomSheet(
            publications = entity.toPubs(),   // Need a conversion (see below)
            config = ActionsBottomSheet.ActionConfig(
                showSave = false,
                showDelete = true,
                showDownload = true
            )
        ) { action ->
            when (action) {
                is ActionsBottomSheet.Action.Save -> {
                    BrowseViewModel(requireActivity().application).saveFavorite(entity)
                }

                is ActionsBottomSheet.Action.CopyURL -> {
                    Config.save(requireContext(), entity.pubDocumentUrl)
                }

                is ActionsBottomSheet.Action.Share -> {
                    Config.sharePublication(requireContext(), entity.pubDocumentUrl)
                }

                is ActionsBottomSheet.Action.Delete -> {
                    deleteFavorite(requireContext(), entity)
                    fetchFaves()
                }

                is ActionsBottomSheet.Action.Download -> {
                    Config.downloadPublication(
                        requireContext(),
                        entity.pubDocumentUrl,
                        entity.pubNumber,
                        entity.pubTitle
                    )
                }
            }
        }

        sheet.show(childFragmentManager, "ActionsSheet")
    }

    // Displays a sorting and bulk-action bottom sheet for the favorites list.
    // This bottom sheet allows the user to:
    // - Sort favorites alphabetically by title.
    // - Sort favorites numerically by publication number.
    // - Delete all favorites at once.
    private fun showSortActionsBottomSheet() {
        val hasItems = favesAdapter?.itemCount?.let { it > 0 } == true

        SortActionsBottomSheet(
            hasItems = hasItems,
            listener = object : SortActionsBottomSheet.SortActionListener {

                // Allows user to sort saved publications by the title of the pub
                // Example: Airlift and Special Missions Aircraft Maintenance will sort higher
                // than Sustaining Airfield Pavement at Enduring Contingency Locations
                override fun onSortByTitle() {
                    viewLifecycleOwner.lifecycleScope.launch {
                        LibraryPrefs.setSortMode(
                            requireContext(),
                            LibrarySortMode.TITLE
                        )
                    }
                }

                override fun onSortByNumber() {
                    viewLifecycleOwner.lifecycleScope.launch {
                        LibraryPrefs.setSortMode(
                            requireContext(),
                            LibrarySortMode.NUMBER
                        )
                    }
                }

                override fun onDeleteAll() {
                    FavoriteDatabase
                        .getDatabase(requireContext())
                        .favoriteDAO()
                        ?.deleteAll()

                    fetchFaves()

                    Config.showToast(
                        requireContext(),
                        "All favorites deleted",
                        ToastType.INFO,
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error)
                    )
                }
            }
        ).show(childFragmentManager, "SortActionBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
