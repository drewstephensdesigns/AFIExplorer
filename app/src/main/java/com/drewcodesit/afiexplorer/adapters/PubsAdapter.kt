package com.drewcodesit.afiexplorer.adapters

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.adapters.PubsAdapter.PubsViewHolder
import com.drewcodesit.afiexplorer.database.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.PubRowItemBinding
import com.drewcodesit.afiexplorer.model.Pubs
import com.drewcodesit.afiexplorer.utils.Config
import es.dmoral.toasty.Toasty.info
import es.dmoral.toasty.Toasty.success
import java.util.*

class PubsAdapter(
    private var ct: Context,
    private var pubsList: List<Pubs>,
    private val listener: MainClickListener
) : ListAdapter<Pubs, PubsViewHolder>(PubsDiffCallback()), Filterable {

    private var pubsListFiltered: List<Pubs> = pubsList

    private val favoriteDAO = FavoriteDatabase.getDatabase(ct).favoriteDAO()

    private lateinit var viewLifecycleOwner: LifecycleOwner

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        viewLifecycleOwner = recyclerView.findViewTreeLifecycleOwner()!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PubsViewHolder {
        val binding = PubRowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PubsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PubsViewHolder, position: Int) {
        val publication = pubsListFiltered[position]

        val fEntity = FavoriteEntity().apply {
            id = publication.PubID
            Number = publication.Number!!
            Title = publication.Title!!
            DocumentUrl = publication.DocumentUrl!!
        }

        if (favoriteDAO?.titleExists(publication.Number.toString()) == 1) {
            // Does exist, updates
            favoriteDAO.update(fEntity)
        }

        holder.apply {
            pubNumber.text = publication.Number
            pubTitle.text = publication.Title

            // string resource with a placeholder, and you're inserting the dynamic value returned
            val certifiedDateString: String =
                ct.getString(R.string.certified_date_placeholder, publication.getCertDate())
            val gmDateString: String =
                ct.getString(R.string.gm_date_placeholder, publication.getGMDate())
            val rescindOrgString: String =
                ct.getString(R.string.rescind_placeholder, publication.RescindOrg)

            // Displays Certified Current Date of publication
            pubCertDate.text = certifiedDateString

            // Gets date if pub has Guidance Memorandum
            if (publication.GMDate != null) {
                pubGMDate.text = gmDateString
            } else {
                pubGMDate.text = ct.getString(R.string.gm_placeholder)
            }

            // Displays Rescind Org
            pubRescindOrg.text = rescindOrgString

            // Last Action category hardcoded for brevity
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

            // Popup Menu
            buttonViewOption.setOnClickListener {
                // Setting Theme
                // Create Pop-up Menu
                val wrapper: Context = ContextThemeWrapper(ct, R.style.AppTheme)
                val popup = PopupMenu(wrapper, buttonViewOption)

                // Inflates Menu
                popup.inflate(R.menu.popup_main)

                // Click listener
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        // Bookmark
                        R.id.mSave -> {
                            if (favoriteDAO?.titleExists(publication.Number.toString()) == 0) {
                                favoriteDAO.addData(fEntity)
                                showSuccessToast("${publication.Number}: added to database!")
                            } else {
                                favoriteDAO?.update(fEntity)
                                showInfoToast("${publication.Number}: updated!")
                            }
                        }

                        // Copy AFI URL
                        R.id.mCopyURL -> {
                            Config.save(ct, publication.DocumentUrl!!)
                        }

                        // Share AFI by URL
                        R.id.mShare -> {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, publication.DocumentUrl)
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
                            val subPath = "/AFIExplorer// ${publication.Title}"
                            val request =
                                DownloadManager.Request(Uri.parse(publication.DocumentUrl))

                            request.setTitle(publication.Number)
                            request.setDescription(publication.Title)
                            request.setDestinationInExternalPublicDir(
                                fileDir, "$subPath.pdf"
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

    override fun getItemCount(): Int {
        return pubsListFiltered.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    private fun showInfoToast(message: String) {
        info(ct, message, Toast.LENGTH_SHORT, true).show()
    }

    private fun showSuccessToast(message: String) {
        success(ct, message, Toast.LENGTH_SHORT, true).show()
    }

    // This method is called automatically by the system whenever the user enters text
    // in the search field of the associated SearchView. The FilterResults object returned
    // by performFiltering() is passed to the publishResults() method of the adapter, which
    // updates the contents of the list or grid to display the filtered items
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                pubsListFiltered = if (charString.isEmpty()) pubsList else {
                    val filteredList = ArrayList<Pubs>()
                    pubsList.filter {
                        it.Title!!.contains(charString, ignoreCase = true) or it.Number!!.contains(
                            charString,
                            ignoreCase = true
                        ) or it.RescindOrg!!.contains(charString, ignoreCase = true)
                    }.forEach {
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
                pubsListFiltered = if (charString.isEmpty()) pubsList else {
                    val filteredList = ArrayList<Pubs>()
                    pubsList.filter {
                        it.RescindOrg!!.lowercase(Locale.ROOT)
                            .contains(charString.lowercase(Locale.ROOT)) or it.RescindOrg!!.contains(
                            charString
                        )
                    }.forEach {
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

    /**
     * The PubsViewHolder class has a bind() function that takes a Pubs
     * object and sets its data to the views. we set the Number, Title, Last Action
     * Cert Date and Rescind Org properties of the Pubs object to their respective views.
     */
    inner class PubsViewHolder(binding: PubRowItemBinding) : RecyclerView.ViewHolder(binding.root) {

        // used to pass the click events to the Fragment that is hosting the RecyclerView
        // When the item view is clicked, the onMainPubsClickListener method of the listener
        // is called, passing in the Pubs object that is associated with the clicked item.
        // bindingAdapterPosition is used to get the position of the clicked item in the filtered list of Pubs
        init {
            binding.root.setOnClickListener {
                listener.onMainPubsClickListener(pubsListFiltered[bindingAdapterPosition])
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
            return oldItem.DocumentUrl == newItem.DocumentUrl && oldItem.Title == newItem.Title && oldItem.Number == newItem.Number && oldItem.PubID == newItem.PubID
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
