package com.drewcodesit.afiexplorer.ui.featured

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.RecentlyUpdatedViewBinding
import com.drewcodesit.afiexplorer.models.Pubs

class RecentlyUpdatedAdapter(
    private var ct: Context,
    val recentCardClickListener : RecentCardClickListener
) : RecyclerView.Adapter<RecentlyUpdatedAdapter.RecentUpdateVH>(){

    private var recentUpdatedPubs : List<Pubs> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentUpdateVH {
        val binding = RecentlyUpdatedViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentUpdateVH(binding)
    }

    override fun onBindViewHolder(holder: RecentUpdateVH, position: Int) {
        val recentItems = recentUpdatedPubs[position]

        holder.apply {
            recentPubNumber.text = recentItems.pubNumber
            recentPubTitle.text = recentItems.pubTitle

            val certifiedDateString : String =
                ct.getString(R.string.certified_date_placeholder, recentItems.getCertDate())

            recentPubCertDate.text = certifiedDateString

            // List of colors stored in array
            val colorResources = intArrayOf(
                R.color.burnt_sienna,
                R.color.air_force_dark_blue,
                R.color.lion,
                R.color.air_force_blue,
                R.color.sea_green
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

    fun setupRecents(recents : List<Pubs>){
        recentUpdatedPubs = recents
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return recentUpdatedPubs.size
    }

    inner class RecentUpdateVH(binding: RecentlyUpdatedViewBinding) : RecyclerView.ViewHolder(binding.root){

        // init for card clicks
        init {
            binding.recentsPubCard.setOnClickListener {
                recentCardClickListener.onRecentCardClickListener(recentUpdatedPubs[bindingAdapterPosition])
            }
        }

        var recentCardView : CardView = binding.recentsPubCard
        var recentPubNumber : TextView = binding.pubNumber
        var recentPubTitle : TextView = binding.pubTitle
        var recentPubCertDate : TextView = binding.pubCertDate
    }

    interface RecentCardClickListener{
        fun onRecentCardClickListener(recents : Pubs)
    }
}
