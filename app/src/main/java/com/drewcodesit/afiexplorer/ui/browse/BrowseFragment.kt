package com.drewcodesit.afiexplorer.ui.browse

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.MainActivity
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FragmentBrowseBinding
import com.drewcodesit.afiexplorer.models.Filters
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config.deleteFavorite
import com.drewcodesit.afiexplorer.utils.Config.downloadPublication
import com.drewcodesit.afiexplorer.utils.Config.save
import com.drewcodesit.afiexplorer.utils.Config.sharePublication
import com.drewcodesit.afiexplorer.utils.Config.showToast
import com.drewcodesit.afiexplorer.utils.objects.ActionsBottomSheet
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.input.type.spinner.InputSpinner

class BrowseFragment : Fragment(),
    BrowseAdapter.MainClickListener,
    BrowseAdapter.MoreActionsListener {

    //private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var _binding: FragmentBrowseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var searchView: SearchView? = null

    private var browseAdapter : BrowseAdapter? = null
    private val browseViewModel : BrowseViewModel by viewModels({requireActivity()})

    //The OnBackPressedDispatcher is a class that allows you
    // to register a OnBackPressedCallback to a LifecycleOwner
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // 1. Check if searchView is active or list is filtered
            val isFiltered = !searchView?.query.isNullOrEmpty() ||
                    (browseAdapter?.currentList?.size != browseAdapter?.pubsList?.size)

            if (isFiltered) {
                // Restore the full list
                refreshPubList()
            } else {
                // Default back behavior
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBrowseBinding.inflate(inflater, container, false)

        initUI()
        setupMenu()
        initViewModel()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
        browseViewModel.saveResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                showToast(requireContext(), it, ToastType.SUCCESS, null)
                browseViewModel.resetSaveResult()
            }
        }
        return binding.root
    }

    private fun initViewModel(){
        binding.loading.isVisible = true
        browseViewModel.browsePublications.observe(viewLifecycleOwner){ items ->
            binding.loading.isVisible = false
            if (items.isNullOrEmpty()){
                binding.noResultsFound.isVisible = true
            } else {
                binding.noResultsFound.isVisible = false
                browseAdapter = BrowseAdapter(items, this, findNavController(), this)
                binding.recyclerView.adapter = browseAdapter
                browseAdapter?.getPubs(items)
            }
        }
    }

    private fun initUI(){
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_search, menu)
                val searchManager =
                    requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
                searchView = menu.findItem(R.id.action_search)?.actionView as SearchView

                searchView?.apply {
                    setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
                    setIconifiedByDefault(true)
                    maxWidth = Int.MAX_VALUE

                    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            searchView?.clearFocus()
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            updateSearchResults(newText)
                            return false
                        }
                    })
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_search -> { true }

                    R.id.action_filter_orgs ->{
                        InputSheet().show(requireContext()) {
                            title("Filter Publications")

                            // Input buttons for non-epubs/MAJCOM publications
                            // DoD: JTR, GTC, DTS regs
                            // AF/: All HAF level publications (SAF, AF, JAG, etc)
                            // LeMay Center: Air Force Doctrine (TTPs are restricted)
                            with(InputRadioButtons {
                                label("Select an Organization")
                                options(Filters.organizations.map { it.displayName } as MutableList<String>)
                                changeListener { value ->
                                    Filters.organizations[value].let { updateFilter(it.filterValue, it.displayName) }
                                }
                            })

                            // Input Spinner of Major Commands for cleaner look
                            with(InputSpinner {
                                label("Select a Command")
                                options(Filters.commands.map { it.displayName } as MutableList<String>)
                                changeListener { value ->
                                    Filters.commands[value].let { updateFilter(it.filterValue, it.displayName) }
                                }
                            })

                            with(InputSpinner{
                                label("Select a base")
                                options(Filters.bases.map { it.displayName } as MutableList<String>)
                                changeListener { value ->
                                    Filters.bases[value].let { updateFilter(it.filterValue, it.displayName) }
                                }
                            })
                        }
                        true
                    }
                    else -> { false }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.CREATED)
    }

    // Change ToolBar Title: (activity as MainActivity).supportActionBar?.title = ""
    // Source: https://stackoverflow.com/questions/27100007/set-a-title-in-toolbar-from-fragment-in-android
    private fun updateFilter(org: String, title: String) {
        browseAdapter?.filterByRescindOrg()?.filter(org)
        (activity as MainActivity).supportActionBar?.title = title
    }

    override fun onMainPubsClickListener(pubs: Pubs) {
       // firebaseAnalytics.logEvent("main_pubs_view"){ param("event_name", pubs.pubTitle!!) }
        try {
            if (isRestrictedDocument(pubs.pubDocumentUrl)) {
                showToast(requireContext(), getString(R.string.pub_restricted), ToastType.ERROR, null)
            } else {
                openPdfDocument(pubs.pubDocumentUrl)
            }
        } catch (e: ActivityNotFoundException) {
            openPdfWithFallback(pubs.pubDocumentUrl)
        }
    }

    // Restricted pubs are still listed on e-pubs, but open a generic page
    // describing actual location. This displays a Toast indicating the
    // file is not publicly accessible (This only works for pdf's that have
    // the below in the URL... Example AFH10-2401 is https://static.e-publishing.af.mil/production/1/af_a4/publication/afh10-2401/generic_restricted.pdf
    private fun isRestrictedDocument(url: String?): Boolean {
        return url?.let { RESTRICTED_DOCS.any{restricted -> restricted in url}} == true
    }

    private fun openPdfDocument(url: String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(url!!.toUri(), "application/pdf")
        }
        startActivity(intent)
    }

    // Migrated to Chrome Custom Tabs as fallback since the pdfViewer
    // doesn't work with targetsdk 36
    // https://developer.android.com/reference/androidx/browser/customtabs/CustomTabsIntent
    private fun openPdfWithFallback(url: String?) {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(requireContext(), url!!.toUri())
    }

    // Callback to refresh (show all) publications list when user selects back button
    // or navigates back to featured fragment
    private fun refreshPubList() {
        (activity as MainActivity).supportActionBar?.title = getString(R.string.app_home)

        // Clear the search view UI without triggering the listener again
        // (or letting the listener handle the filter(null) call)
        searchView?.setQuery("", false)
        searchView?.clearFocus()

        browseAdapter?.filterByRescindOrg()?.filter("/"){
            binding.recyclerView.post {
                binding.recyclerView.postDelayed({
                    binding.recyclerView.layoutManager?.scrollToPosition(0)
                }, 100)
            }
        }
    }

    private fun updateSearchResults(newText: String?) {
        browseAdapter?.filter?.filter(newText) { count ->
            binding.noResultsFound.isVisible = count == 0 && !newText.isNullOrEmpty() // Show "no results" only when there's a query and no matches
            binding.noResultsFoundText.isVisible = count == 0 && !newText.isNullOrEmpty()
            binding.noResultsFoundText.text = getString(R.string.no_results_found, newText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMoreActionsClickListener(pubs: Pubs, fEntity: FavoriteEntity) {
        val isInFavorites = browseViewModel.isFavorite(pubs.pubID)
        val sheet = ActionsBottomSheet(
            pubs,
            config = ActionsBottomSheet.ActionConfig(
                showSave = !isInFavorites,
                showDelete = isInFavorites,
                showDownload = true
            )
        ) { action ->
            when (action) {
                is ActionsBottomSheet.Action.Save -> {
                    browseViewModel.saveFavorite(fEntity)
                }

                is ActionsBottomSheet.Action.CopyURL -> {
                    save(requireContext(), pubs.pubDocumentUrl!!)
                }

                is ActionsBottomSheet.Action.Share -> {
                    sharePublication(requireContext(), pubs.pubDocumentUrl!!)
                }

                is ActionsBottomSheet.Action.Download -> {
                    downloadPublication(
                        requireContext(),
                        pubs.pubDocumentUrl!!,
                        pubs.pubNumber!!,
                        pubs.pubTitle!!
                    )
                }

                is ActionsBottomSheet.Action.Delete -> {
                    deleteFavorite(requireContext(), fEntity)
                }
            }
        }
        sheet.show(childFragmentManager, "ActionsSheet")
    }

    companion object{
        private val RESTRICTED_DOCS = listOf(
            "generic_restricted.pdf",
            "restricted_access.pdf",
            "for_official_use_only.pdf",
            "generic_fouo.pdf",
            "stocked_and_issued",
            "generic_opr1.pdf",
            "generic_opr.pdf"
        )
    }
}