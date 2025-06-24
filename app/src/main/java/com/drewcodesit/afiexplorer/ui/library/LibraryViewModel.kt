package com.drewcodesit.afiexplorer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDAO
import com.drewcodesit.afiexplorer.database.favorites.FavoriteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class LibraryViewModel(private val favoriteDAO: FavoriteDAO) : ViewModel() {

    private val _sortOption = MutableStateFlow(0) // 0 = Title, 1 = Number
    val sortOption: StateFlow<Int> get() = _sortOption

    val favorites: StateFlow<List<FavoriteEntity>> = favoriteDAO.getFavoriteData()
        .combine(_sortOption) { list, sort ->
            when (sort) {
                0 -> list.sortedBy { it.pubTitle }
                1 -> list.sortedBy { it.pubNumber }
                else -> list
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSortOption(option: Int) {
        _sortOption.value = option
    }

    fun deleteFavorite(entity: FavoriteEntity) {
        viewModelScope.launch {
            favoriteDAO.delete(entity)
        }
    }

    fun deleteAllFavorites() {
        viewModelScope.launch {
            favoriteDAO.deleteAll()
        }
    }
}