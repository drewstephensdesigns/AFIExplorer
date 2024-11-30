package com.drewcodesit.afiexplorer.ui.browse

import android.app.Application
import android.util.Log
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


class BrowseViewModel(
    private val app: Application) : AndroidViewModel(app) {

    private var _browseItemsList = ArrayList<Pubs>()
    private val _browsePublications = MutableLiveData<List<Pubs>>()

    val browsePublications : MutableLiveData<List<Pubs>>
        get() = _browsePublications

    init {
        fetchPublications()
    }

    private fun fetchPublications() {
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

                // Define all endpoint paths
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
                    "supplemental"
                )

                // Fetch publications concurrently
                val results = withContext(Dispatchers.IO) {
                    endpoints.map { async { service.getPubs(it) } }.awaitAll()
                }

                // Combine and process the results
                val combinedPublications = results.flatMap { it.publications }

                // Sort the publications by cert date
                val sortedList = combinedPublications.sortedByDescending { it.getCertDate() }

                // Update the LiveData
                _browseItemsList.clear()
                _browseItemsList.addAll(sortedList)
                _browsePublications.postValue(_browseItemsList)

            } catch (e: Exception) {
                showErrorToast(e)
                Log.e("BROWSE VIEW MODEL", "Error is : $e")
            }
        }
    }

    private fun showErrorToast(error: Exception){
        Toasty.error(app.applicationContext, "Error: $error", Toasty.LENGTH_SHORT, false).show()
    }
}