package com.drewcodesit.afiexplorer.adapters


import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.view.MainActivity.Companion.favoriteDatabase
import es.dmoral.toasty.Toasty.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by drewstephens on 5/13/2021.
 */
class MainAdapter(
    private var ct: Context,
    private val pubsList: ArrayList<Pubs>,
    private val listener: PubsAdapterListener)
    : RecyclerView.Adapter<MainAdapter.MyViewHolder>(), Filterable {

    private var pubsListFiltered: ArrayList<Pubs> = pubsList // 18 JUL


    private lateinit var certDate:String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
       val view =
            LayoutInflater.from(parent.context).inflate(R.layout.pub_row_item, parent, false)
        return MyViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val publication = pubsListFiltered[position]

        val favoriteEntity = FavoriteEntity()
        val id = publication.getId()
        val number = publication.Number
        val title = publication.Title
        val url = publication.DocumentUrl
        val rescindOrg = publication.RescindOrg

        favoriteEntity.id = id
        favoriteEntity.Number = number
        favoriteEntity.Title = title
        favoriteEntity.DocumentUrl = url

        // Clipboard Service
        val clipboard: ClipboardManager =
            ct.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Creates PopUp Menu on Recyclerview
        // Modified from https://www.simplifiedcoding.net/create-options-menu-recyclerview-item-tutorial/#
        holder.buttonViewOption?.setOnClickListener {
            // Setting Theme to Popup Menu
            // Creating a Popup Menu
            val wrapper: Context = ContextThemeWrapper(ct, R.style.AppTheme)
            val popup = PopupMenu(wrapper, holder.buttonViewOption)
            //inflating menu from xml resource
            popup.inflate(R.menu.popup_main)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    // Bookmark
                    R.id.menu1 -> {
                        if (FavoriteDatabase.getDatabase(ct).favoriteDAO()?.titleExists(number.toString()) == 0){
                            // Does not exist, adds to database
                            favoriteDatabase?.favoriteDAO()?.addData(favoriteEntity)
                            success(ct, "$number: added to favorites!", Toast.LENGTH_SHORT, true).show()
                        } else {
                            // Does exists, updates the database
                            favoriteDatabase?.favoriteDAO()?.updateFaves(favoriteEntity)
                            info(ct, "$number has been updated", Toast.LENGTH_SHORT, false).show()
                            //Log.i("MAIN_ADAPTER", "${FavoriteDatabase.getDatabase(ct).favoriteDAO()?.titleExists(number.toString())}")
                        }
                    }

                    // Copy Pub Number
                    R.id.menu2 -> {
                        val clip = ClipData.newPlainText("Copied Pub!", number)
                        clipboard.setPrimaryClip(clip)
                        info(ct, "Saved Pub Number to clipboard", Toast.LENGTH_SHORT).show()
                    }

                    // Copy URL
                    R.id.menu3 -> {
                        val clip = ClipData.newPlainText("Copied Pub!", url)
                        clipboard.setPrimaryClip(clip)
                        info(ct, "Saved Pub URL to clipboard", Toast.LENGTH_SHORT).show()
                    }

                    // Share
                    R.id.menu4 -> {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, url)
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(ct, shareIntent, null)
                    }

                    // Downloads file
                    R.id.menu5 -> {

                        /**
                         ** fileDir: Standard directory in which to place documents that have been created by the user.
                         ** subPath: Creates sub-folder that the app will download to
                         ** request: parses url of the selected AFI/Publication
                         */
                        val manager = ct.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                        val fileDir = Environment.DIRECTORY_DOCUMENTS
                        val subPath = "/AFIExplorer// ${publication.Title}"
                        val request = DownloadManager.Request(Uri.parse(publication.DocumentUrl))

                        request.setTitle(publication.Number)
                        request.setDescription(publication.Title)
                        request.setDestinationInExternalPublicDir(
                            fileDir,
                            "$subPath.pdf"
                        )
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        info(ct, publication.Number + " downloaded to: " + fileDir + "/AFIExplorer/", Toast.LENGTH_SHORT, true).show()

                        // Deprecated
                        //request.setVisibleInDownloadsUi(true)
                        manager.enqueue(request)
                    }
                }
                false
            }
            //displaying the popup
            popup.show()
        }

        holder.also { h ->
            with(publication) {

                // Converts Milliseconds to Readable Date Format
                // Certified Current Date from EPubs Site
                val rawdate = CertDate

                // Calendar Instance
                val calendar = Calendar.getInstance()
                val dateReplace = rawdate.replace("/Date(", "").replace(")/", "")
                val timeInMillis = java.lang.Long.valueOf(dateReplace)

                calendar.timeInMillis = timeInMillis

                // Converts Calendar Instance from Long to Simple
                certDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timeInMillis))

                // Textfields
                h.pubNumber!!.text = Number
                h.pubTitle!!.text = Title

                "Certified Current: $certDate".also { h.pubCertDate!!.text = it }

                "Rescind Org: $rescindOrg".also { h.pubRescindOrg!!.text = it }

                // Hard Coding E-Pubs Actions for Grammar
                return when(LastAction){
                    "GM" -> h.pubLastAction!!.text = ct.getString(R.string.guidance_memorandum)
                    "IC" -> h.pubLastAction!!.text = ct.getString(R.string.interim_change)
                    "UpdateContact" -> h.pubLastAction!!.text = ct.getString(R.string.update_contact)
                    "Rewrite" -> h.pubLastAction!!.text = ct.getString(R.string.rewrite)
                    "Transfer" -> h.pubLastAction!!.text = ct.getString(R.string.transfer)
                    "Correction" -> h.pubLastAction!!.text = ct.getString(R.string.correction)
                    else -> h.pubLastAction!!.text = ct.getString(R.string.unknown_action)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return pubsListFiltered.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     ** Filters SearchView to text matching requested search
     ** Displays Error Toast if no results are found
     ** https://johncodeos.com/how-to-add-search-in-recyclerview-using-kotlin/
     ** https://github.com/johncodeos-blog/SearchRecyclerViewExample
     **/
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {

                val charString = constraint?.toString() ?: ""
                pubsListFiltered = if (charString.isEmpty()) pubsList else {
                    val filteredList = ArrayList<Pubs>()
                    pubsList
                        .filter {
                            it.Title!!.lowercase(Locale.ROOT).contains(charString.lowercase(Locale.ROOT)) or
                            it.Title!!.contains(charString) or

                            it.Number!!.lowercase(Locale.ROOT).contains(charString.lowercase(Locale.ROOT)) or
                            it.Number!!.contains(charString) or

                            it.RescindOrg!!.lowercase(Locale.ROOT).contains(charString.lowercase(Locale.ROOT)) or
                            it.RescindOrg!!.contains(charString)

                        }
                        .forEach {
                            filteredList.add(it)
                        }
                    filteredList
                }
                return FilterResults().apply { values = pubsListFiltered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                pubsListFiltered = results?.values as ArrayList<Pubs>
                results.count = pubsListFiltered.size
                notifyDataSetChanged()
            }
        }
    }

    /**
     ** pubNumber - Publication series (ex: AFI21-101)
     ** pubTitle - Publication title (ex: Aircraft and Equipment Maintenance Management
     ** pubLastAction - Last action completed on publication (ex: Correction)
     ** pubCertDate - Certified current date of publication (ex: Jan 2021)
     ** buttViewOption - Pop-up menu to Save/Share/Copy pub data
     **/
    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var pubNumber: TextView? = view.findViewById<View?>(R.id.pubNumber) as TextView
        var pubTitle: TextView? = view.findViewById<View?>(R.id.pubTitle) as TextView
        var pubLastAction: TextView? = view.findViewById<View?>(R.id.pubLastAction) as TextView
        var pubCertDate: TextView? = view.findViewById<View?>(R.id.pubCertDate) as TextView
        var pubRescindOrg: TextView? = view.findViewById<View?>(R.id.pubRescindOrg) as TextView
        var buttonViewOption: ImageView? = view.findViewById<View?>(R.id.textViewOptions) as ImageView

        // Clicking on any publication in the list allows user to open PDF
        init {
            view.setOnClickListener {
                listener.onPubsSelected(pubsListFiltered[bindingAdapterPosition])
            }
        }
    }

    // Reference to Main Activity to open
    // selected PDFs in app or defaults to Pdf-Viewer
    // Library Source: https://github.com/afreakyelf/Pdf-Viewer
    interface PubsAdapterListener {
        fun onPubsSelected(pubs: Pubs)
    }
}
