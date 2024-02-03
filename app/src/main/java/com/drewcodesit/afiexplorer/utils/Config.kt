package com.drewcodesit.afiexplorer.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import com.drewcodesit.afiexplorer.R

object Config {

    // Base API
    const val BASE_URL = "https://api.afiexplorer.com"

    // Featured Pubs
    const val FEATURED_PUBS_URL = "https://drewstephensdesigns.github.io/AFIExplorer/Featured/"

    // Database
    const val DATABASE_NAME = "myfavdb"

    // Table
    const val TABLE_NAME = "favoriteslist"

    fun save(context: Context, text: String) {
        val clip = ClipData.newPlainText(context.getString(R.string.copied_to_clipboard), text)
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
    }
}

