package com.drewcodesit.afiexplorer.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FavoritesListItemBinding
import es.dmoral.toasty.Toasty
import java.util.*

class FavoriteAdapter(
    private var ct: Context,
    private val favoriteListEntities: MutableList<FavoriteEntity?>?,
    private val favListener: FavAdapterListener,
    private var listener: ItemListener)
    : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>(), Filterable {

    private var favePubsListFiltered: MutableList<FavoriteEntity?>? = favoriteListEntities

    /**
     *
     * @param parent ViewGroup
     * @param viewType Int
     * @return ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FavoritesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    /**
     *
     * @param holder ViewHolder
     * @param position Int
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fl = favePubsListFiltered?.get(position)
        holder.pubNumber.text = fl!!.Number
        holder.pubTitle.text = fl.Title

        // Pop-Up Menu to Match MainActivity (Added 10 April 2022)
        val clipboard: ClipboardManager =
            ct.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        holder.buttonViewOption.setOnClickListener {
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
                val searchTerm = constraint?.toString() ?: ""
                favePubsListFiltered = (favoriteListEntities?.filter {
                    it?.Title?.contains(searchTerm, ignoreCase = true) == true ||
                            it?.Number?.contains(searchTerm, ignoreCase = true) == true
                } ?: emptyList()) as MutableList<FavoriteEntity?>?

                return FilterResults().apply { values = favePubsListFiltered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                favePubsListFiltered = results?.values as MutableList<FavoriteEntity?>
                notifyDataSetChanged()
            }
        }
    }
    // Filter Favorites By Title
    fun filterByTitle(){
        favoriteListEntities?.sortWith { pubTitle1, pubTitle2 ->
            pubTitle1?.Title!!.compareTo(pubTitle2?.Title!!)
        }
    }

    // Filter Favorites by Number
    fun filterByNumber(){
        favoriteListEntities?.sortWith {pubNumber1, pubNumber2 ->
            pubNumber1?.Number!!.compareTo(pubNumber2?.Number!!)
        }
    }

    /**
     *
     * @property pubNumber TextView
     * @property pubTitle TextView
     * @property buttonViewOption ImageView?
     */
    inner class ViewHolder(view: FavoritesListItemBinding) : RecyclerView.ViewHolder(view.root) {
        var pubNumber: TextView = view.pubNumber
        var pubTitle: TextView = view.pubTitle
        var buttonViewOption: ImageView = view.textViewOptions

        // Normal click to open publication instance from
        // Epubs site -> Matches Links from MainActivity
        init {
            view.root.setOnClickListener{
                favoriteListEntities?.get(bindingAdapterPosition)?.let { it1 ->
                    favListener.onFavsSelected(it1)
                }
           }
        }
    }

    interface FavAdapterListener {
        fun onFavsSelected(fav: FavoriteEntity)
    }
    interface ItemListener {
        fun onItemClicked(favsListToDelete: FavoriteEntity, position: Int)

    }
}
