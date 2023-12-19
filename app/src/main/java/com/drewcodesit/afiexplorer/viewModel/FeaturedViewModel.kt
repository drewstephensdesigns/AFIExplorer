package com.drewcodesit.afiexplorer.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.model.FeaturedPubs
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


class FeaturedViewModel(
    private val app: Application
) :
    AndroidViewModel(app) {

    // Featured
    private var _featuredList = ArrayList<FeaturedPubs>()
    private val _featuredPublications = MutableLiveData<List<FeaturedPubs>>()
    val featuredPublications: MutableLiveData<List<FeaturedPubs>>
        get() = _featuredPublications

    // Recently Updated
    private var _recentViewList = ArrayList<Pubs>()
    private val _recentPublications = MutableLiveData<List<Pubs>>()
    val recentPublications: MutableLiveData<List<Pubs>>
        get() = _recentPublications


    init {
        fetchPubs()
        fetchRecentPubs()
    }
    private fun fetchPubs() {
        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(Config.FEATURED_PUBS_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(ApiService::class.java)
                val response = withContext(Dispatchers.IO) {
                    service.getPubs()
                }

                val sortedPubsList = response.sortedByDescending { it.Number }

                _featuredList.clear()
                _featuredList.addAll(sortedPubsList)

                _featuredPublications.postValue(_featuredList)

            } catch (e: Exception) {
                showErrorToast()
            }
        }
    }

    private fun fetchRecentPubs() {
        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(Config.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(ApiService2::class.java)
                val response = withContext(Dispatchers.IO) {
                    service.getRecentPubs()
                }

                val sortedPubsList = response.sortedByDescending { it.getCertDate() }

                _recentViewList.clear()
                _recentViewList.addAll(sortedPubsList)

                val firstTen = sortedPubsList.take(10) // Extract the first 10 items
                _recentPublications.postValue(firstTen)

            } catch (e: Exception) {
                showErrorToast()
            }
        }
    }


    private fun showErrorToast() {
        Toasty.error(app.applicationContext, R.string.no_internet, Toast.LENGTH_SHORT, false).show()
    }

    interface ApiService {
        @GET("data.json") // Replace "endpoint" with your actual endpoint
        suspend fun getPubs(): List<FeaturedPubs> // Replace Pubs with your data model
    }

    interface ApiService2 {
        @GET("/")
        suspend fun getRecentPubs(): List<Pubs>
    }
}