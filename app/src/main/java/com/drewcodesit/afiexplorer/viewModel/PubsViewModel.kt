package com.drewcodesit.afiexplorer.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

class PubsViewModel(
    private val app: Application,
) : AndroidViewModel(app) {

    private var _pubsList = ArrayList<Pubs>()
    private val _publications = MutableLiveData<List<Pubs>>()
    val publications: MutableLiveData<List<Pubs>>
        get() = _publications

    init {
        fetchPubs()
    }

    private fun fetchPubs() {
        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(Config.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(ApiService::class.java)
                val response = withContext(Dispatchers.IO) {
                    service.getPubs()
                }

                val sortedPubsList = response.sortedByDescending { it.getCertDate() }

                _pubsList.clear()
                _pubsList.addAll(sortedPubsList)

                _publications.postValue(_pubsList)
            } catch (e: Exception) {
                showErrorToast()
            }
        }
    }

    private fun showErrorToast() {
        Toasty.error(app.applicationContext, R.string.no_internet, Toast.LENGTH_SHORT, false).show()
    }

    interface ApiService {
        @GET("/") // Replace "endpoint" with your actual endpoint
        suspend fun getPubs(): List<Pubs> // Replace Pubs with your data model
    }
}