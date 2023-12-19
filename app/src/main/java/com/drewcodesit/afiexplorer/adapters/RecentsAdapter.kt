package com.drewcodesit.afiexplorer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.RecentsListItemBinding
import com.drewcodesit.afiexplorer.model.Pubs

class RecentsAdapter(
    private var ct: Context,
    private val recentUpdatedPubs: List<Pubs>,
    val recentsClickListener: RecentUpdatedClickListener
) : RecyclerView.Adapter<RecentsAdapter.RecentUpdateViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentsAdapter.RecentUpdateViewHolder {
        val binding =
            RecentsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentUpdateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentsAdapter.RecentUpdateViewHolder, position: Int) {
        val recentItems = recentUpdatedPubs[position]

        holder.apply {
            pubNumber.text = recentItems.Number
            pubTitle.text = recentItems.Title

            val certifiedDateString: String =
                ct.getString(R.string.certified_date_placeholder, recentItems.getCertDate())

            pubCertDate.text = certifiedDateString

            // list of colors stored in the colorResources array
            val colorResources = intArrayOf(
                R.color.card_bg_orange,
                R.color.bottom_nav_icon,
                R.color.bottom_nav_icon_dark,
                R.color.teal_700
            )

            // When you calculate position % colorResources.size, you're finding the remainder
            // of the division of position by the number of colors in the colorResources array (colorResources.size)
            // essentially "cycles" through the indices of the colorResources array.
            val colorIndex = position % colorResources.size
            recentCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    ct,
                    colorResources[colorIndex]
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return recentUpdatedPubs.size
    }

    inner class RecentUpdateViewHolder(
        binding: RecentsListItemBinding
    ) : RecyclerView.ViewHolder(binding.root){
        init {
            binding.recentsPubCard.setOnClickListener {
                recentsClickListener.onRecentUpdatedClickListener(recentUpdatedPubs[bindingAdapterPosition])
            }
        }

        var recentCardView: CardView = binding.recentsPubCard
        var pubNumber: TextView = binding.pubNumber
        var pubTitle: TextView = binding.pubTitle
        var pubCertDate: TextView = binding.pubCertDate
    }

    interface RecentUpdatedClickListener {
        fun onRecentUpdatedClickListener(pubs: Pubs)
    }
}
