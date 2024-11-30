package com.drewcodesit.afiexplorer.ui.featured

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.lang.Exception

class FeaturedViewModel(
    private val app: Application
) : AndroidViewModel(app) {
    private var _featuredItemsList = ArrayList<Pubs>()
    private val _featuredPublications = MutableLiveData<List<Pubs>>()

    val featuredPublications: MutableLiveData<List<Pubs>>
        get() = _featuredPublications

    init {
        fetchFeaturedPublications()
    }

    private fun fetchFeaturedPublications(){
        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(Config.FEATURED_PUBS_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(FeaturedApiService::class.java)
                val response = withContext(Dispatchers.IO){
                    service.getFeaturedPublications()
                }

                val sortedPubsList = response.sortedByDescending { it.pubNumber }

                _featuredItemsList.clear()
                _featuredItemsList.addAll(sortedPubsList)

                _featuredPublications.postValue(_featuredItemsList)
            } catch (e: Exception){
                showErrorToast("Error: $e")
                Log.e("Featured View Model", "Error is: $e")
            }
        }
    }

    private fun showErrorToast(message: String){
        Toasty.error(app.applicationContext, message, Toasty.LENGTH_SHORT, false).show()
    }

    interface FeaturedApiService{
        @GET("data.json")
        suspend fun getFeaturedPublications() : List<Pubs>
    }
}