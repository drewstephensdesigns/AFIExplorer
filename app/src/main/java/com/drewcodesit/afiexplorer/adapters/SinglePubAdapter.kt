package com.drewcodesit.afiexplorer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.SinglePubItemBinding
import com.drewcodesit.afiexplorer.model.FeaturedPubs
import com.drewcodesit.afiexplorer.utils.MainClickListener

class SinglePubAdapter(
    private val ct: Context,
    private val featuredPubs: List<FeaturedPubs>,
    val singlePubClickListener: MainClickListener
) : RecyclerView.Adapter<SinglePubAdapter.SinglePubViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SinglePubAdapter.SinglePubViewHolder {
        val binding =
            SinglePubItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SinglePubViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SinglePubAdapter.SinglePubViewHolder, position: Int) {
        val singlePubs = featuredPubs[position]

        holder.apply {
            singlePubNumber.text = singlePubs.Number
            singlePubTitle.text = singlePubs.Title

            // list of colors stored in the colorResources array
            // TODO("theme the card background based on light/dark mode")
            val colorResources = intArrayOf(
                R.color.bottom_nav_icon,
                R.color.bottom_nav_icon_dark,
                R.color.card_bg_orange
            )

            // When you calculate position % colorResources.size, you're finding the remainder
            // of the division of position by the number of colors in the colorResources array (colorResources.size)
            // essentially "cycles" through the indices of the colorResources array.
            val colorIndex = position % colorResources.size

            // You then use colorIndex to access the corresponding color resource from the colorResources array,
            // and set the background color of the card to that color.
            singlePubCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    ct,
                    colorResources[colorIndex]
                )
            )
        }
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

    inner class SinglePubViewHolder(binding: SinglePubItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.singlePubCard.setOnClickListener {
                singlePubClickListener.onFeaturedPubsClickListener(featuredPubs[bindingAdapterPosition])
            }
        }

        var singlePubCard: CardView = binding.singlePubCard
        var singlePubNumber: TextView = binding.singlePubNumber
        var singlePubTitle: TextView = binding.singlePubTitle
    }
}
