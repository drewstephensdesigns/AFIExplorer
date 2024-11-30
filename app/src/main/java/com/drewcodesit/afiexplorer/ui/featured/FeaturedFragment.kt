package com.drewcodesit.afiexplorer.ui.featured

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.MainActivity
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.FragmentFeaturedBinding
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.DotsIndicatorDecoration
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty

class FeaturedFragment : Fragment(),
    FeaturedAdapter.FeaturedCardClickListener,
    RecentlyUpdatedAdapter.RecentCardClickListener {

    private var _binding: FragmentFeaturedBinding? = null
    private val binding get() = _binding!!

    private lateinit var featuredAdapter: FeaturedAdapter
    private lateinit var recentAdapter: RecentlyUpdatedAdapter

    private lateinit var featuredViewModel: FeaturedViewModel
    private lateinit var recentViewModel: RecentlyUpdatedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeaturedBinding.inflate(inflater, container, false)

        initViewModels()
        setupQuickLinks()
        return binding.root
    }

    private fun initViewModels() {
        binding.loading.visibility = View.VISIBLE

        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)

        featuredViewModel = ViewModelProvider(requireActivity(), factory)[FeaturedViewModel::class.java]
        recentViewModel = ViewModelProvider(requireActivity(), factory)[RecentlyUpdatedViewModel::class.java]

        // Observe featured publications
        featuredViewModel.featuredPublications.observe(viewLifecycleOwner) { featuredItems ->
            setupRecyclerView(
                featuredItems,
                "Featured Items Not Available!",
                ::initFeaturedUI,
                ::onFeaturedItemsFetched
            )
        }

        // Observe recently updated publications
        recentViewModel.recentPublications.observe(viewLifecycleOwner) { recentlyUpdatedItems ->
            setupRecyclerView(
                recentlyUpdatedItems,
                "Recent Items Not Fetched",
                ::initRecentUI,
                ::onRecentlyUpdatedItemsFetched
            )
        }
    }

    private fun setupRecyclerView(
        items: List<Pubs>,
        errorMessage: String,
        initUI: () -> Unit,
        onItemsFetched: (List<Pubs>) -> Unit
    ) {
        if (items.isEmpty()) {
            showToast(errorMessage)
        } else {
            initUI()
            onItemsFetched(items)
        }
    }

    private fun onFeaturedItemsFetched(items: List<Pubs>) {
        featuredAdapter = FeaturedAdapter(requireContext(), this)
        binding.singlePubRv.adapter = featuredAdapter
        featuredAdapter.setupPubs(items)
    }

    private fun onRecentlyUpdatedItemsFetched(items: List<Pubs>) {
        recentAdapter = RecentlyUpdatedAdapter(requireContext(), this)
        binding.recentsRv.adapter = recentAdapter
        recentAdapter.setupRecents(items)
        binding.loading.visibility = View.GONE
    }

    private fun initFeaturedUI() {
        setupHorizontalRecyclerView(binding.singlePubRv)
        // enforce a "snap-to-position" effect, particularly useful when you want your
        // RecyclerView to emulate the behavior of a ViewPager or a horizontal pager.
        val pageSnapper = PagerSnapHelper()
        pageSnapper.attachToRecyclerView(binding.singlePubRv)
        setupDotsIndicator(binding.singlePubRv)
    }

    private fun initRecentUI() {
        setupHorizontalRecyclerView(binding.recentsRv)
    }

    private fun setupHorizontalRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun setupDotsIndicator(recyclerView: RecyclerView) {
        val radius = resources.getDimensionPixelSize(R.dimen.dot_radius)
        val dotsHeight = resources.getDimensionPixelSize(R.dimen.dot_height)

        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(com.google.android.material.R.attr.colorControlNormal, typedValue, true)
        val color = ContextCompat.getColor(requireContext(), typedValue.resourceId)

        recyclerView.addItemDecoration(
            DotsIndicatorDecoration(
                radius,
                radius * 4,
                dotsHeight,
                color,
                color
            )
        )
    }

    private fun setupQuickLinks() {
        binding.apply {
            viewAll.setOnClickListener { navigateToBrowse() }
            quickLinkEpub.setOnClickListener { openUrl(resources.getString(R.string.epubs_url)) }
            quickLinkDoctrine.setOnClickListener { openUrl(resources.getString(R.string.af_doctrine_url)) }
            quickLinkResilience.setOnClickListener { openUrl(resources.getString(R.string.resilience_url)) }
            quickLinkFeedback.setOnClickListener { sendFeedback() }
        }
    }

    private fun navigateToBrowse() {
        val navView = (requireActivity() as MainActivity).findViewById<BottomNavigationView>(R.id.nav_view)
        val item = navView.menu.findItem(R.id.navigation_browse)
        NavigationUI.onNavDestinationSelected(item, findNavController())
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun sendFeedback() {
        val versionName = context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName
        val feedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(
                "mailto:" + Uri.encode("drewstephensdesigns@gmail.com") +
                        "?subject=" + Uri.encode("App Feedback: ") +
                        resources.getString(R.string.app_name) + " $versionName"
            )
        }
        startActivity(Intent.createChooser(feedbackIntent, "Send feedback..."))
    }

    private fun showToast(message: String) {
        Toasty.error(requireContext(), message, Toasty.LENGTH_SHORT, false).show()
    }

    override fun onFeaturedCardClickListener(featured: Pubs) {
        openPdf(featured.pubDocumentUrl!!, featured.pubTitle!!)
    }

    override fun onRecentCardClickListener(recents: Pubs) {
        openPdf(recents.pubDocumentUrl!!, recents.pubTitle!!)
    }

    private fun openPdf(url: String, title: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/pdf")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                PdfViewerActivity.launchPdfFromUrl(
                    requireContext(),
                    url,
                    title,
                    "",
                    enableDownload = true
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}