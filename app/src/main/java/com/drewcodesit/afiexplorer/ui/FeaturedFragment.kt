package com.drewcodesit.afiexplorer.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.adapters.FeaturedAdapter
import com.drewcodesit.afiexplorer.adapters.FeaturedAdapter.FeaturedPubsClickListener
import com.drewcodesit.afiexplorer.adapters.RecentsAdapter
import com.drewcodesit.afiexplorer.adapters.RecentsAdapter.RecentUpdatedClickListener
import com.drewcodesit.afiexplorer.databinding.FragmentFeaturedBinding
import com.drewcodesit.afiexplorer.model.FeaturedPubs
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.DotsIndicatorDecoration
import com.drewcodesit.afiexplorer.view.MainActivity
import com.drewcodesit.afiexplorer.viewModel.FeaturedViewModel
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty

class FeaturedFragment : Fragment(),
    RecentUpdatedClickListener,
    FeaturedPubsClickListener {

    private var _binding: FragmentFeaturedBinding? = null
    private val binding get() = _binding!!

    private var featuredAdapter: FeaturedAdapter? = null

    private var recentsAdapter: RecentsAdapter? = null

    private var featuredViewModel: FeaturedViewModel? = null

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFeaturedBinding.inflate(inflater, container, false)
        initViewModel()
        initUI()
        initRecentsUI()
        setUpQuickLinks()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initViewModel() {
        binding.loading.visibility = View.VISIBLE

        // Separates business logic from the UI
        featuredViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory
                .getInstance(requireActivity().application)
        )[FeaturedViewModel::class.java]

        featuredViewModel?.featuredPublications?.observe(viewLifecycleOwner) { featuredList ->
            featuredList.let {
                featuredAdapter = FeaturedAdapter(requireContext(), it, this)
                _binding?.singlePubRv?.adapter = featuredAdapter
                featuredAdapter!!.notifyDataSetChanged()

            }
        }

        featuredViewModel?.recentPublications?.observe(viewLifecycleOwner) { fullList ->
            fullList.let {
                recentsAdapter = RecentsAdapter(requireContext(), it, this)
                _binding?.recentsRv?.adapter = recentsAdapter
                recentsAdapter!!.notifyDataSetChanged()
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun initUI() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.singlePubRv.layoutManager = layoutManager
        binding.singlePubRv.itemAnimator = DefaultItemAnimator()

        // enforce a "snap-to-position" effect, particularly useful when you want your
        // RecyclerView to emulate the behavior of a ViewPager or a horizontal pager.
        val pageSnapper = PagerSnapHelper()
        pageSnapper.attachToRecyclerView(binding.singlePubRv)

        // Dot indicator view
        val radius = resources.getDimensionPixelSize(R.dimen.dot_radius)
        val dotsHeight = resources.getDimensionPixelSize(R.dimen.dot_height)

        // Gets light/dark theme for dots indicator
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(com.google.android.material.R.attr.colorControlNormal, typedValue, true)
        val color = ContextCompat.getColor(requireContext(), typedValue.resourceId)

        // Adds page indicator to recyclerview
        binding.singlePubRv.addItemDecoration(
            DotsIndicatorDecoration(
                radius,
                radius * 4,
                dotsHeight,
                color,
                color
            )
        )
    }

    private fun initRecentsUI() {

        // Horizontal List
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recentsRv.layoutManager = layoutManager
        binding.recentsRv.itemAnimator = DefaultItemAnimator()
    }

    private fun setUpQuickLinks(){
        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName

        // E-Pubs Main Index
        binding.quickLinkEpub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = (Uri.parse(resources.getString(R.string.epubs_url)))
            startActivity(intent)
        }

        // Feedback to developer
        binding.quickLinkFeedback.setOnClickListener {
            val send = Intent(Intent.ACTION_SENDTO)

            // Email subject (App Name and Version Code for troubleshooting)
            val uriText = "mailto:" + Uri.encode("drewstephensdesigns@gmail.com") +
                    "?subject=" + Uri.encode("App Feedback: ") + resources.getString(R.string.app_name) + " " + versionName
            val uri = Uri.parse(uriText)
            send.data = uri

            // Displays apps that are able to handle email
            startActivity(Intent.createChooser(send, "Send feedback..."))
        }

       // Navigates user to full publications list
       binding.viewAll.setOnClickListener {
           binding.viewAll.findNavController().navigate(R.id.navigation_home)
       }
    }


    // Launches document natively if application is installed
    // Launches PDFViewer Library is fall back if else
    // Source: https://github.com/afreakyelf/Pdf-Viewer
    override fun onFeaturedPubsClickListener(featured: FeaturedPubs) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(featured.DocumentUrl), "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: in-case of JAVA
                    requireContext(),
                    "${featured.DocumentUrl}",     // PDF URL in String format
                    "${featured.Title}",          // PDF Name/Title in String format
                    "",                  // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true             // This param is true by default.
                )
            )
        }
    }

    override fun onRecentUpdatedClickListener(pubs: Pubs) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(pubs.DocumentUrl), "application/pdf")
            startActivity(intent)
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
       // adapter?.filterByRescindOrg()?.filter("")

        if (exit) {
            requireActivity().finish() // finish activity
        } else {
            Toasty.normal(requireContext(), getString(R.string.action_exit_app)).show()
            exit = true
            Handler(Looper.getMainLooper()).postDelayed({ exit = false }, 1 * 1000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}