/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.ui.browse

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.BrowseItemsViewBinding
import com.drewcodesit.afiexplorer.models.Pubs

class BrowseAdapter(
    private val listener: MainClickListener,
    private val actionsListener: MoreActionsListener
) : ListAdapter<Pubs, BrowseAdapter.BrowseVH>(PubsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseVH {
        val binding = BrowseItemsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrowseVH(binding)
    }

    override fun onBindViewHolder(holder: BrowseVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BrowseVH(private val binding: BrowseItemsViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pubs: Pubs) {
            with(binding) {
                pubNumber.text = pubs.pubNumber.orEmpty()
                pubTitle.text = pubs.pubTitle.orEmpty()
                pubCertDate.text = itemView.context.getString(R.string.certified_date_placeholder, pubs.getCertDate())

                categoryIndicator.setBackgroundColor(
                    getCategoryColor(root.context, pubs.pubNumber.orEmpty())
                )

                optionsContainer.setOnClickListener {
                    actionsListener.onMoreActionsClickListener(
                        pubs,
                        FavoriteEntity(
                            pubs.pubID,
                            pubs.pubNumber.orEmpty(),
                            pubs.pubTitle.orEmpty(),
                            pubs.pubDocumentUrl.orEmpty(),
                        )
                    )
                }
                itemView.setOnClickListener { listener.onMainPubsClickListener(pubs) }
            }
        }

        private fun getCategoryColor(context: Context, pubNumber: String): Int {
            val colorRes = CATEGORY_COLORS.entries
                .firstOrNull { pubNumber.startsWith(it.key) }
                ?.value ?: R.color.cat_other
            return ContextCompat.getColor(context, colorRes)
        }
    }

    class PubsDiffCallback : DiffUtil.ItemCallback<Pubs>() {
        override fun areItemsTheSame(oldItem: Pubs, newItem: Pubs): Boolean {
            return oldItem.pubID == newItem.pubID
        }
        override fun areContentsTheSame(oldItem: Pubs, newItem: Pubs): Boolean {
            return oldItem == newItem
        }
    }

    interface MainClickListener { fun onMainPubsClickListener(pubs: Pubs) }
    interface MoreActionsListener { fun onMoreActionsClickListener(pubs: Pubs, fEntity: FavoriteEntity) }

    companion object {
        private val CATEGORY_COLORS = mapOf(
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
            "AFSOC"             to R.color.cat_afsoc,
            "AFGSC"             to R.color.cat_afgsc,
            "AFRC"              to R.color.cat_afrc,
            "AETC"              to R.color.cat_aetc,
            "PACAF"             to R.color.cat_pacaf,
            "USAFE-AFAFRICA"    to R.color.cat_usafe,
            "AMC"               to R.color.cat_amc,
            "ACC"               to R.color.cat_acc,
            "ANG"               to R.color.cat_ang,
        )
    }
}