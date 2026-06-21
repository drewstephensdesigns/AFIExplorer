/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.repository.PublicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel for the Browse screen, responsible for fetching publications
// from multiple endpoints and managing favorites.
class BrowseViewModel(private val app: Application) : AndroidViewModel(app) {

    // Network & DB Dependencies isolated
    private val database: FavoriteDatabase by lazy { FavoriteDatabase.getDatabase(app) }

    private val repository = PublicationRepository()

    // Source of truth for raw data
    private val _allPublications = MutableLiveData<List<Pubs>>(emptyList())

    // Search query backing field
    private val _searchQuery = MutableLiveData("")
    val currentQuery: LiveData<String> get() = _searchQuery

    // MediatorLiveData dynamically combines raw list + search query (Replaces Filterable)
    val browsePublications = MediatorLiveData<List<Pubs>>().apply {
        addSource(_allPublications) { value = filterAndSort(it, _searchQuery.value) }
        addSource(_searchQuery) { value = filterAndSort(_allPublications.value, it) }
    }

    private val _saveResult = MutableLiveData<String?>()
    val saveResult: LiveData<String?> = _saveResult

    private val _errorEvent = MutableLiveData<String?>()
    val errorEvent: LiveData<String?> = _errorEvent

    init {
        fetchPublications()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun fetchPublications() {
        viewModelScope.launch {
            // Ask the repository for data instead of making network calls manually
            val cleanList = repository.fetchAllPublications()

            if (cleanList.isNotEmpty()) {
                val sortedList = cleanList.sortedByDescending { it.certDateMillis() }
                _allPublications.value = sortedList
            } else {
                _errorEvent.value = "Failed to load publications. Please check your connection."
            }
        }
    }

    // Business Logic moved from Adapter to ViewModel
    private fun filterAndSort(list: List<Pubs>?, query: String?): List<Pubs> {
        val cleanList = list.orEmpty()
        val cleanQuery = query?.trim().orEmpty()

        if (cleanQuery.isEmpty()) return cleanList

        return cleanList.filter { pub ->
            pub.pubTitle?.contains(cleanQuery, ignoreCase = true) == true ||
                    pub.pubNumber?.contains(cleanQuery, ignoreCase = true) == true ||
                    pub.pubRescindOrg?.contains(cleanQuery, ignoreCase = true) == true
        }.sortedWith(
            compareBy<Pubs> { pub ->
                val pubNumber = pub.pubNumber.orEmpty()
                when {
                    !pubNumber.contains("_") && pubNumber.startsWith("DAFI", true) -> 0
                    !pubNumber.contains("_") -> 1
                    else -> 2
                }
            }.thenBy { it.pubNumber }
        )
    }

    fun saveFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = database.favoriteDAO() ?: return@launch
            val exists = dao.titleExists(favorite.pubNumber)

            if (exists == 0) {
                dao.addData(favorite)
                _saveResult.postValue("${favorite.pubNumber} saved!")
            } else {
                dao.update(favorite)
                _saveResult.postValue("${favorite.pubNumber} updated!")
            }
        }
    }

    // Fixed: Now wraps database execution in a non-blocking coroutine safety net if called safely,
    // or optimized via LiveData/Flow if observed from UI.
    suspend fun isFavorite(pubId: Int): Boolean = withContext(Dispatchers.IO) {
        return@withContext database.favoriteDAO()?.exists(pubId) == 1
    }

    fun resetSaveResult() { _saveResult.value = null }
    fun resetErrorEvent() { _errorEvent.value = null }
}