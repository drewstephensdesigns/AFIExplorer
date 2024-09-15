package com.drewcodesit.afiexplorer.ui.browse

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.BrowseItemsViewBinding
import com.drewcodesit.afiexplorer.models.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import es.dmoral.toasty.Toasty
import es.dmoral.toasty.Toasty.info
import es.dmoral.toasty.Toasty.success
import java.util.Locale

class BrowseAdapter(
    private val ct: Context,
    private var pubsList: List<Pubs>,
    private val listener: MainClickListener
) : ListAdapter<Pubs, BrowseAdapter.BrowseVH>(PubsDiffCallback()), Filterable {

    private var publicationsList : List<Pubs> = pubsList

    private val favoriteDAO = FavoriteDatabase.getDatabase(ct).favoriteDAO()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseVH {
        val binding = BrowseItemsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrowseVH(binding)
    }

    override fun getItemCount(): Int {
        return publicationsList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getPubs(pubs: List<Pubs>){
        publicationsList = pubs
        notifyDataSetChanged()
    }

    private fun showInfoToast(message: String) {
        info(ct, message, Toasty.LENGTH_SHORT, true).show()
    }

    private fun showSuccessToast(message: String) {
        success(ct, message, Toasty.LENGTH_SHORT, true).show()
    }

    override fun onBindViewHolder(holder: BrowseVH, position: Int) {
        val publications = publicationsList[position]

        val fEntity = FavoriteEntity().apply {
            id = publications.pubID
            pubNumber = publications.pubNumber!!
            pubTitle = publications.pubTitle!!
            pubDocumentUrl = publications.pubDocumentUrl!!
        }

        if(favoriteDAO?.titleExists(publications.pubNumber.toString()) == 1){
            favoriteDAO.update(fEntity)
        }

        holder.apply {
            pubNumber.text = publications.pubNumber
            pubTitle.text = publications.pubTitle

            // string resource with a placeholder, and you're inserting the dynamic value returned
            val certifiedDateString: String = ct.getString(R.string.certified_date_placeholder, publications.getCertDate())

            val gmDateString: String = ct.getString(R.string.gm_date_placeholder, publications.getGMDate())

            val rescindOrgString: String = ct.getString(R.string.rescind_placeholder, publications.pubRescindOrg)

            // Displays Certified Current Date of publication
            pubCertDate.text = certifiedDateString

            // Gets date if pub has Guidance Memorandum
            if (publications.pubGMDate != null) {
                pubGMDate.text = gmDateString
            } else {
                pubGMDate.text = ct.getString(R.string.gm_placeholder)
            }

            // Displays Rescind Org
            pubRescindOrg.text = rescindOrgString

            // Last Action category hardcoded for brevity
            val actionText = when (publications.pubLastAction) {
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

            // Popup Menu
            buttonViewOption.setOnClickListener {
                // Setting Theme
                // Create Pop-up Menu
                val wrapper: Context = ContextThemeWrapper(ct, R.style.Theme_AFIExplorer)
                val popup = PopupMenu(wrapper, buttonViewOption)

                // Inflates Menu
                popup.inflate(R.menu.popup_main)

                // Click listener
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        // Bookmark
                        R.id.mSave -> {
                            if (favoriteDAO?.titleExists(publications.pubNumber.toString()) == 0) {
                                favoriteDAO.addData(fEntity)
                                showSuccessToast("${publications.pubNumber}: added to database!")
                            } else {
                                favoriteDAO?.update(fEntity)
                                showInfoToast("${publications.pubNumber}: updated!")
                            }
                        }

                        // Copy AFI URL
                        R.id.mCopyURL -> {
                            Config.save(ct, publications.pubDocumentUrl!!)
                        }

                        // Share AFI by URL
                        R.id.mShare -> {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, publications.pubDocumentUrl)
                                type = "text/plain"
                            }

                            val shareIntent = Intent.createChooser(sendIntent, null)
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(ct, shareIntent, null)
                        }

                        // Download file to device
                        R.id.mDownload -> {
                            /**
                             ** fileDir: Standard directory in which to place documents that have been created by the user.
                             ** subPath: Creates sub-folder that the app will download to
                             ** request: parses url of the selected AFI/Publication
                             */
                            val manager =
                                ct.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                            val fileDir = Environment.DIRECTORY_DOCUMENTS
                            val subPath = "/AFIExplorer// ${publications.pubTitle}"
                            val request =
                                DownloadManager.Request(Uri.parse(publications.pubDocumentUrl))

                            request.setTitle(publications.pubNumber)
                            request.setDescription(publications.pubTitle)
                            request.setDestinationInExternalPublicDir(
                                fileDir, "$subPath.pdf"
                            )
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            info(
                                ct,
                                publications.pubNumber + " downloaded to: " + fileDir + "/AFIExplorer/",
                                Toasty.LENGTH_SHORT,
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

    // This method is called automatically by the system whenever the user enters text
    // in the search field of the associated SearchView. The FilterResults object returned
    // by performFiltering() is passed to the publishResults() method of the adapter, which
    // updates the contents of the list or grid to display the filtered items
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                publicationsList = if (charString.isEmpty()) pubsList else {
                    val filteredList = ArrayList<Pubs>()
                    pubsList.filter {
                        it.pubTitle!!.contains(charString, ignoreCase = true) or it.pubNumber!!.contains(
                            charString,
                            ignoreCase = true
                        ) or it.pubRescindOrg!!.contains(charString, ignoreCase = true)
                    }.forEach {
                        filteredList.add(it)

                    }
                    filteredList
                }
                return FilterResults().apply { values = publicationsList }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val searchResults: ArrayList<Pubs> = results?.values as ArrayList<Pubs>

                //publicationsList = results?.values as ArrayList<Pubs>
                //results.count = publicationsList.size
                results.count = searchResults.size
                notifyDataSetChanged()
            }
        }
    }

    /**
     * This method is called by the system whenever the user selects an organization
     * from the filtering dialog in HomeFragment. The FilterResults object returned
     * by performFiltering() is passed to the publishResults() method of the adapter, which
     * updates the contents of the list or grid to display the filtered items
     */
    fun filterByRescindOrg(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                publicationsList = if (charString.isEmpty()) pubsList else {
                    val filteredList = ArrayList<Pubs>()
                    pubsList.filter {
                        it.pubRescindOrg!!.lowercase(Locale.ROOT)
                            .contains(charString.lowercase(Locale.ROOT)) or it.pubRescindOrg!!.contains(
                            charString
                        )
                    }.forEach {
                        filteredList.add(it)
                    }
                    filteredList
                }
                return FilterResults().apply { values = publicationsList }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                publicationsList = results?.values as ArrayList<Pubs>
                results.count = publicationsList.size
                notifyDataSetChanged()
            }
        }
    }


    inner class BrowseVH(binding: BrowseItemsViewBinding) : RecyclerView.ViewHolder(binding.root){

        //init
        init {
            binding.root.setOnClickListener {
                listener.onMainPubsClickListener(publicationsList[bindingAdapterPosition])
            }
        }

        var pubNumber: TextView = binding.pubNumber
        var pubTitle: TextView = binding.pubTitle
        var pubLastAction: TextView = binding.pubLastAction
        var pubCertDate: TextView = binding.pubCertDate
        var pubGMDate: TextView = binding.pubGMDate
        var pubRescindOrg: TextView = binding.pubRescindOrg
        var buttonViewOption: ImageView = binding.textViewOptions

    }

    // compares the old and new Pubs objects and returns
    // true if they are the same. We use this class to
    // optimize the updates in the RecyclerView.
    class PubsDiffCallback : DiffUtil.ItemCallback<Pubs>() {
        override fun areItemsTheSame(oldItem: Pubs, newItem: Pubs): Boolean {
            return oldItem.pubDocumentUrl == newItem.pubDocumentUrl &&
                    oldItem.pubTitle == newItem.pubTitle &&
                    oldItem.pubNumber == newItem.pubNumber &&
                    oldItem.pubID == newItem.pubID
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Pubs, newItem: Pubs): Boolean {
            return oldItem == newItem
        }
    }

    // notify the parent class when a main item in the RecyclerView is clicked.
    // This allows the parent class to respond to the click event and perform some action,
    // such as opening a detail view for the selected item.
    interface MainClickListener {fun onMainPubsClickListener(pubs: Pubs)}
}