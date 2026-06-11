package com.drewcodesit.afiexplorer.ui.browse

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.interfaces.ApiService
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.objects.PublicationEndpoints
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ViewModel for the Browse screen, responsible for fetching publications
// from multiple endpoints and managing favorites.
class BrowseViewModel(private val app: Application) : AndroidViewModel(app) {

    // Main list of publications exposed to the UI
    private val _browsePublications = MutableLiveData<List<Pubs>>()
    val browsePublications: LiveData<List<Pubs>> = _browsePublications

    // Result message for favorite operations (save/update)
    private val _saveResult = MutableLiveData<String?>()
    val saveResult: LiveData<String?> = _saveResult

    // Error messages exposed to the UI to avoid showing Toasts directly from the ViewModel
    private val _errorEvent = MutableLiveData<String?>()
    val errorEvent: LiveData<String?> = _errorEvent

    // Lazy initialization of the database to avoid recreating it multiple times
    private val database: FavoriteDatabase by lazy { FavoriteDatabase.getDatabase(app) }

    // Lazy initialization of the ApiService to avoid recreating it multiple times
    private val apiService: ApiService by lazy {
        val gson = GsonBuilder().create()
        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    init {
        fetchPublications()
    }

    // Fetches publications from all defined endpoints concurrently.
    // Uses coroutines to handle network calls off the main thread.
    fun fetchPublications() {
        viewModelScope.launch {
            try {
                // Perform network requests concurrently using async/awaitAll
                val results = withContext(Dispatchers.IO) {
                    PublicationEndpoints.ALL_PATHS.map { path ->
                        async {
                            try {
                                apiService.getPubs(path).publications
                            } catch (e: Exception) {
                                Log.e("BrowseViewModel", "Failed to fetch path: $path", e)
                                emptyList() // Gracefully skip failed endpoints
                            }
                        }
                    }.awaitAll()
                }

                // Flatten results and sort by certification date descending
                val sortedList = results.flatten()
                    .sortedByDescending { it.certDateMillis() }

                _browsePublications.postValue(sortedList)

            } catch (e: Exception) {
                Log.e("BrowseViewModel", "Global fetch error", e)
                _errorEvent.postValue("Failed to load publications. Please check your connection.")
            }
        }
    }

    // Saves or updates a publication in the favorites database.
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

    // Checks if a publication is already marked as a favorite.
    // Note: This performs a synchronous DB check.
    fun isFavorite(pubId: Int): Boolean {
        return database.favoriteDAO()?.exists(pubId) == 1
    }

    // Resets the save result after it has been handled by the UI.
    fun resetSaveResult() {
        _saveResult.value = null
    }

    // Resets the error event after it has been handled by the UI.
    fun resetErrorEvent() {
        _errorEvent.value = null
    }
}
