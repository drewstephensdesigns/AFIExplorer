package com.drewcodesit.afiexplorer.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import es.dmoral.toasty.Toasty
import java.util.*

class FavoriteAdapter(
    private var ct: Context,
    private val favoriteListEntities: MutableList<FavoriteEntity?>?,
    private val favListener: FavAdapterListener,
    private var listener: ItemListener)
    : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>(), Filterable {

    //ListAdapter<Pubs, FavoriteAdapter.ViewHolder>(DiffCallback), Filterable {

    private var favePubsListFiltered: MutableList<FavoriteEntity?>? = favoriteListEntities

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.favorites_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val fl = favePubsListFiltered?.get(position)//favoriteListEntities?.get(position)
        holder.pubNumber.text = fl!!.Number
        holder.pubTitle.text = fl.Title

        // Pop-Up Menu to Match MainActivity (Added 10 April 2022)
        val clipboard: ClipboardManager =
            ct.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        holder.buttonViewOption?.setOnClickListener {
            // Setting Theme to Popup Menu
            // Creating a Popup Menu
            val wrapper: Context = ContextThemeWrapper(ct, R.style.AppTheme)
            val popup = PopupMenu(wrapper, holder.buttonViewOption)
            //inflating menu from xml resource
            popup.inflate(R.menu.popup_faves)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    // Copy Pub Number
                    R.id.menu1 -> {
                        val clip = ClipData.newPlainText("Copied Pub!", fl.Number)
                        clipboard.setPrimaryClip(clip)
                        Toasty.info(ct, "Saved Pub Number to clipboard", Toast.LENGTH_SHORT).show()
                    }

                    // Copy URL
                    R.id.menu2 -> {
                        val clip = ClipData.newPlainText("Copied Pub!", fl.DocumentUrl)
                        clipboard.setPrimaryClip(clip)
                        Toasty.info(ct, "Saved Pub URL to clipboard", Toast.LENGTH_SHORT).show()
                    }

                    // Share
                    R.id.menu3 -> {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, fl.DocumentUrl)
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ContextCompat.startActivity(ct, shareIntent, null)
                    }
                    // Delete
                    R.id.menu4 -> {
                        listener.onItemClicked(fl, position)
                    }
                }
                false
            }
            //displaying the popup
            popup.show()
        }
    }

    override fun getItemCount(): Int {
        return if(favePubsListFiltered != null){
            favePubsListFiltered!!.size
        }else {
            0
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {

                val charString = constraint?.toString() ?: ""
                favePubsListFiltered = if (charString.isEmpty()) favoriteListEntities else {
                    val filteredList = ArrayList<FavoriteEntity?>()
                    favoriteListEntities
                        ?.filter {
                            it?.Title!!.lowercase(Locale.ROOT).contains(charString.lowercase(Locale.ROOT)) or
                                    it.Number!!.lowercase(Locale.ROOT).contains(constraint!!) or
                                    it.Number!!.contains(constraint) or it.Title!!.contains(charString)
                        }
                        ?.forEach {
                            filteredList.add(it)
                        }
                    filteredList
                }
                return FilterResults().apply { values = favePubsListFiltered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                favePubsListFiltered = results?.values as ArrayList<FavoriteEntity?>
                notifyDataSetChanged()
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var pubNumber: TextView = view.findViewById(R.id.pubNumber)
        var pubTitle: TextView = view.findViewById(R.id.pubTitle)
        var buttonViewOption: ImageView? = view.findViewById<View?>(R.id.textViewOptions) as ImageView

        // Normal click to open publication instance from
        // Epubs site -> Matches Links from MainActivity
        init {
            view.setOnClickListener{
                favoriteListEntities?.get(bindingAdapterPosition)?.let { it1 ->
                    favListener.onFavsSelected(it1)
                }
           }
        }
    }

    //
    interface FavAdapterListener {
        fun onFavsSelected(fav: FavoriteEntity)
    }
    interface ItemListener {
        fun onItemClicked(favsListToDelete: FavoriteEntity, position: Int)
    }
}
