package com.drewcodesit.afiexplorer.ui.browse

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
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
import com.drewcodesit.afiexplorer.databinding.FragmentBrowseBinding
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config.showToast
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import com.google.firebase.analytics.FirebaseAnalytics
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.input.type.spinner.InputSpinner
import com.rajat.pdfviewer.PdfViewerActivity

class BrowseFragment : Fragment(), BrowseAdapter.MainClickListener {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var _binding: FragmentBrowseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var searchView: SearchView? = null

    private var browseAdapter : BrowseAdapter? = null
    private val browseViewModel : BrowseViewModel by viewModels({requireActivity()})

    //The OnBackPressedDispatcher is a class that allows you
    // to register a OnBackPressedCallback to a LifecycleOwner
    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun handleOnBackPressed() {
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

    private fun initViewModel(){

        browseViewModel.browsePublications.observe(viewLifecycleOwner){ items ->
            if (items.isNullOrEmpty()){
                binding.noResultsFound.isVisible = true
            } else {
                binding.noResultsFound.isVisible = false


                browseAdapter = BrowseAdapter(items, this, findNavController(), browseViewModel)
                binding.recyclerView.adapter = browseAdapter
                browseAdapter?.getPubs(items)
                binding.loading.isVisible = false
                binding.loadingText.isVisible = false
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
                                options(
                                    mutableListOf(
                                        "Department of Defense",
                                        "Headquarters Air Force",
                                        "Headquarters Space Force",
                                        "LeMay Center"
                                    )
                                )

                                val orgs = mapOf(
                                    0 to "DoD",
                                    1 to "HAF",
                                    2 to "USSF",
                                    3 to "LeMay Center"
                                )

                                changeListener { value ->
                                    orgs[value]?.let { updateFilter(it, it) }
                                }
                            })

                            // Input Spinner of Major Commands for cleaner look
                            with(InputSpinner {
                                label("Select a Command from the dropdown")
                                options(
                                    mutableListOf(
                                        "Air Combat Command",
                                        "Air Mobility Command",
                                        "Air Education & Training Command",
                                        "Pacific Air Forces",
                                        "US Air Forces in Europe-AFAFRICA",
                                        "Air Force Global Strike Command",
                                        "Air Force Material Command",
                                        "Air Force Reserve Command",
                                        "Air Force Special Operations Command",
                                        "Air National Guard",
                                        "Space Force/SPoC",
                                        "Space Force/SSC",
                                        "Space Force/STARCOM",
                                        "Space Force/COO",
                                        "Space Force/CSRO"
                                    )
                                )

                                val commands = mapOf(
                                    0 to "ACC",
                                    1 to "AMC",
                                    2 to "AETC",
                                    3 to "PACAF",
                                    4 to "USAFE-AFAFRICA",
                                    5 to "AFGSC",
                                    6 to "AFMC",
                                    7 to "AFRC",
                                    8 to "AFSOC",
                                    9 to "ANG",
                                    10 to "SPoC",
                                    11 to "SSC",
                                    12 to "STARCOM",
                                    13 to "COO",
                                    14 to "CSRO"
                                )
                                changeListener { value ->
                                    commands[value]?.let {
                                        updateFilter(it, it)
                                    }
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
        browseAdapter?.applyFilter(org, BrowseAdapter.FilterMode.ORG)
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
            openPdfWithFallback(pubs.pubDocumentUrl, pubs.pubTitle)
        }
    }

    // Restricted pubs are still listed on e-pubs, but open a generic page
    // describing actual location. This displays a Toast indicating the
    // file is not publicly accessible (This only works for pdf's that have
    // the below in the URL... Example AFH10-2401 is https://static.e-publishing.af.mil/production/1/af_a4/publication/afh10-2401/generic_restricted.pdf
    private fun isRestrictedDocument(url: String?): Boolean {
        return url?.let {
            listOf(
                "generic_restricted.pdf", "restricted_access.pdf", "for_official_use_only.pdf",
                "generic_fouo.pdf", "stocked_and_issued", "generic_opr1.pdf", "generic_opr.pdf"
            ).any { it in url }
        } == true
    }

    private fun openPdfDocument(url: String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(url!!.toUri(), "application/pdf")
        }
        startActivity(intent)
    }

    private fun openPdfWithFallback(url: String?, title: String?) {
        startActivity(
            PdfViewerActivity.launchPdfFromUrl(
                requireContext(),
                url.orEmpty(),
                title.orEmpty(),
                "",  // If nothing specific, leave empty
                enableDownload = true
            )
        )
    }

    // Callback to refresh (show all) publications list when user selects back button
    // or navigates back to featured fragment
    private fun refreshPubList() {
        (activity as MainActivity).supportActionBar?.title = resources.getString(R.string.app_home)
        browseAdapter?.applyFilter("", BrowseAdapter.FilterMode.SEARCH)
        searchView?.setQuery("", false)
        binding.recyclerView.scrollToPosition(0)
    }

    private fun updateSearchResults(newText: String?) {
        browseAdapter?.applyFilter(newText ?: "", BrowseAdapter.FilterMode.SEARCH) { count ->
            binding.noResultsFound.isVisible = count == 0 && !newText.isNullOrEmpty() // Show "no results" only when there's a query and no matches
            binding.noResultsFoundText.isVisible = count == 0 && !newText.isNullOrEmpty()
            binding.noResultsFoundText.text = getString(R.string.no_results_found, newText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}