package com.drewcodesit.afiexplorer.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.drewcodesit.afiexplorer.MyApplication
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import es.dmoral.toasty.Toasty

class PubsViewModel(
    private val app: Application,
) : AndroidViewModel(app) {
    private var request: JsonArrayRequest? = null


    private var _pubsList = ArrayList<Pubs>()
    private val _publications = MutableLiveData<List<Pubs>>()
    val publications: MutableLiveData<List<Pubs>>
        get() = _publications

    init {
        fetchPubs()
    }

    // Volley
    private fun fetchPubs() {
        request = JsonArrayRequest(
            Request.Method.GET,
            Config.BASE_URL,
            null,{ response ->
                val items: List<Pubs> =
                    Gson().fromJson(response.toString(), object : TypeToken<List<Pubs>>() {}.type)

                // Sort the list by getCertDate() in descending order
                val sortedPubsList = items.sortedByDescending { it.getCertDate() }

                // Update the _pubsList data with the sorted list
                _pubsList.clear()

                _pubsList.addAll(sortedPubsList)

                // Notify observers that the data has changed
                _publications.postValue(_pubsList)
            },
            {error ->
                println(error.printStackTrace())
                showErrorToast()
            }
        )
        MyApplication.instance.addToRequestQueue(request!!)
    }

    private fun showErrorToast() {
        Toasty.error(app.applicationContext, R.string.no_internet, Toast.LENGTH_SHORT, false).show()
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel any pending requests when the ViewModel is destroyed
        request?.cancel()
    }
}