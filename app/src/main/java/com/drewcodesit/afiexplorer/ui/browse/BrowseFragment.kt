/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

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
import com.drewcodesit.afiexplorer.utils.filter.Filters
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config.deleteFavorite
import com.drewcodesit.afiexplorer.utils.Config.downloadPublication
import com.drewcodesit.afiexplorer.utils.Config.save
import com.drewcodesit.afiexplorer.utils.Config.sharePublication
import com.drewcodesit.afiexplorer.utils.Config.showToast
import com.drewcodesit.afiexplorer.utils.filter.ActionsBottomSheet
import com.drewcodesit.afiexplorer.utils.filter.SearchBarManager
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.input.type.spinner.InputSpinner
import kotlinx.coroutines.launch

class BrowseFragment : Fragment(),
    BrowseAdapter.MainClickListener,
    BrowseAdapter.MoreActionsListener {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    private lateinit var browseAdapter: BrowseAdapter
    private val browseViewModel: BrowseViewModel by viewModels()

    private lateinit var searchBarManager: SearchBarManager

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val hasActiveFilter = !browseViewModel.currentQuery.value.isNullOrEmpty()

            if (searchBarManager.isExpanded() || hasActiveFilter) {
                // Reset both the UI text box and the ViewModel state
                binding.searchEditText.text.clear()
                browseViewModel.setSearchQuery("")

                if (searchBarManager.isExpanded()) {
                    searchBarManager.collapse()
                }
            } else {
                // No filters active and search bar is closed -> let system handle exit
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
        initViewModel()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )

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
        searchBarManager = SearchBarManager(
            fab = binding.fabSearch,
            searchRow = binding.searchBarContainer,
            searchEditText = binding.searchEditText,
            cancel = binding.btnCollapseSearch,
            bottomNav = bottomNav
        )

        // Simply push the string updates directly to the ViewModel's state pipeline
        binding.searchEditText.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            browseViewModel.setSearchQuery(query)

            // Re-enable the callback whenever a query exists so the next back press captures it
            onBackPressedCallback.isEnabled = query.isNotEmpty() || searchBarManager.isExpanded()
            binding.noResultsFoundText.text = getString(R.string.no_results_found, query)

            if (query.isEmpty()) {
                binding.recyclerView.scrollToPosition(0)
            }
        }
        binding.btnFilter.setOnClickListener { openFilterSheet() }
        browseViewModel.errorEvent.observe(viewLifecycleOwner){errorMessage ->
            // check for error message
            if(errorMessage != null){
                // show error message
                showToast(requireContext(), errorMessage, ToastType.ERROR, null)

                // reset error
                browseViewModel.resetErrorEvent()
            }

        }
    }

    private fun initUI() {
        browseAdapter = BrowseAdapter(this, this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = browseAdapter
        }
    }

    private fun initViewModel() {
        binding.loading.isVisible = true
        binding.noResultsFound.isVisible = false
        binding.noResultsFoundText.isVisible = false

        // Observe query changes to dynamically toggle back press behavior
        browseViewModel.currentQuery.observe(viewLifecycleOwner) { query ->
            onBackPressedCallback.isEnabled = query.isNotEmpty() || searchBarManager.isExpanded()
        }

        browseViewModel.browsePublications.observe(viewLifecycleOwner) { items ->
            // Use the ViewModel's query state instead of the EditText field to judge loading/searching states
            val isSearching = !browseViewModel.currentQuery.value.isNullOrEmpty()

            if (items.isNotEmpty() || isSearching) {
                binding.loading.isVisible = false
            }

            val isEmpty = items.isNullOrEmpty()

            binding.noResultsFound.isVisible = isEmpty && !binding.loading.isVisible
            binding.noResultsFoundText.isVisible = isEmpty && isSearching && !binding.loading.isVisible

            browseAdapter.submitList(items) {
                // Works uniformly whether cleared from backspace or filter resets
                if (isSearching || browseViewModel.currentQuery.value.isNullOrEmpty()) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun openFilterSheet() {
        searchBarManager.collapse()

        InputSheet().show(requireContext()) {
            title("Filter Publications")
            with(InputRadioButtons {
                label("Other Publications")
                options(Filters.externalOrg.map { it.displayName }.toMutableList())
                changeListener { index ->
                    browseViewModel.setSearchQuery(Filters.externalOrg[index].filterValue)
                }
            })

            with(InputSpinner {
                label("DAF Publications")
                options(Filters.organizations.map { it.displayName }.toMutableList())
                changeListener { index ->
                    browseViewModel.setSearchQuery(Filters.organizations[index].filterValue)
                }
            })

            with(InputSpinner {
                label("MAJCOM Publications")
                options(Filters.commands.map { it.displayName }.toMutableList())
                changeListener { index ->
                    browseViewModel.setSearchQuery(Filters.commands[index].filterValue)
                }
            })

            with(InputSpinner {
                label("Base Level Publications")
                options(Filters.bases.map { it.displayName }.toMutableList())
                changeListener { index ->
                    browseViewModel.setSearchQuery(Filters.bases[index].filterValue)
                }
            })
        }
    }

    override fun onMoreActionsClickListener(pubs: Pubs, fEntity: FavoriteEntity) {
        // Fixed: Wrapped the IO suspension method within a lifecycle scope safe context
        viewLifecycleOwner.lifecycleScope.launch {
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
                        save(requireContext(), pubs.pubDocumentUrl.orEmpty())

                    is ActionsBottomSheet.Action.Share ->
                        sharePublication(requireContext(), pubs.pubDocumentUrl.orEmpty())

                    is ActionsBottomSheet.Action.Download ->
                        downloadPublication(
                            requireContext(),
                            pubs.pubDocumentUrl.orEmpty(),
                            pubs.pubNumber.orEmpty(),
                            pubs.pubTitle.orEmpty()
                        )

                    is ActionsBottomSheet.Action.Delete ->
                        deleteFavorite(requireContext(), fEntity)
                }
            }.show(childFragmentManager, "ActionsSheet")
        }
    }

    override fun onMainPubsClickListener(pubs: Pubs) {
        if (isRestrictedDocument(pubs.pubDocumentUrl)) {
            showToast(requireContext(), getString(R.string.pub_restricted), ToastType.ERROR, null)
        } else {
            try {
                openPdfDocument(pubs.pubDocumentUrl)
                getAnalytics().logEvent("pub_clicked") {
                    param("pub_number", pubs.pubNumber.orEmpty())
                    param("pub_title", pubs.pubTitle.orEmpty())
                }
            } catch (_: ActivityNotFoundException) {
                openPdfWithFallback(pubs.pubDocumentUrl)
            }
        }
    }

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

    private fun getAnalytics(): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val RESTRICTED_DOCS = listOf(
            "generic_restricted.pdf", "restricted_access.pdf", "for_official_use_only.pdf",
            "generic_fouo.pdf", "stocked_and_issued", "generic_opr1.pdf", "generic_opr.pdf"
        )
    }
}