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

    private var _binding: FragmentBrowseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var searchView: SearchView? = null

    private var browseAdapter : BrowseAdapter? = null
    private val browseViewModel : BrowseViewModel by viewModels()

    //The OnBackPressedDispatcher is a class that allows you
    // to register a OnBackPressedCallback to a LifecycleOwner
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            searchView?.onActionViewCollapsed()
            refreshPubList()
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

    private fun initViewModel() {
        binding.loading.visibility = View.VISIBLE
        browseViewModel.browsePublications.observe(viewLifecycleOwner) { items ->
            if (items.isNullOrEmpty()) {
                binding.noResultsFound.isVisible = true
            } else {
                binding.loading.visibility = View.GONE
                binding.noResultsFound.isVisible = false
                browseAdapter?.getPubs(items)
            }
        }
    }


    private fun initUI() {
        browseAdapter = BrowseAdapter(emptyList(), this, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = browseAdapter
        }
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            // Handle for example visibility of menu items
            override fun onPrepareMenu(menu: Menu) {}

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
                                label("Other Publications")
                                options(Filters.externalOrg.map { it.displayName } as MutableList<String>)
                                changeListener { value ->
                                    Filters.externalOrg[value].let { updateFilter(it.filterValue, it.displayName) }
                                }
                            })

                            with(InputSpinner {
                                label("DAF Publications")
                                options(Filters.organizations.map { it.displayName } as MutableList<String>)
                                changeListener { value ->
                                    Filters.organizations[value].let { updateFilter(it.filterValue, it.displayName) }
                                }
                            })

                            // Input Spinner of Major Commands for cleaner look
                            with(InputSpinner {
                                label("MAJCOM Publications")
                                options(Filters.commands.map { it.displayName } as MutableList<String>)
                                changeListener { value ->
                                    Filters.commands[value].let { updateFilter(it.filterValue, it.displayName) }
                                }
                            })

                            with(InputSpinner{
                                label("Base Level Publications")
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
        browseAdapter?.filter?.filter(org)
        (activity as MainActivity).supportActionBar?.title = title
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

    override fun onMainPubsClickListener(pubs: Pubs) {
        // firebaseAnalytics.logEvent("main_pubs_view"){ param("event_name", pubs.pubTitle!!) }
        try {
            if (isRestrictedDocument(pubs.pubDocumentUrl)) {
                showToast(requireContext(), getString(R.string.pub_restricted), ToastType.ERROR, null)
            } else {
                openPdfDocument(pubs.pubDocumentUrl)
            }
        } catch (_: ActivityNotFoundException) {
            openPdfWithFallback(pubs.pubDocumentUrl)
            //Log.e("BrowseFragment", "onMainPubsClickListener: $e")
            //Log.e("PDF Status", "ERROR LOADING: ${e.message}")
        }
    }

    // Restricted pubs are still listed on e-pubs, but open a generic page
    // describing actual location. This displays a Toast indicating the
    // file is not publicly accessible (This only works for pdf's that have
    // the below in the URL... Example AFH10-2401 is https://static.e-publishing.af.mil/production/1/af_a4/publication/afh10-2401/generic_restricted.pdf
    private fun isRestrictedDocument(url: String?): Boolean {
        return url?.let { RESTRICTED_DOCS.any{ restricted -> restricted in url }} == true
    }

    private fun openPdfDocument(url: String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(url!!.toUri(), "application/pdf")
        }
        startActivity(intent)
    }

    private fun openPdfWithFallback(url: String?) {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(requireContext(), url!!.toUri())
    }

    // Callback to refresh (show all) publications list when user selects back button
    // or navigates back to featured fragment
    private fun refreshPubList() {
        (activity as MainActivity).supportActionBar?.title = resources.getString(R.string.app_home)
        browseAdapter?.filter?.filter("")
    }

    // Update search results based on user input in the search bar
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

    companion object {
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