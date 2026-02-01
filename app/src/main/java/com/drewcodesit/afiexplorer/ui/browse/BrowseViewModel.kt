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
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class BrowseViewModel(
    private val app: Application) : AndroidViewModel(app) {

    private val allPublications = mutableListOf<Pubs>()
    private val _browsePublications = MutableLiveData<List<Pubs>>()
    val browsePublications: LiveData<List<Pubs>> = _browsePublications

    private val _saveResult = MutableLiveData<String?>()
    val saveResult: LiveData<String?> = _saveResult

    private lateinit var database: FavoriteDatabase


    init {
        initializeDatabase()
        fetchPublications()
    }

    // Initialize the database here
    private fun initializeDatabase() { database = FavoriteDatabase.getDatabase(app) }

    private fun fetchPublications() {
        viewModelScope.launch {
            try {
                val gson = GsonBuilder().setLenient().create()

                val retrofit = Retrofit.Builder()
                    .baseUrl(Config.BASE_URL) // ends with /v2/
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()

                val service = retrofit.create(ApiService::class.java)

                // ONE call. ONE endpoint.
                val response = withContext(Dispatchers.IO) {
                    service.getPubs()
                }

                val sortedList = response.publications
                    .sortedByDescending { it.getCertDate() }

                allPublications.clear()
                allPublications.addAll(sortedList)

                _browsePublications.value = allPublications

            } catch (e: Exception) {
                Config.showToast(
                    app.applicationContext,
                    "Error encountered: $e",
                    ToastType.ERROR,
                    null
                )
                Log.e("BROWSE_VIEW_MODEL", "Error", e)
            }
        }
    }

    fun saveFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = database.favoriteDAO()?.titleExists(favorite.pubNumber)
            if (exists == 0) {
                database.favoriteDAO()?.addData(favorite)
                _saveResult.postValue("${favorite.pubNumber} saved!")
            } else {
                database.favoriteDAO()?.update(favorite)
                _saveResult.postValue("${favorite.pubNumber} updated!")
            }
        }
    }

    // Check if publication is in favorites
    fun isFavorite(pubId: Int): Boolean {
        return database.favoriteDAO()?.exists(pubId) == 1
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }
}