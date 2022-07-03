package com.drewcodesit.afiexplorer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.drewcodesit.afiexplorer.utils.DelegatesExt


/**
 * Created by drewstephens
 *
 * */
class MyApplication : Application() {

    companion object {
        var instance: MyApplication by DelegatesExt.notNullSingleValue()
        val TAG: String? = MyApplication::class.java.simpleName
    }

    private var mRequestQueue: RequestQueue? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        applyTheme()
    }

    /**
     * Applies the App's Theme from sharedPrefs
     */
    fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val modeNight = sharedPreferences.getInt(
                getString(R.string.pref_key_mode_night),
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        AppCompatDelegate.setDefaultNightMode(modeNight)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        req.tag = TAG
        getRequestQueue()?.add(req)
    }

    private fun getRequestQueue(): RequestQueue? {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(instance)
        } else println("Request is null")
        return mRequestQueue
    }
}