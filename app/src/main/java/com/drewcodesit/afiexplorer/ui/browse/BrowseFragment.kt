package com.drewcodesit.afiexplorer.ui.browse

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.drewcodesit.afiexplorer.utils.objects.SearchBarManager
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.input.type.spinner.InputSpinner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowseFragment : Fragment(),
    BrowseAdapter.MainClickListener,
    BrowseAdapter.MoreActionsListener {

    // ViewBinding
    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    // Core Components
    private lateinit var browseAdapter: BrowseAdapter
    private val browseViewModel: BrowseViewModel by viewModels()

    // State
    private var isDataLoaded = false

    private lateinit var searchBarManager: SearchBarManager
    private var searchJob: Job? = null

    // Back Press Handling
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (searchBarManager.isExpanded()) {
                //binding.searchEditText.setText("")
                binding.searchEditText.text.clear()
                searchBarManager.collapse()          // then collapse
                // refreshPubList() no longer needed; the setText above already resets the filter
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    // Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBrowseBinding.inflate(inflater, container, false)

        initUI()
        initViewModel()

        // Register back press handler
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )

        // Observe save result messages
        browseViewModel.saveResult.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            showToast(requireContext(), message, ToastType.SUCCESS, null)
            browseViewModel.resetSaveResult()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        // Initialize search UI manager
        searchBarManager = SearchBarManager(
            fab = binding.fabSearch,
            searchRow = binding.searchBarContainer,
            searchEditText = binding.searchEditText,
            cancel = binding.btnCollapseSearch,
            bottomNav = bottomNav
        )

        // Handle search input (debounced externally if needed)
        binding.searchEditText.doAfterTextChanged { text ->
            applySearch(text?.toString())
            onBackPressedCallback.isEnabled = !text.isNullOrEmpty()
        }

        // Open filter bottom sheet
        binding.btnFilter.setOnClickListener { openFilterSheet() }
    }

    // UI Setup
    private fun initUI() {
        browseAdapter = BrowseAdapter(emptyList(), this, this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = browseAdapter
        }
    }

    // ViewModel Setup
    private fun initViewModel() {
        binding.loading.isVisible = true

        browseViewModel.browsePublications.observe(viewLifecycleOwner) { items ->
            renderList(items)
        }

        // Restore cached data when returning to fragment
        browseViewModel.browsePublications.value?.let {
            renderList(it)
        }
    }

    // Rendering Logic
    private fun renderList(items: List<Pubs>?) {
        val isEmpty = items.isNullOrEmpty()

        isDataLoaded = true
        binding.loading.isVisible = false
        binding.noResultsFound.isVisible = isEmpty

        if (!isEmpty) {
            browseAdapter.getPubs(items)
        }
    }

    // Search + Filter
    private fun applySearch(query: String?) {
        if (!isDataLoaded) return

        searchJob?.cancel()

        if (query.isNullOrEmpty()) {
            browseAdapter.resetList()
            binding.noResultsFound.isVisible = false
            binding.noResultsFoundText.isVisible = false
        } else {
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                updateSearchResults(query)
            }
        }
    }

    private fun updateSearchResults(newText: String?) {
        browseAdapter.filter.filter(newText) { count ->
            val showEmpty = count == 0 && !newText.isNullOrEmpty()

            binding.noResultsFound.isVisible = showEmpty
            binding.noResultsFoundText.isVisible = showEmpty
            binding.noResultsFoundText.text =
                getString(R.string.no_results_found, newText)
        }
    }

    private fun updateFilter(org: String) {
        browseAdapter.filter.filter(org)
    }

    private fun refreshPubList() {
        browseAdapter.filter.filter("")
    }

    // Filter Bottom Sheet
    private fun openFilterSheet() {
        searchBarManager.collapse()

        InputSheet().show(requireContext()) {
            title("Filter Publications")

            with(InputRadioButtons {
                label("Other Publications")
                options(Filters.externalOrg.map { it.displayName }.toMutableList())
                changeListener { index ->
                    updateFilter(Filters.externalOrg[index].filterValue)
                }
            })

            with(InputSpinner {
                label("DAF Publications")
                options(Filters.organizations.map { it.displayName }.toMutableList())
                changeListener { index ->
                    updateFilter(Filters.organizations[index].filterValue)
                }
            })

            with(InputSpinner {
                label("MAJCOM Publications")
                options(Filters.commands.map { it.displayName }.toMutableList())
                changeListener { index ->
                    updateFilter(Filters.commands[index].filterValue)
                }
            })

            with(InputSpinner {
                label("Base Level Publications")
                options(Filters.bases.map { it.displayName }.toMutableList())
                changeListener { index ->
                    updateFilter(Filters.bases[index].filterValue)
                }
            })
        }
    }

    // Adapter Callbacks
    override fun onMoreActionsClickListener(pubs: Pubs, fEntity: FavoriteEntity) {
        val isInFavorites = browseViewModel.isFavorite(pubs.pubID)

        ActionsBottomSheet(
            pubs,
            config = ActionsBottomSheet.ActionConfig(
                showSave = !isInFavorites,
                showDelete = isInFavorites,
                showDownload = true
            )
        ) { action ->
            when (action) {
                is ActionsBottomSheet.Action.Save ->
                    browseViewModel.saveFavorite(fEntity)

                is ActionsBottomSheet.Action.CopyURL ->
                    save(requireContext(), pubs.pubDocumentUrl!!)

                is ActionsBottomSheet.Action.Share ->
                    sharePublication(requireContext(), pubs.pubDocumentUrl!!)

                is ActionsBottomSheet.Action.Download ->
                    downloadPublication(
                        requireContext(),
                        pubs.pubDocumentUrl!!,
                        pubs.pubNumber!!,
                        pubs.pubTitle!!
                    )

                is ActionsBottomSheet.Action.Delete ->
                    deleteFavorite(requireContext(), fEntity)
            }
        }.show(childFragmentManager, "ActionsSheet")
    }

    override fun onMainPubsClickListener(pubs: Pubs) {
        if (isRestrictedDocument(pubs.pubDocumentUrl)) {
            showToast(
                requireContext(),
                getString(R.string.pub_restricted),
                ToastType.ERROR,
                null
            )
        } else {
            try {
                openPdfDocument(pubs.pubDocumentUrl)
                getAnalytics().logEvent("pub_clicked") {
                    param("pub_number", pubs.pubNumber!!)
                    param("pub_title", pubs.pubTitle!!)
                }
            } catch (_: ActivityNotFoundException) {
                openPdfWithFallback(pubs.pubDocumentUrl)
            }
        }
    }

    // PDF Handling
    private fun isRestrictedDocument(url: String?): Boolean {
        return url?.let { u -> RESTRICTED_DOCS.any { it in u } } == true
    }

    private fun openPdfDocument(url: String?) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(url!!.toUri(), "application/pdf")
        })
    }

    private fun openPdfWithFallback(url: String?) {
        CustomTabsIntent.Builder()
            .build()
            .launchUrl(requireContext(), url!!.toUri())
    }

    // Analytics
    private fun getAnalytics(): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(requireContext())
    }

    // Cleanup
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Constants
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