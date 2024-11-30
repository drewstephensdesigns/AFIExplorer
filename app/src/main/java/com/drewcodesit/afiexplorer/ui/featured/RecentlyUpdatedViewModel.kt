package com.drewcodesit.afiexplorer.ui.featured

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.interfaces.ApiService
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import com.google.gson.GsonBuilder
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class RecentlyUpdatedViewModel(
    private val app: Application
) : AndroidViewModel(app){

    // Recently Updated
    private var _recentViewList = ArrayList<Pubs>()
    private val _recentPublications = MutableLiveData<List<Pubs>>()
    val recentPublications: MutableLiveData<List<Pubs>>
        get() = _recentPublications

    init {
        fetchRecentPubs()
    }

    private fun fetchRecentPubs() {
        viewModelScope.launch {
            try {
                val gson = GsonBuilder()
                    .setLenient()
                    .create()

                val retrofit = Retrofit.Builder()
                    .baseUrl(Config.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()

                val service = retrofit.create(ApiService::class.java)

                val endpoints = listOf(
                    "air-force/departmental",
                    "air-national-guard/air-national-guard",
                    "major-commands/air-combat-command",
                    "major-commands/air-education-and-training-command",
                    "major-commands/air-force-global-strike-command",
                    "major-commands/air-force-materiel-command",
                    "major-commands/air-force-reserve-command",
                    "major-commands/air-force-special-operations-command",
                    "major-commands/air-mobility-command",
                    "major-commands/pacific-air-force",
                    "major-commands/united-states-air-force-in-europe-af-africa",
                    "united-states-space-force/headquarters",
                    "united-states-space-force/space-operations-command",
                    "united-states-space-force/space-systems-command",
                    "united-states-space-force/space-training-readiness-command",
                    "united-states-space-force/ussf-coo",
                    "united-states-space-force/ussf-csro",
                )

                // Fetch Pubs Concurrently
                val results = withContext(Dispatchers.IO){
                    endpoints.map { async { service.getPubs(it) } }.awaitAll()
                }

                // Combine and process results
                val combinedPubs = results.flatMap { it.publications }

                // Sort publications by date
                val sortedList = combinedPubs.sortedByDescending { it.getCertDate() }


                _recentViewList.clear()
                _recentViewList.addAll(sortedList)

                val firstTen = sortedList.take(10) // Extract the first 10 items
                _recentPublications.postValue(firstTen)

            } catch (e: Exception) {
                showErrorToast(e)
                Log.e("BROWSE VIEW MODEL", "Error is : $e")
            }
        }
    }

    private fun showErrorToast(error: Exception) {
        Toasty.error(app.applicationContext, "Error is: $error", Toast.LENGTH_SHORT, false).show()
    }
}