package com.drewcodesit.afiexplorer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDAO

class LibraryViewModelFactory(private val favoriteDAO: FavoriteDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(favoriteDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
