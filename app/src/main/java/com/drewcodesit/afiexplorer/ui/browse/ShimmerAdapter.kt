package com.drewcodesit.afiexplorer.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R

class ShimmerAdapter(private val itemCount: Int = 5) : RecyclerView.Adapter<ShimmerAdapter.ShimmerViewHolder>() {

    class ShimmerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.browse_item_skeleton, parent, false)
        return ShimmerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShimmerViewHolder, position: Int) {
        // No binding, since it's just a skeleton
    }

    override fun getItemCount(): Int = itemCount
}