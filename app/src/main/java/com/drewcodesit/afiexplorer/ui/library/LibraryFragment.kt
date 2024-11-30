package com.drewcodesit.afiexplorer.ui.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FragmentLibraryBinding
import com.drewcodesit.afiexplorer.utils.FavesListenerItem
import com.maxkeppeler.sheets.core.ButtonStyle
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.lottie.LottieAnimation
import com.maxkeppeler.sheets.lottie.withCoverLottieAnimation
import com.rajat.pdfviewer.PdfViewerActivity
import es.dmoral.toasty.Toasty


class LibraryFragment : Fragment(), FavesListenerItem {

    private var _binding: FragmentLibraryBinding? = null

    private var favesAdapter: LibraryAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        initMenu()
        initUI()
        fetchFaves()

        return binding.root
    }

    // Initializes the menu for the by inflating a menu resource and setting up a MenuProvider
    // to handle menu creation, visibility, and item selection.
    private fun initMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_fave_actions, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filter_faves -> {
                        InputSheet().show(requireContext()) {
                            style(SheetStyle.BOTTOM_SHEET)
                            title("Sort By")
                            with(InputRadioButtons {
                                options(mutableListOf("Publication Title", "Publication Number"))
                                changeListener { value ->
                                    when (value) {
                                        0 -> {
                                            favesAdapter?.sortFavorites()
                                        }

                                        1 -> {
                                            favesAdapter?.sortFavoritesByNumber()
                                        }
                                    }
                                }
                            })
                        }
                        true
                    }

                    R.id.action_clear_database -> {
                        nukeDatabase()
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initUI() {
        binding.rvFavorites.layoutManager = LinearLayoutManager(context)
        binding.rvFavorites.setHasFixedSize(true)
        binding.rvFavorites.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(
                com.drewcodesit.afiexplorer.utils.LineDividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL,
                    36
                )
            )
        }
    }

    private fun fetchFaves() {
        val favorites =
            FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()?.getFavoriteData()

        favesAdapter = LibraryAdapter(requireContext(), favorites!!, this, this)
        binding.rvFavorites.adapter = favesAdapter

        // Show or Hide Empty State
        if (favorites.isEmpty()) {
            // Kotlin View Binding
            binding.emptyFavesInfoImg.visibility = View.VISIBLE
            binding.emptyFavesInfoText.visibility = View.VISIBLE
            binding.emptyFavesInfoText.text = getString(R.string.no_results_found_db)

        } else {
            // Kotlin View Binding
            binding.emptyFavesInfoImg.visibility = View.GONE
            binding.emptyFavesInfoText.visibility = View.GONE
        }
    }

    //  Prompts user for confirmation before deleting the app's database using an InfoSheet dialog
    // with informative content and a caution animation.
    private fun nukeDatabase() {
        InfoSheet().show(requireContext()) {
            style(SheetStyle.DIALOG)
            // withIconButton(IconButton( R.drawable.ic_error)){}
            title("Delete Database?")
            content(R.string.action_nuke_database)
            withCoverLottieAnimation(LottieAnimation {
                setupAnimation {
                    setAnimation(R.raw.caution_anim)
                }
            })
            onNegative(
                "Not Yet",
            ) { /* Set listener when negative button is clicked. */ }
            onPositive("Ok") {
                FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()?.deleteAll()
                fetchFaves()
                showDeleteToast("Database cleared!")
            }
            positiveButtonStyle(ButtonStyle.OUTLINED)
            negativeButtonStyle(ButtonStyle.NORMAL)
            displayNegativeButton(true)
            displayPositiveButton(true)
        }
    }

    // Opens document from the Favorites Screen
    // If PDF Reader installed the doc will open natively
    // Else falls back to the PDFViewer Activity
    override fun onFavesSelectedListener(onOpened: FavoriteEntity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(onOpened.pubDocumentUrl), "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                // Use 'launchPdfFromPath' if you want to use assets file (enable "fromAssets" flag) / internal directory
                PdfViewerActivity.launchPdfFromUrl(      //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: incase of JAVA
                    requireContext(),
                    onOpened.pubDocumentUrl,       // PDF URL in String format
                    onOpened.pubNumber,           // PDF Name/Title in String format
                    "",                     // If nothing specific, Put "" it will save to Downloads
                    enableDownload = true                // This param is true by default.
                )
            )
        }
    }

    // Deletes single row from database and refreshes the list
    override fun onFavesDeletedListener(onDeleted: FavoriteEntity, position: Int) {
        FavoriteDatabase.getDatabase(requireContext()).favoriteDAO()?.delete(onDeleted)
        showDeleteToast("You deleted ${onDeleted.pubNumber}")
        favesAdapter?.notifyItemRemoved(position)
        fetchFaves()
    }

    private fun showDeleteToast(message: String) {
        Toasty.info(requireContext(), message, R.drawable.ic_error).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

    }
}
