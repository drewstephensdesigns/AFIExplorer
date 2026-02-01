package com.drewcodesit.afiexplorer.utils

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import es.dmoral.toasty.Toasty

object Config {

    // Base API
    const val BASE_URL = "https://afiexplorer.drewstephensdesigns.workers.dev/v2/"

    // Featured Pubs
    const val FEATURED_PUBS_URL = "https://drewstephensdesigns.github.io/AFIExplorer/Featured/"

    // Room Database Name
    const val DATABASE_NAME = "myfavdb"

    // Room Database Table Names
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

    /**
     * Displays a styled toast message to the user using the Toasty library.
     * * @param context The application or activity context.
     * @param message The text content to be displayed in the toast.
     * @param type The [ToastType] (INFO, SUCCESS, WARNING, ERROR, NORMAL) determining the toast's styling.
     * @param drawableResId An optional icon to display alongside the message.
     */
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

    /**
     * Removes a favorite entry from the local database and provides visual feedback to the user.
     * * This function performs a synchronous deletion via [FavoriteDAO] and displays
     * an info toast confirming the removal of the publication by its number.
     * * @param context The context used to access the database and resources.
     * @param favorite The [FavoriteEntity] object to be removed from the database.
     */
    fun deleteFavorite(context: Context, favorite: FavoriteEntity) {
        FavoriteDatabase.getDatabase(context).favoriteDAO()?.delete(favorite)
        showToast(
            context,
            context.resources.getString(R.string.delete_hint, favorite.pubNumber),
            ToastType.INFO,
            AppCompatResources.getDrawable(context, R.drawable.ic_error)
        )
    }

    /**
     * Maps a [FavoriteEntity] database object to a [Pubs] domain model.
     * * Used to transform persistent data entities into a format suitable for the
     * UI or business logic, specifically transferring identifiers and document metadata.
     * * @return A new instance of [Pubs] populated with the entity's data.
     */
    fun FavoriteEntity.toPubs(): Pubs {
        return Pubs(
            pubID = this.id,
            pubNumber = this.pubNumber,
            pubTitle = this.pubTitle,
            pubDocumentUrl = this.pubDocumentUrl
            //pubLastAction = this.p,
            //pubCertDate = this.pubCertDate
        )
    }
}