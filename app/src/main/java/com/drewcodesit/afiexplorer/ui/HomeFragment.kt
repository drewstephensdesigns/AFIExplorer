package com.drewcodesit.afiexplorer.ui

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.adapters.PubsAdapter
import com.drewcodesit.afiexplorer.databinding.FragmentHomeBinding
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.MyDividerItemDecoration
import com.drewcodesit.afiexplorer.view.MainActivity
import com.drewcodesit.afiexplorer.viewModel.PubsViewModel
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.input.type.spinner.InputSpinner
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty


class HomeFragment : Fragment(),
    PubsAdapter.PubsAdapterListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var pubsViewModel: PubsViewModel? = null
    private var adapter: PubsAdapter? = null
    private var searchView: SearchView? = null

    private var exit: Boolean = false

    //The OnBackPressedDispatcher is a class that allows you
    // to register a OnBackPressedCallback to a LifecycleOwner
    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun handleOnBackPressed() {
                closeOrRefreshApp()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Loading Indicator
        binding.loading.visibility = View.VISIBLE

        // Separates business logic from the UI
        pubsViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory
                .getInstance(requireActivity().application)
        )[PubsViewModel::class.java]

        pubsViewModel?.publications?.observe(viewLifecycleOwner) { pubsList ->
            pubsList?.let {
                adapter = PubsAdapter(requireContext(), it,this)
                _binding?.recyclerView?.adapter = adapter
                binding.loading.visibility = View.GONE
            }
        }
        return binding.root
    }

    // An override in a Fragment class that gets called when the associated
    // view has been created. It performs necessary UI initialization
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        setupMenu()
        setupBottomSheet()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    // While the Fragment menu API, which could be used for creating menu items and
    // reacting to the selection of such, is now marked as deprecated, the ComponentActivity
    // replaces these functionalities by implementing the MenuHost interface.
    // Source: https://medium.com/tech-takeaways/how-to-migrate-the-deprecated-oncreateoptionsmenu-b59635d9fe10
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

                        // Source: https://stackoverflow.com/questions/61570173/display-no-results-found
                        override fun onQueryTextChange(newText: String?): Boolean {
                            adapter?.filter?.filter(newText) { i ->
                                if (i == 0) {
                                    _binding?.recyclerView?.visibility = View.GONE
                                    _binding?.noResultsFound?.visibility = View.VISIBLE
                                    _binding?.noResultsFound?.text =
                                        resources.getString(R.string.no_results_found, newText)
                                } else {
                                    (activity as MainActivity).supportActionBar?.title = resources.getString(R.string.app_home)
                                    _binding?.noResultsFound?.visibility = View.GONE
                                    _binding?.recyclerView?.visibility = View.VISIBLE
                                }
                            }
                            return false
                        }
                    })
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Validate and handle the selected menu item
                return when (menuItem.itemId) {
                    R.id.action_search -> {
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.CREATED)
    }

    // Sets up the RecyclerView with a layout manager, fixed size, animation, and a
    // custom divider between items.
    private fun initUI() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.apply {
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

    // Displays bottom sheet for filtering of publications
    // Radio Buttons for non Major Commands
    // Spinner Selection for Major Commands
    private fun setupBottomSheet() {
        binding.fabFilterMain.setOnClickListener {
            InputSheet().show(requireContext()) {
                title("Filter Publications")

                // Input buttons for non-epubs/MAJCOM publications
                // DoD: JTR, GTC, DTS regs
                // AF/: All HAF level publications (SAF, AF, JAG, etc)
                // LeMay Center: Air Force Doctrine (TTPs are restricted)
                // Added 18 JUL: Air National Guard Publications
                with(InputRadioButtons {
                    label("Select an Organization")
                    options(mutableListOf("DoD", "HAF", "LeMay Center", "Air National Guard"))
                    changeListener { value ->
                        when (value) {
                            0 -> {
                                updateFilter("DoD", "DoD")
                            }

                            1 -> {
                                updateFilter("AF/", "HAF")
                            }

                            2 -> {
                                updateFilter("LeMay", "Doctrine")
                            }

                            3 -> {
                                updateFilter("ANG", "ANG")
                            }
                            else ->{
                                (activity as MainActivity).supportActionBar?.title = getString(R.string.app_home)
                            }
                        }
                    }
                })

                // Input Spinner of Major Commands for cleaner look
                with(InputSpinner {
                    label("Select a Command from the dropdown")
                    options(
                        mutableListOf(
                            "ACC", "AMC",
                            "AETC", "PACAF",
                            "USAFE-AFAFRICA",
                            "AFGSC", "AFMC",
                            "AFRC", "AFSOC"
                        )
                    )
                    changeListener { value ->
                        when (value) {
                            0 -> {
                                updateFilter("ACC", "ACC")
                            }

                            1 -> {
                                updateFilter("AMC", "AMC")
                            }

                            2 -> {
                                updateFilter("AETC", "AETC")
                            }

                            3 -> {
                                updateFilter("PACAF", "PACAF")
                            }

                            4 -> {
                                updateFilter("USAFE-AFAFRICA", "USAFE-AFAFRICA")
                            }

                            5 -> {
                                updateFilter("AFGSC", "AFGSC")
                            }

                            6 -> {
                                updateFilter("AFMC", "AFMC")
                            }

                            7 -> {
                                updateFilter("AFRC", "AFRC")
                            }

                            8 -> {
                                updateFilter("AFSOC", "AFSOC")
                            }
                            else -> {
                                (activity as MainActivity).supportActionBar?.title = getString(R.string.app_home)
                            }
                        }
                    }
                })
            }
        }
    }

    // Change ToolBar Title: (activity as MainActivity).supportActionBar?.title = ""
    // Source: https://stackoverflow.com/questions/27100007/set-a-title-in-toolbar-from-fragment-in-android
    private fun updateFilter(org: String, title: String) {
        adapter?.filterByRescindOrg()?.filter(org)
        (activity as MainActivity).supportActionBar?.title = title
    }

    // The function first checks if the DocumentUrl property of the Pubs object contains certain keywords
    // that indicate that the document is restricted. If the DocumentUrl contains any of these keywords,
    // the function shows a Toast message indicating that the document is restricted and cannot be accessed.
    // If the DocumentUrl does not contain any of the restricted keywords, the function creates an Intent to view
    // the PDF document with a PDF viewer application. If an ActivityNotFoundException is caught, it means that there
    // is no PDF viewer application installed on the device, so the function launches a PDF viewer activity
    override fun onMainPubsClickListener(pubs: Pubs) {
        try {
            if (pubs.DocumentUrl?.contains("generic_restricted.pdf") == true
                || (pubs.DocumentUrl?.contains("restricted_access.pdf")) == true
                || (pubs.DocumentUrl?.contains("for_official_use_only.pdf")) == true
                || (pubs.DocumentUrl?.contains("generic_fouo.pdf")) == true
                || (pubs.DocumentUrl?.contains("stocked_and_issued")) == true
                || (pubs.DocumentUrl?.contains("generic_opr1.pdf")) == true
                || (pubs.DocumentUrl?.contains("generic_opr.pdf")) == true
            ) {
                showRestrictedToast(getString(R.string.pub_restricted))

            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(pubs.DocumentUrl), "application/pdf")
                startActivity(intent)
            }

            // Launches PDFViewer Library if no reader is installed
            // Source: https://github.com/afreakyelf/Pdf-Viewer
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: in-case of JAVA
                    requireContext(),
                    "${pubs.DocumentUrl}",     // PDF URL in String format
                    "${pubs.Title}",          // PDF Name/Title in String format
                    "",                  // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true             // This param is true by default.
                )
            )
        }
    }

    // Callback to refresh (show all) publications list when user selects back button
    // or closes app after hitting back twice within 2 seconds
    private fun closeOrRefreshApp() {
        (activity as MainActivity).supportActionBar?.title = resources.getString(R.string.app_home)

        // Filters list back to all pubs view
        adapter?.filterByRescindOrg()?.filter("")

        if (exit) {
            requireActivity().finish() // finish activity
        } else {
            Toasty.normal(requireContext(), getString(R.string.action_exit_app)).show()
            exit = true
            Handler(Looper.getMainLooper()).postDelayed({ exit = false }, 1 * 1000)
        }
    }

    // Cleans up toast notification for restricted access publications
    private fun showRestrictedToast(message: String) {
        Toasty.error(requireContext(), message, Toast.LENGTH_SHORT, false).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
