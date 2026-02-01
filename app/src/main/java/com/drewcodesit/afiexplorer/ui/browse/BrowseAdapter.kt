package com.drewcodesit.afiexplorer.ui.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.BrowseItemsViewBinding
import com.drewcodesit.afiexplorer.models.Pubs

class BrowseAdapter(
    var pubsList: List<Pubs>,
    private val listener: MainClickListener,
    private val navController: NavController,
    private val actionsListener: MoreActionsListener
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

    fun filterByRescindOrg() : Filter{
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                val filteredList = if (charString.isEmpty()) pubsList else pubsList.filter {
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
        fun bind(pubs: Pubs) {
            with(binding) {
                pubNumber.text = pubs.pubNumber
                pubTitle.text = pubs.pubTitle
                pubCertDate.text = navController.context.getString(
                    R.string.certified_date_placeholder,
                    pubs.getCertDate()
                )

                // displays the bottom sheet actions for each publication
                optionsContainer.setOnClickListener {
                    actionsListener.onMoreActionsClickListener(
                        pubs,
                        FavoriteEntity(
                            pubs.pubID,
                            pubs.pubNumber!!,
                            pubs.pubTitle!!,
                            pubs.pubDocumentUrl!!,
                        )
                    )
                }
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
    interface MoreActionsListener{ fun onMoreActionsClickListener(pubs: Pubs, fEntity: FavoriteEntity)}
}