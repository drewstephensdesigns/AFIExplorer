package com.drewcodesit.afiexplorer.ui.featured

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
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
    RecentlyUpdatedAdapter.RecentCardClickListener{

    private var _binding: FragmentFeaturedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var featuredAdapter: FeaturedAdapter? = null
    private var recentAdapter: RecentlyUpdatedAdapter? = null

    private var featuredViewModel: FeaturedViewModel? = null
    private var recentViewModel : RecentlyUpdatedViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFeaturedBinding.inflate(inflater, container, false)

        initViewModels()
        setUpQuickLinks()
        return binding.root
    }

    private fun initViewModels(){
        binding.loading.visibility = View.VISIBLE
        featuredViewModel = ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[FeaturedViewModel::class.java]

        recentViewModel = ViewModelProvider(
            requireActivity(), ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[RecentlyUpdatedViewModel::class.java]

        featuredViewModel?.featuredPublications?.observe(viewLifecycleOwner){featuredItems ->
            featuredItems.let {
                featuredAdapter = FeaturedAdapter(requireContext(), this)
                binding.singlePubRv.adapter = featuredAdapter
            }
            when{
                featuredItems.isEmpty() ->{
                    Toasty.error(requireContext(), "Featured Items Not Available!", Toasty.LENGTH_SHORT, false).show()

                }
                featuredItems != null ->{
                    initFeaturedUI()
                    featuredAdapter?.setupPubs(featuredItems)
                }
            }
        }

        recentViewModel?.recentPublications?.observe(viewLifecycleOwner){recentlyUpdatedItems ->
            recentlyUpdatedItems.let {
                recentAdapter = RecentlyUpdatedAdapter(requireContext(), this)
                binding.recentsRv.adapter = recentAdapter
            }
            when{
                recentlyUpdatedItems.isEmpty() ->{
                    Toasty.error(requireContext(), "Recent Items Not Fetched", Toasty.LENGTH_SHORT, false).show()
                }
                recentlyUpdatedItems != null ->{
                    initRecentUI()
                    binding.loading.visibility = View.GONE
                    recentAdapter?.setupRecents(recentlyUpdatedItems)
                }
            }
        }
    }

    private fun initFeaturedUI(){
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

    private fun initRecentUI(){
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recentsRv.layoutManager = layoutManager
        binding.recentsRv.itemAnimator = DefaultItemAnimator()
    }

    private fun setUpQuickLinks() {
        val viewAll: TextView = binding.viewAll
        val epubs: TextView = binding.quickLinkEpub
        val doctrine: TextView = binding.quickLinkDoctrine
        val resilience: TextView = binding.quickLinkResilience
        val feedBack: TextView = binding.quickLinkFeedback

        viewAll.setOnClickListener {
            val item = (requireActivity() as MainActivity).findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_browse)
            NavigationUI.onNavDestinationSelected(item, findNavController())
        }

        val openUrl: (String) -> Unit = { url ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        epubs.setOnClickListener { openUrl(resources.getString(R.string.epubs_url)) }
        doctrine.setOnClickListener { openUrl(resources.getString(R.string.af_doctrine_url)) }
        resilience.setOnClickListener { openUrl(resources.getString(R.string.resilience_url)) }

        feedBack.setOnClickListener {
            val versionName = context?.packageManager?.getPackageInfo(context?.packageName!!, 0)?.versionName
            val send = Intent(Intent.ACTION_SENDTO)
            val uriText = "mailto:" + Uri.encode("drewstephensdesigns@gmail.com") +
                    "?subject=" + Uri.encode("App Feedback: ") + resources.getString(R.string.app_name) + " " + versionName
            val uri = Uri.parse(uriText)
            send.data = uri
            startActivity(Intent.createChooser(send, "Send feedback..."))
        }
    }

    // Launches document natively if application is installed
    // Launches PDFViewer Library is fall back if else
    // Source: https://github.com/afreakyelf/Pdf-Viewer
    override fun onFeaturedCardClickListener(featured: Pubs) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(featured.pubDocumentUrl), "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(          //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: in-case of JAVA
                    requireContext(),
                    "${featured.pubDocumentUrl}",     // PDF URL in String format
                    "${featured.pubTitle}",          // PDF Name/Title in String format
                    "",                         // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true                   // This param is true by default.
                )
            )
        }
    }

    override fun onRecentCardClickListener(recents: Pubs) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(recents.pubDocumentUrl), "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(         //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: in-case of JAVA
                    requireContext(),
                    "${recents.pubDocumentUrl}",     // PDF URL in String format
                    "${recents.pubTitle}",          // PDF Name/Title in String format
                    "",                        // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true                   // This param is true by default.
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}