package com.drewcodesit.afiexplorer.ui.options

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.databinding.OptionItemsViewBinding
import com.drewcodesit.afiexplorer.models.OptionItems

class OptionsAdapter(private val items: List<OptionItems>)
    : RecyclerView.Adapter<OptionsAdapter.OptionVH>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OptionVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = OptionItemsViewBinding.inflate(inflater, parent, false)
        return OptionVH(binding)
    }

    override fun onBindViewHolder(holder: OptionVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
    class OptionVH(val binding: OptionItemsViewBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(item: OptionItems){
            with(binding){
                optionTitle.text = item.title
                optionTitle.setCompoundDrawablesWithIntrinsicBounds(
                    item.iconRes,
                    0,
                    0,
                    0
                )

                itemView.setOnClickListener { item.onClick() }
            }
        }
    }
}