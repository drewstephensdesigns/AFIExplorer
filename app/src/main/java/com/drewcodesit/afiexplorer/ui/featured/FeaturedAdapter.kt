package com.drewcodesit.afiexplorer.ui.featured

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.FeaturedItemsViewBinding
import com.drewcodesit.afiexplorer.models.Pubs

class FeaturedAdapter(
    private val ct: Context,
    val featuredCardClickListener : FeaturedCardClickListener

) : RecyclerView.Adapter<FeaturedAdapter.FeaturedItemsVH>() {

    private var featuredPublications: List<Pubs> = emptyList()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedItemsVH {
        val binding =
            FeaturedItemsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedItemsVH(binding)
    }

    // determine the total number of items that the RecyclerView should display
    override fun getItemCount(): Int {
        return featuredPublications.size
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

    override fun onBindViewHolder(holder: FeaturedItemsVH, position: Int) {
        val featuredItems = featuredPublications[position]

        holder.apply {
            featuredCardNumber.text = featuredItems.pubNumber
            featuredCardTitle.text = featuredItems.pubTitle

            // list of colors for the featured items
            val colorResources = intArrayOf(
                R.color.sea_green,
                R.color.air_force_blue,
                R.color.lion,
                R.color.air_force_dark_blue,
                R.color.burnt_sienna
            )

            val colorIndex = position % colorResources.size
            featuredCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    ct, colorResources[colorIndex]
                )
            )
        }
    }

    fun setupPubs(featured: List<Pubs>) {
        featuredPublications = featured
        notifyDataSetChanged()
    }


    inner class FeaturedItemsVH(binding: FeaturedItemsViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // init for card clicks
        init {
            binding.singlePubCard.setOnClickListener {
                featuredCardClickListener.onFeaturedCardClickListener(featuredPublications[bindingAdapterPosition])
            }
        }

        var featuredCard: CardView = binding.singlePubCard
        var featuredCardNumber: TextView = binding.singlePubNumber
        var featuredCardTitle: TextView = binding.singlePubTitle
    }

    interface FeaturedCardClickListener{
        fun onFeaturedCardClickListener(featured : Pubs)
    }
}