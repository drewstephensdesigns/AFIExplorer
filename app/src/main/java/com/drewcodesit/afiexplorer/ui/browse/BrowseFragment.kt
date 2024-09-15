package com.drewcodesit.afiexplorer.ui.browse

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.MainActivity
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.FragmentBrowseBinding
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.LineDividerItemDecoration
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.input.type.spinner.InputSpinner
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty

class BrowseFragment : Fragment(), BrowseAdapter.MainClickListener {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var _binding: FragmentBrowseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var searchView: SearchView? = null

    private var browseAdapter : BrowseAdapter? = null
    private var browseViewModel : BrowseViewModel? = null

    private var isFirstBackPress: Boolean = true

    //The OnBackPressedDispatcher is a class that allows you
    // to register a OnBackPressedCallback to a LifecycleOwner
    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun handleOnBackPressed() {
                refreshPubList()
                //findNavController().navigate(R.id.navigation_featured)
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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        return binding.root
    }

    private fun initViewModel(){
        binding.loading.visibility = View.VISIBLE

        browseViewModel = ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))[BrowseViewModel::class.java]

        browseViewModel?.browsePublications?.observe(viewLifecycleOwner){browseItems ->
            browseItems.let {
                browseAdapter = BrowseAdapter(requireContext(), it, this)
                binding.loading.visibility = View.GONE
                binding.recyclerView.adapter = browseAdapter
            }
            when {
                browseItems.isEmpty() ->{
                    binding.noResultsFound.visibility = View.VISIBLE
                }

                browseItems != null -> {
                    browseAdapter?.getPubs(browseItems)
                }
            }
        }
    }

    private fun initUI(){
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)

        binding.recyclerView.apply {
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
                            browseAdapter?.filter?.filter(newText) { i ->
                                if (i == 0) {
                                    _binding?.recyclerView?.visibility = View.GONE

                                    // Hiding filter to prevent weird bug


                                    // Displays the lottie animation
                                    _binding?.noResultsFound?.visibility = View.VISIBLE
                                    _binding?.noResultsFoundText?.visibility = View.VISIBLE
                                    _binding?.noResultsFoundText?.text =
                                        resources.getString(R.string.no_results_found, newText)
                                } else {
                                    val mainActivity = activity as? MainActivity
                                    mainActivity?.supportActionBar?.title = resources.getString(R.string.app_home)

                                    // Hides the lottie animation
                                    _binding?.noResultsFound?.visibility = View.GONE
                                    _binding?.noResultsFoundText?.visibility = View.GONE

                                    // Displays the layout
                                    _binding?.recyclerView?.visibility = View.VISIBLE

                                }
                            }

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
                                        "DoD",
                                        "HAF",
                                        "LeMay Center"
                                    )
                                )

                                val orgs = mapOf(
                                    0 to "DoD",
                                    1 to "HAF",
                                    2 to "LeMay Center"
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
                                        "ACC",
                                        "AMC",
                                        "AETC",
                                        "PACAF",
                                        "USAFE-AFAFRICA",
                                        "AFGSC",
                                        "AFMC",
                                        "AFRC",
                                        "AFSOC",
                                        "ANG"
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
                                    9 to "ANG"
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
        browseAdapter?.filterByRescindOrg()?.filter(org)
        (activity as MainActivity).supportActionBar?.title = title
    }

    override fun onMainPubsClickListener(pubs: Pubs) {
        //firebaseAnalytics.logEvent("main_pubs_view"){ param("event_name", pubs.pubTitle!!) }
        try {
            if (pubs.pubDocumentUrl?.contains("generic_restricted.pdf") == true
                || (pubs.pubDocumentUrl?.contains("restricted_access.pdf")) == true
                || (pubs.pubDocumentUrl?.contains("for_official_use_only.pdf")) == true
                || (pubs.pubDocumentUrl?.contains("generic_fouo.pdf")) == true
                || (pubs.pubDocumentUrl?.contains("stocked_and_issued")) == true
                || (pubs.pubDocumentUrl?.contains("generic_opr1.pdf")) == true
                || (pubs.pubDocumentUrl?.contains("generic_opr.pdf")) == true
            ) {
                showRestrictedToast(getString(R.string.pub_restricted))

            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(pubs.pubDocumentUrl), "application/pdf")
                startActivity(intent)
            }

            // Launches PDFViewer Library if no reader is installed
            // Source: https://github.com/afreakyelf/Pdf-Viewer
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: in-case of JAVA
                    requireContext(),
                    "${pubs.pubDocumentUrl}",     // PDF URL in String format
                    "${pubs.pubTitle}",          // PDF Name/Title in String format
                    "",                  // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true             // This param is true by default.
                )
            )
        }
    }

    // Callback to refresh (show all) publications list when user selects back button
    // or navigates back to featured fragment
    private fun refreshPubList() {
        (activity as MainActivity).supportActionBar?.title = resources.getString(R.string.app_home)

        if (isFirstBackPress) {
            isFirstBackPress = false
            browseAdapter?.filterByRescindOrg()?.filter("")
            Toasty.info(requireContext(), resources.getString(R.string.action_navigate_back), Toasty.LENGTH_SHORT, false).show()

        } else {
            findNavController().navigateUp()
        }
    }

    // Cleans up toast notification for restricted access publications
    private fun showRestrictedToast(message: String) {
        Toasty.error(requireContext(), message, Toasty.LENGTH_SHORT, false).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}