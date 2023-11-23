package com.drewcodesit.afiexplorer.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.adapters.FeaturedAdapter
import com.drewcodesit.afiexplorer.databinding.FragmentFeaturedBinding
import com.drewcodesit.afiexplorer.model.FeaturedPubs
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.MainClickListener
import com.drewcodesit.afiexplorer.viewModel.FeaturedViewModel
import com.rajat.pdfviewer.PdfViewerActivity

class FeaturedFragment : Fragment(), MainClickListener {

    private var _binding: FragmentFeaturedBinding? = null
    private val binding get() = _binding!!

    private var featuredAdapter: FeaturedAdapter? = null
    private var featuredViewModel: FeaturedViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFeaturedBinding.inflate(inflater, container, false)
        initViewModel()
        initUI()
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
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun initUI() {
        binding.singlePubRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    override fun onMainPubsClickListener(pubs: Pubs) {}

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
