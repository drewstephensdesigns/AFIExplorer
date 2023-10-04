package com.drewcodesit.afiexplorer.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.drewcodesit.afiexplorer.MyApplication
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.model.FeaturedPubs
import com.drewcodesit.afiexplorer.utils.Config
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import es.dmoral.toasty.Toasty

class FeaturedPubsViewModel(private val app: Application) : AndroidViewModel(app) {

    private var request: JsonArrayRequest? = null
    private val _featuredPubsList = mutableListOf<FeaturedPubs>()
    private val _featuredPubs = MutableLiveData<List<FeaturedPubs>>()
    val featuredPubs: MutableLiveData<List<FeaturedPubs>> get() = _featuredPubs
    private var featuredAPI: List<FeaturedPubs>? = null

    init {
        fetchFeaturedPubs()
    }

    private fun mergeAndSortLists(featuredList: List<FeaturedPubs>): List<FeaturedPubs> {
        val mergedList = mutableListOf<FeaturedPubs>()
        mergedList.addAll(featuredList)

        return mergedList.sortedByDescending {
            it.PubID
        }
    }

    // creates a network request using Volley to fetch a JSON response from a specified URL,
    // handles the response by parsing it into a list of FeaturedPubs, sets the retry policy
    // for the request, and adds the request to the Volley request queue for execution.
    private fun fetchFeaturedPubs(){
        val featuredRequest = JsonArrayRequest(
            Request.Method.GET,
            Config.FEATURED_PUBS_URL, // Replace with your first source URL
            null, { response ->
                val items: List<FeaturedPubs> =
                    Gson().fromJson(
                        response.toString(),
                        object : TypeToken<List<FeaturedPubs>>() {}.type
                    )
                // process the list of FeaturedPubs items obtained from a successful network response
                handleResponse(items)
            },
            { error ->
                error.showErrorMessage()
            }
        ).apply {
            retryPolicy = DefaultRetryPolicy(
                MY_CUSTOM_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        }

        MyApplication.instance.addToRequestQueue(featuredRequest)
    }

    // Simplifies error handling
    private fun VolleyError.showErrorMessage() {
        println(this.printStackTrace())
        showErrorToast()
    }

    // processes the network response by sorting and merging the received FeaturedPubs items,
    // updates the ViewModel's internal list with the sorted data, and notifies any observers
    // (such as UI components) of the updated data
    private fun handleResponse(items: List<FeaturedPubs>){

        featuredAPI = items
        val sortedList = mergeAndSortLists(
            featuredAPI ?: emptyList()
        )

        _featuredPubsList.clear()
        _featuredPubsList.addAll(sortedList)
        _featuredPubs.postValue(_featuredPubsList)
    }

    private fun showErrorToast() {
        Toasty.error(app.applicationContext, R.string.no_internet, Toast.LENGTH_SHORT, false).show()
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel any pending requests when the ViewModel is destroyed
        request?.cancel()
    }

    companion object {
        private const val MY_CUSTOM_TIMEOUT_MS = 10000
    }
}