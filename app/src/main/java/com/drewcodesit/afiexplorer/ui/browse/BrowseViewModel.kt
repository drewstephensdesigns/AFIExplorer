package com.drewcodesit.afiexplorer.ui.browse

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

class BrowseViewModel(
    private val app: Application) : AndroidViewModel(app) {

    private var _browseItemsList = ArrayList<Pubs>()
    private val _browsePublications = MutableLiveData<List<Pubs>>()

    val browsePublications : MutableLiveData<List<Pubs>>
        get() = _browsePublications

    init {
        fetchPublications()
    }

    private fun fetchPublications(){
        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(Config.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(ApiService::class.java)
                val response = withContext(Dispatchers.IO){
                    service.getPubs()
                }

                val sortedList = response.sortedByDescending { it.getCertDate() }

                _browseItemsList.clear()
                _browseItemsList.addAll(sortedList)

                _browsePublications.postValue(_browseItemsList)
            } catch (e: Exception){
                showErrorToast()
            }
        }
    }

    private fun showErrorToast(){
        Toasty.error(app.applicationContext, R.string.no_internet, Toasty.LENGTH_SHORT, false).show()
    }

    interface ApiService{
        @GET("/") // Replace "endpoint" with your actual endpoint
        suspend fun getPubs(): List<Pubs> // Replace Pubs with your data model
    }
}