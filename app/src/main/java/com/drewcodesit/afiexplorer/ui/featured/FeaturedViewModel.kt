package com.drewcodesit.afiexplorer.ui.featured

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.R
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

                val service = retrofit.create(ApiService::class.java)
                val response = withContext(Dispatchers.IO){
                    service.getFeaturedPublications()
                }

                val sortedPubsList = response.sortedByDescending { it.pubNumber }

                _featuredItemsList.clear()
                _featuredItemsList.addAll(sortedPubsList)

                _featuredPublications.postValue(_featuredItemsList)
            } catch (e: Exception){
                showErrorToast()
            }
        }
    }

    private fun showErrorToast(){
        Toasty.error(app.applicationContext, R.string.no_internet, Toasty.LENGTH_SHORT, false).show()
    }

    interface ApiService{
        @GET("data.json")
        suspend fun getFeaturedPublications() : List<Pubs>
    }
}