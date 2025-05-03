package com.drewcodesit.afiexplorer.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.BrowseItemsViewBinding
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config.downloadPublication
import com.drewcodesit.afiexplorer.utils.Config.save
import com.drewcodesit.afiexplorer.utils.Config.sharePublication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowseAdapter(
    private var pubsList: List<Pubs>,
    private val listener: MainClickListener,
    private val navController: NavController,
    private val browseViewModel: BrowseViewModel
) : ListAdapter<Pubs, BrowseAdapter.BrowseVH>(PubsDiffCallback()), Filterable {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseVH {
        val binding = BrowseItemsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrowseVH(binding)
    }

    override fun getItemId(position: Int): Long { return position.toLong() }
    override fun getItemViewType(position: Int): Int { return position }

    override fun onBindViewHolder(holder: BrowseVH, position: Int) {
        val publication = getItem(position)
        holder.bind(publication)
    }

    fun getPubs(newList: List<Pubs>) { submitList(newList) }

    private fun showPopupMenu(view: View, publications: Pubs, fEntity: FavoriteEntity) {
        val wrapper = ContextThemeWrapper(view.context, R.style.Theme_AFIExplorer)
        val popup = PopupMenu(wrapper, view)
        popup.inflate(R.menu.popup_main)

        popup.setOnMenuItemClickListener { item ->
            CoroutineScope(Dispatchers.IO).launch {
                when (item.itemId) {
                    R.id.mSave -> { browseViewModel.saveFavorite(fEntity) }
                    R.id.mCopyURL -> save(view.context, publications.pubDocumentUrl!!)
                    R.id.mShare -> sharePublication(view.context, publications.pubDocumentUrl!!)
                    R.id.mDownload -> downloadPublication(view.context, publications.pubDocumentUrl!!, publications.pubNumber!!, publications.pubTitle!!)
                }
            }
            false
        }
        popup.show()
    }

    // This method is called automatically by the system whenever the user enters text
    // updates the contents of the list or grid to display the filtered items
    // This method is called automatically by the system whenever the user enters text
    // updates the contents of the list or grid to display the filtered items
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                val filteredList = if (charString.isEmpty()) pubsList else pubsList.filter {
                    it.pubTitle!!.contains(charString, ignoreCase = true) ||
                            it.pubNumber!!.contains(charString, ignoreCase = true) ||
                            it.pubRescindOrg!!.contains(charString, ignoreCase = true)
                }
                return FilterResults().apply { values = filteredList }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val searchResults = (results?.values as? List<*>)?.filterIsInstance<Pubs>() ?: emptyList()
                results?.count = searchResults.size
                submitList(searchResults)
            }
        }
    }

    inner class BrowseVH(private val binding: BrowseItemsViewBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(pubs: Pubs){
            with(binding){
                pubNumber.text = pubs.pubNumber
                pubTitle.text = pubs.pubTitle
                pubCertDate.text = navController.context.getString(R.string.certified_date_placeholder, pubs.getCertDate())
                pubGMDate.text = pubs.pubGMDate?.let {
                    navController.context.getString(R.string.gm_date_placeholder, pubs.getGMDate())
                } ?: navController.context.getString(R.string.gm_placeholder)

                pubRescindOrg.text = navController.context.getString(R.string.rescind_placeholder, pubs.pubRescindOrg)

                // Brevity and uniformity for publication changes
                val actionText = when (pubs.pubLastAction) {
                    "GM" -> navController.context.getString(R.string.guidance_memorandum)
                    "AC" -> navController.context.getString(R.string.ac)
                    "IC" -> navController.context.getString(R.string.interim_change)
                    "UpdateContact" -> navController.context.getString(R.string.update_contact)
                    "Rewrite" -> navController.context.getString(R.string.rewrite)
                    "Transfer" -> navController.context.getString(R.string.transfer)
                    "Correction" -> navController.context.getString(R.string.correction)
                    "CertifiedCurrent" -> navController.context.getString(R.string.certified_current)
                    else -> navController.context.getString(R.string.unknown_action)
                }
                pubLastAction.text = actionText
                textViewOptions.setOnClickListener { showPopupMenu(it, pubs, FavoriteEntity(pubs.pubID, pubs.pubNumber!!, pubs.pubTitle!!, pubs.pubDocumentUrl!!)) }
                itemView.setOnClickListener { listener.onMainPubsClickListener(pubs) }
            }
        }
    }

    // compares the old and new Pubs objects and returns
    // true if they are the same. We use this class to
    // optimize the updates in the RecyclerView.
    class PubsDiffCallback : DiffUtil.ItemCallback<Pubs>() {
        override fun areItemsTheSame(oldItem: Pubs, newItem: Pubs): Boolean { return oldItem.pubDocumentUrl == newItem.pubDocumentUrl && oldItem.pubTitle == newItem.pubTitle && oldItem.pubNumber == newItem.pubNumber && oldItem.pubID == newItem.pubID }
        override fun areContentsTheSame(oldItem: Pubs, newItem: Pubs): Boolean { return oldItem == newItem }
    }

    // notify the parent class when a main item in the RecyclerView is clicked.
    // This allows the parent class to respond to the click event and perform some action,
    // such as opening a detail view for the selected item.
    interface MainClickListener {fun onMainPubsClickListener(pubs: Pubs)}
}