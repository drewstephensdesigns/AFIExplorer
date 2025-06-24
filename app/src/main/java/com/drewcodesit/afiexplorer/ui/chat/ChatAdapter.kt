package com.drewcodesit.afiexplorer.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.databinding.ItemChatGeminiBinding
import com.drewcodesit.afiexplorer.databinding.ItemChatUserBinding
import com.drewcodesit.afiexplorer.models.ChatMessage

// Adapter for displaying Gemini AI and User Response in a single layout
class ChatAdapter(private val messages: MutableList<ChatMessage>) :
RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    // View type constants for user and Gemini messages
    private val USER_MESSAGE = 0
    private val GEMINI_MESSAGE = 1

    // Creates the appropriate ViewHolder based on the view type (user or Gemini)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType){
            USER_MESSAGE -> UserMessageViewHolder(ItemChatUserBinding.inflate(inflater, parent, false))
            GEMINI_MESSAGE -> GeminiMessageViewHolder(ItemChatGeminiBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    // Binds the message data to the corresponding ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder){
            is UserMessageViewHolder ->holder.bind(message)
            is GeminiMessageViewHolder -> holder.bind(message)
        }
    }

    // Returns the total number of messages in the list
    override fun getItemCount(): Int {
        return messages.size
    }

    // Determines the type of view (user or Gemini) based on the message
    override fun getItemViewType(position: Int): Int {
        return if(messages[position].isUser) USER_MESSAGE else GEMINI_MESSAGE
    }

    // ViewHolder for user messages
    inner class UserMessageViewHolder(private val binding: ItemChatUserBinding):
        RecyclerView.ViewHolder(binding.root){

            // Binds user message text to view
            fun bind(message: ChatMessage){
            binding.messageText.text = message.text
        }
    }

    // ViewHolder for Gemini (AI) messages
    inner class GeminiMessageViewHolder(private val binding: ItemChatGeminiBinding) :
        RecyclerView.ViewHolder(binding.root){

            // Binds Gemini message text to view
            fun bind(message: ChatMessage){
            binding.messageText.text = message.text
        }
    }

    // Adds a new message to the list and notifies the adapter to update the view
    fun addMessage(message: ChatMessage){
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}