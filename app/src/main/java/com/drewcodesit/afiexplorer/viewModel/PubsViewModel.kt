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
    private val app: Application
) : AndroidViewModel(app) {
    private var request: JsonArrayRequest? = null

    private var _pubsList = ArrayList<Pubs>()

    private val _publications = MutableLiveData<List<Pubs>>()

    val publications: MutableLiveData<List<Pubs>>
        get() = _publications

    private var fetchedListsCount = 0
    private var baseAPI: List<Pubs>? = null
    private var angAPI: List<Pubs>? = null

    init {
        fetchPubs()
    }

    // handle the merging and sorting of data:
    private fun mergeAndSortLists(baseList: List<Pubs>, angList: List<Pubs>): List<Pubs> {
        val mergedList = mutableListOf<Pubs>()
        mergedList.addAll(baseList)
        mergedList.addAll(angList)
        return mergedList.sortedByDescending {
            it.getCertDate()
        }
    }

    // Request 1 = AFI Explorer API
    // Request 2 = Hand-Jammed ANG Pubs from GitHub
    private fun fetchPubs() {
        val request1 = JsonArrayRequest(
            Request.Method.GET,
            Config.BASE_URL, // Replace with your first source URL
            null, { response ->
                val items: List<Pubs> =
                    Gson().fromJson(response.toString(), object : TypeToken<List<Pubs>>() {}.type)
                handleResponse(items)
            },
            { error ->
                println(error.printStackTrace())
                showErrorToast()
            }
        )

        val request2 = JsonArrayRequest(
            Request.Method.GET,
            Config.ANG_URL, // Replace with your second source URL
            null, { response ->
                val items: List<Pubs> =
                    Gson().fromJson(response.toString(), object : TypeToken<List<Pubs>>() {}.type)
                handleResponse(items)
            },
            { error ->
                println(error.printStackTrace())
                showErrorToast()
            }
        )

        MyApplication.instance.addToRequestQueue(request1)
        MyApplication.instance.addToRequestQueue(request2)
    }


    // Helper function to handle the response from both requests:
    private fun handleResponse(items: List<Pubs>) {
        fetchedListsCount++
        if (fetchedListsCount == 1) {
            baseAPI = items
        } else if (fetchedListsCount == 2) {
            angAPI = items
            val sortedList = mergeAndSortLists(
                baseAPI ?: emptyList(),
                angAPI ?: emptyList()
            )

            _pubsList.clear()
            _pubsList.addAll(sortedList)
            _publications.postValue(_pubsList)
        }
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
