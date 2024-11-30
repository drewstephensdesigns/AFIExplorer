package com.drewcodesit.afiexplorer.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteDatabase

object Config {

    // Base API
    const val BASE_URL = "https://afiexplorer.drewstephensdesigns.workers.dev/v2/"

    // Featured Pubs
    // TODO("move to cloudflare worker")
    const val FEATURED_PUBS_URL = "https://drewstephensdesigns.github.io/AFIExplorer/Featured/"

    // Database
    const val DATABASE_NAME = "myfavdb"

    // Table
    const val TABLE_NAME = "favoriteslist"

    fun save(context: Context, text: String) {
        val clip = ClipData.newPlainText(context.getString(R.string.copied_to_clipboard), text)
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
    }

    fun Context.getDBVersion() = FavoriteDatabase.getDatabase(this).openHelper.readableDatabase.version.toString()

}