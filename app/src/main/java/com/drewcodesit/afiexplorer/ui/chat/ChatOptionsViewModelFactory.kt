package com.drewcodesit.afiexplorer.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChatOptionsViewModelFactory(
    private val repository: ChatOptionsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatOptionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatOptionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}