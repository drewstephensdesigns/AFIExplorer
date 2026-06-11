package com.drewcodesit.afiexplorer.ui.browse

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
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
    private val actionsListener: MoreActionsListener
) : ListAdapter<Pubs, BrowseAdapter.BrowseVH>(PubsDiffCallback()), Filterable {

    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseVH {
        val binding = BrowseItemsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrowseVH(binding)
    }

    //override fun getItemId(position: Int): Long { return position.toLong() }
    //override fun getItemViewType(position: Int): Int { return position }

    override fun onBindViewHolder(holder: BrowseVH, position: Int) {
        val publication = getItem(position)
        holder.bind(publication)
    }

    fun getPubs(newList: List<Pubs>) {
       // Log.d("BrowseDebug", "getPubs — isAttachedToWindow: ${recyclerView?.isAttachedToWindow}, isComputingLayout: ${recyclerView?.isComputingLayout}")
       // Log.d("BrowseDebug", "getPubs called — newList size: ${newList.size}, current internal size: ${currentList.size}")
        pubsList = newList.toList()
        submitList(pubsList)
    }

    // bypass the diff entirely by resetting to the full list immediately.
    fun resetList() {
        submitList(pubsList.toList()) {
            recyclerView?.scrollToPosition(0)
        }
    }

    private fun getCategoryColor(context: Context, pubNumber: String): Int {
        val colorRes = CATEGORY_COLORS.entries
            .firstOrNull { pubNumber.startsWith(it.key) }
            ?.value ?: R.color.cat_other
        return ContextCompat.getColor(context, colorRes)
    }

    companion object {
        private val CATEGORY_COLORS = mapOf(
            // Publication types
            "DAFMAN"            to R.color.cat_dafman,
            "AFMAN"             to R.color.cat_dafman,
            "AFMD"              to R.color.cat_afmd,
            "DAFI"              to R.color.cat_dafi,
            "AFI"               to R.color.cat_dafi,
            "CFETP"             to R.color.cat_cfetp,
            "AFJQS"             to R.color.cat_afjqs,
            "STARCOM"           to R.color.cat_starcom,
            "DOD"               to R.color.cat_dod,
            "SPFI"              to R.color.cat_spfi,
            // Commands — AF prefix (specific first)
            "AFSOC"             to R.color.cat_afsoc,
            "AFGSC"             to R.color.cat_afgsc,
            "AFRC"              to R.color.cat_afrc,
            "AETC"              to R.color.cat_aetc,
            "AFMAN"             to R.color.cat_dafman,
            "AFI"               to R.color.cat_dafi,
            // Commands — other
            "PACAF"             to R.color.cat_pacaf,
            "USAFE-AFAFRICA"    to R.color.cat_usafe,
            "AMC"               to R.color.cat_amc,
            "ACC"               to R.color.cat_acc,
            "ANG"               to R.color.cat_ang,
        )
    }

    // This method is called automatically by the system whenever the user enters text
    // updates the contents of the list or grid to display the filtered items
    // This method is called automatically by the system whenever the user enters text
    // updates the contents of the list or grid to display the filtered items
    override fun getFilter(): Filter {

        return object : Filter() {

            // background thread
            override fun performFiltering(constraint: CharSequence?): FilterResults {

                val query = constraint?.toString()?.trim().orEmpty()

                val filteredList = if (query.isEmpty()) {

                    pubsList

                } else {

                    pubsList.filter { pub ->

                        pub.pubTitle?.contains(query, ignoreCase = true) == true ||
                                pub.pubNumber?.contains(query, ignoreCase = true) == true ||
                                pub.pubRescindOrg?.contains(query, ignoreCase = true) == true
                    }
                        .sortedWith(
                            compareBy<Pubs> {

                                val pubNumber = it.pubNumber.orEmpty()

                                when {

                                    // Parent publication
                                    !pubNumber.contains("_") &&
                                            pubNumber.startsWith("DAFI", true) -> 0

                                    // Base supplements (CANNONAFBI21-101)
                                    !pubNumber.contains("_") -> 1

                                    // Regular supplements
                                    else -> 2
                                }

                            }.thenBy {
                                it.pubNumber
                            }
                        )
                }

                return FilterResults().apply {
                    values = filteredList
                    count = filteredList.size
                }
            }

            // Runs om main/UI thread
            // Updates Recyclerview after filtering has completed
            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {

                val searchResults =
                    (results?.values as? List<*>)?.filterIsInstance<Pubs>()
                        ?: emptyList()

                submitList(searchResults) {

                    recyclerView?.scrollToPosition(0)

                    // Optional:
                    // notify empty state here AFTER submitList finishes
                }
            }
        }
    }

    inner class BrowseVH(private val binding: BrowseItemsViewBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(pubs: Pubs){
            with(binding){
                pubNumber.text = pubs.pubNumber.orEmpty()
                pubTitle.text = pubs.pubTitle.orEmpty()
                pubCertDate.text = itemView.context.getString(R.string.certified_date_placeholder, pubs.getCertDate())

                binding.categoryIndicator.setBackgroundColor(
                    getCategoryColor(root.context, pubs.pubNumber.orEmpty())
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