package com.drewcodesit.afiexplorer.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.drewcodesit.afiexplorer.MyApplication
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.model.FeaturedPubs
import com.drewcodesit.afiexplorer.utils.Config
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import es.dmoral.toasty.Toasty


class FeaturedViewModel(
    private val app: Application
) :
    AndroidViewModel(app) {

    private var request: JsonArrayRequest? = null

    private var _featuredList = ArrayList<FeaturedPubs>()

    private val _featuredPublications = MutableLiveData<List<FeaturedPubs>>()

    val featuredPublications: MutableLiveData<List<FeaturedPubs>>
        get() = _featuredPublications

    init {
        fetchFeaturedPubs()
    }
    private fun fetchFeaturedPubs() {
        request = JsonArrayRequest(
            Request.Method.GET,
            Config.FEATURED_PUBS_URL,
            null, { response ->
                val items: List<FeaturedPubs> =
                    Gson().fromJson(response.toString(), object : TypeToken<List<FeaturedPubs>>() {}.type)

                // Sort the list by ID
                val sortedPubsList = items.sortedByDescending { it.PubID }

                _featuredList.clear()

                _featuredList.addAll(sortedPubsList/*.take(3)*/)

                // Notify observers that the data has changed
                _featuredPublications.postValue(_featuredList)
            },
            { error ->
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