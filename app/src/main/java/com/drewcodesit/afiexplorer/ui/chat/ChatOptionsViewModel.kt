package com.drewcodesit.afiexplorer.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.drewcodesit.afiexplorer.database.preloaded.AFITopics
import kotlinx.coroutines.flow.Flow

class ChatOptionsViewModel(repo : ChatOptionsRepository) : ViewModel() {
    val topics : Flow<List<AFITopics>> = repo.getAllTopics()
}


