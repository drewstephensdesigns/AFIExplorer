package com.drewcodesit.afiexplorer.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.database.preloaded.AFITopics
import com.drewcodesit.afiexplorer.databinding.ChatOptionsViewBinding

class ChatOptionsAdapter(
    private var topics: List<AFITopics>,
    private val onItemClick: (AFITopics) -> Unit
) : RecyclerView.Adapter<ChatOptionsAdapter.AfiTopicViewHolder>() {

    inner class AfiTopicViewHolder(private val binding: ChatOptionsViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(topic: AFITopics) {
            with(binding){
                titleTextView.text = topic.title
                subtitleTextView.text = topic.subtitle
                pubNumber.text = topic.number
                iconImageView.setImageResource(topic.iconResId)
                binding.root.setOnClickListener { onItemClick(topic) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AfiTopicViewHolder {
        val binding = ChatOptionsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AfiTopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AfiTopicViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount(): Int = topics.size

    fun updateTopics(newTopics: List<AFITopics>) {
        topics = newTopics
        notifyDataSetChanged()
    }
}