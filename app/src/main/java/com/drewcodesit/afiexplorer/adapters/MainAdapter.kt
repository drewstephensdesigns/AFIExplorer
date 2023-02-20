package com.drewcodesit.afiexplorer.adapters


import android.annotation.SuppressLint
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
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.PubRowItemBinding
import com.drewcodesit.afiexplorer.model.Pubs
import es.dmoral.toasty.Toasty.*
import java.util.*

/**
 * Created by drewstephens on 5/13/2021.
 */
class MainAdapter(
    private var ct: Context,
    private val pubsList: ArrayList<Pubs>,
    private val listener: PubsAdapterListener
) : RecyclerView.Adapter<MainAdapter.MyViewHolder>(), Filterable {

    private var pubsListFiltered: ArrayList<Pubs> = pubsList // 18 JUL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = PubRowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        pubsListFiltered[position].let {publication ->
            holder.apply {
                pubNumber.text = publication.Number
                pubTitle.text = publication.Title
                pubCertDate.text = "Certified Current: ${publication.getCertDate()}"
                pubRescindOrg.text = "Rescind Org: ${publication.RescindOrg}"

                val actionText = when (publication.LastAction) {
                    "GM" -> ct.getString(R.string.guidance_memorandum)
                    "AC" -> ct.getString(R.string.ac)
                    "IC" -> ct.getString(R.string.interim_change)
                    "UpdateContact" -> ct.getString(R.string.update_contact)
                    "Rewrite" -> ct.getString(R.string.rewrite)
                    "Transfer" -> ct.getString(R.string.transfer)
                    "Correction" -> ct.getString(R.string.correction)
                    "CertifiedCurrent" -> ct.getString(R.string.certified_current)
                    else -> ct.getString(R.string.unknown_action)
                }
                pubLastAction.text = actionText

                // Clipboard Service
                val clipboard: ClipboardManager =
                    ct.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                buttonViewOption.setOnClickListener {
                    // Setting Theme
                    // Create Pop-up Menu
                    val wrapper: Context = ContextThemeWrapper(ct, R.style.AppTheme)
                    val popup = PopupMenu(wrapper, buttonViewOption)

                    // Inflates Menu
                    popup.inflate(R.menu.popup_main)
                    popup.setOnMenuItemClickListener { item ->
                        when(item.itemId) {
                            // Bookmark
                            R.id.menu1 -> {
                                val favoriteEntity = FavoriteEntity().apply {
                                    id = publication.PubID
                                    Number = publication.Number
                                    Title = publication.Title
                                    DocumentUrl = publication.DocumentUrl
                                }
                                val favoriteDAO = FavoriteDatabase.getDatabase(ct).favoriteDAO()
                                if (favoriteDAO?.titleExists(publication.Number.toString()) == 0) {
                                    // Does not exist, adds to database
                                    favoriteDAO.addData(favoriteEntity)
                                    success(
                                        ct,
                                        "${publication.Number}: Added to database!",
                                        Toast.LENGTH_SHORT,
                                        true
                                    ).show()
                                } else {
                                    // Does exist, updates faves
                                    favoriteDAO?.updateFaves(favoriteEntity)
                                    info(
                                        ct,
                                        "${publication.Number}: updated!",
                                        Toast.LENGTH_SHORT,
                                        false
                                    ).show()
                                }
                            }

                            // Copy Pub Number
                            R.id.menu2 -> {
                                val clip = ClipData.newPlainText("Copied Pub!", publication.Number)
                                clipboard.setPrimaryClip(clip)
                                info(ct,
                                    "Saved Pub Number to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // Copy Pub URL
                            R.id.menu3 -> {
                                val clip = ClipData.newPlainText("Copied Pub!", publication.DocumentUrl)
                                clipboard.setPrimaryClip(clip)
                                info(ct,
                                    "Saved Pub URL to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // Share
                            R.id.menu4 -> {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, publication.DocumentUrl)
                                    type = "text/plain"
                                }

                                val shareIntent = Intent.createChooser(sendIntent, null)
                                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(ct, shareIntent, null)
                            }

                            // Download File to Device
                            R.id.menu5 -> {
                                /**
                                 ** fileDir: Standard directory in which to place documents that have been created by the user.
                                 ** subPath: Creates sub-folder that the app will download to
                                 ** request: parses url of the selected AFI/Publication
                                 */
                                val manager =
                                    ct.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

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
                                info(
                                    ct,
                                    publication.Number + " downloaded to: " + fileDir + "/AFIExplorer/",
                                    Toast.LENGTH_SHORT,
                                    true
                                ).show()

                                // Deprecated
                                manager.enqueue(request)
                            }
                        }
                        false
                    }
                    popup.show()
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
                    pubsList.filter {
                            it.Title!!.lowercase(Locale.ROOT)
                                .contains(charString.lowercase(Locale.ROOT)) or
                                    it.Title!!.contains(charString) or

                                    it.Number!!.lowercase(Locale.ROOT)
                                        .contains(charString.lowercase(Locale.ROOT)) or
                                    it.Number!!.contains(charString)
                        }
                        .forEach {
                            filteredList.add(it)
                        }
                    filteredList
                }
                return FilterResults().apply { values = pubsListFiltered }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                pubsListFiltered = results?.values as ArrayList<Pubs>
                results.count = pubsListFiltered.size
                notifyDataSetChanged()
            }
        }
    }


    fun filterByRescindOrg() : Filter{
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                pubsListFiltered = if (charString.isEmpty()) pubsList else {
                    val filteredList = ArrayList<Pubs>()
                    pubsList.filter {
                        it.RescindOrg!!.lowercase(Locale.ROOT)
                            .contains(charString.lowercase(Locale.ROOT)) or
                                it.RescindOrg!!.contains(charString)
                    }
                        .forEach {
                            filteredList.add(it)
                        }
                    filteredList
                }
                return FilterResults().apply { values = pubsListFiltered }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                pubsListFiltered = results?.values as ArrayList<Pubs>
                results.count = pubsListFiltered.size
                notifyDataSetChanged()
            }
        }
    }

    /** pubNumber - Publication series (ex: AFI21-101)
     ** pubTitle - Publication title (ex: Aircraft and Equipment Maintenance Management
     ** pubLastAction - Last action completed on publication (ex: Correction)
     ** pubCertDate - Certified current date of publication (ex: Jan 2021)
     ** buttonViewOption - Pop-up menu to Save/Share/Copy pub data
     **/
    inner class MyViewHolder(view: PubRowItemBinding) : RecyclerView.ViewHolder(view.root) {

        var pubNumber: TextView = view.pubNumber
        var pubTitle: TextView = view.pubTitle
        var pubLastAction: TextView = view.pubLastAction
        var pubCertDate: TextView = view.pubCertDate
        var pubRescindOrg: TextView = view.pubRescindOrg
        var buttonViewOption: ImageView = view.textViewOptions

        // Clicking on any publication in the list allows user to open PDF
        init {
            view.root.setOnClickListener {
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
