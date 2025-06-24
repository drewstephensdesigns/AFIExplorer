package com.drewcodesit.afiexplorer.utils

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Environment
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDatabase
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import es.dmoral.toasty.Toasty
import java.lang.Math.sqrt

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
    const val VECTOR_TABLE = "vector_table"
    const val AFI_TOPICS_TABLE = "afi_topics"

    // Displays database as version number in OptionsFragment.kt
    fun Context.getDBVersion() = FavoriteDatabase.getDatabase(this).openHelper.readableDatabase.version.toString()

    // Global function to save text to clipboard
    fun save(context: Context, text: String) {
        val clip = ClipData.newPlainText(context.getString(R.string.copied_to_clipboard), text)
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
    }

    // Global function to share publication
    fun sharePublication(ct: Context, url: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ct.startActivity(shareIntent)
    }

    // Global function to download publication
    fun downloadPublication(ct: Context, url: String, title: String, subTitle: String){

        val manager = ct.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(url.toUri())
            .setTitle(title)
            .setDescription(subTitle)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS, "/AFIExplorer/${subTitle}.pdf")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        manager.enqueue(request)
    }


    fun showToast(context: Context, message: String, type: ToastType, drawableResId: Drawable?) {
        val toast = when (type) {
            ToastType.INFO -> Toasty.info(context, message, Toast.LENGTH_SHORT)
            ToastType.SUCCESS -> Toasty.success(context, message, Toast.LENGTH_SHORT)
            ToastType.WARNING -> Toasty.warning(context, message, Toast.LENGTH_SHORT)
            ToastType.ERROR -> Toasty.error(context, message, Toast.LENGTH_SHORT)
            ToastType.NORMAL -> { Toasty.normal(context, message, Toast.LENGTH_SHORT)
            }
        }
        toast.show()
    }

    fun generateFakeEmbedding(text: String): List<Float> {
        return List(256) { (0..100).random() / 100f }  // 256-dim random embedding
    }

    fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Float {
        val dotProduct = vec1.zip(vec2).sumOf { (a, b) -> (a * b).toDouble() }
        val mag1 = kotlin.math.sqrt(vec1.sumOf { (it * it).toDouble() })
        val mag2 = kotlin.math.sqrt(vec2.sumOf { (it * it).toDouble() })
        return if (mag1 == 0.0 || mag2 == 0.0) 0f else (dotProduct / (mag1 * mag2)).toFloat()
    }

}