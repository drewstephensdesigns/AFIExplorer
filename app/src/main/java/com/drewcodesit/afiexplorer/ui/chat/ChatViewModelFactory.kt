package com.drewcodesit.afiexplorer.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.drewcodesit.afiexplorer.database.preloaded.VectorDAO

class ChatViewModelFactory(private val vector: VectorDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(vector) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}