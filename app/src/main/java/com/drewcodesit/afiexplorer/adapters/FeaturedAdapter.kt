package com.drewcodesit.afiexplorer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.FeaturedListItemBinding
import com.drewcodesit.afiexplorer.model.FeaturedPubs

class FeaturedAdapter(
    private val ct: Context,
    private val featuredPubs: List<FeaturedPubs>,
    val featuredClickListener: FeaturedPubsClickListener
) : RecyclerView.Adapter<FeaturedAdapter.FeaturedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = FeaturedListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return FeaturedViewHolder(binding)

    }

    // determine the total number of items that the RecyclerView should display
    override fun getItemCount(): Int {
        return featuredPubs.size
    }

    // provide a unique identifier (usually a long value) for an item at a given position
    // in the RecyclerView. The purpose of this function is to help the RecyclerView track
    // individual items efficiently
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // specify the type of view that should be used at a particular position. In your code,
    // it returns the position itself as the view type
    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        val featured = featuredPubs[position]

        holder.apply {
            singlePubNumber.text = featured.Number
            singlePubTitle.text = featured.Title

            // list of colors stored in the colorResources array
            val colorResources = intArrayOf(
                R.color.teal_700,
                R.color.bottom_nav_icon_dark,
                R.color.bottom_nav_icon,
                R.color.card_bg_orange,

            )

            // When you calculate position % colorResources.size, you're finding the remainder
            // of the division of position by the number of colors in the colorResources array (colorResources.size)
            // essentially "cycles" through the indices of the colorResources array.
            val colorIndex = position % colorResources.size
            singlePubCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    ct,
                    colorResources[colorIndex]
                )
            )
        }
    }

    inner class FeaturedViewHolder(binding: FeaturedListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.singlePubCard.setOnClickListener {
                featuredClickListener.onFeaturedPubsClickListener(featuredPubs[bindingAdapterPosition])
            }
        }

        var singlePubCard: CardView = binding.singlePubCard
        var singlePubNumber: TextView = binding.singlePubNumber
        var singlePubTitle: TextView = binding.singlePubTitle
    }

    interface FeaturedPubsClickListener{
        fun onFeaturedPubsClickListener(featured: FeaturedPubs)
    }
}