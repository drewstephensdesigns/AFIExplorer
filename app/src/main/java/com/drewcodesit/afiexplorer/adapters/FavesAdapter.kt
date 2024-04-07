package com.drewcodesit.afiexplorer.adapters

import android.content.Context
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.adapters.FavesAdapter.FaveViewHolder
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.FavoritesListItemBinding
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.FavesClickListener


class FavesAdapter(
    private var ct: Context,
    private val faveEntity: MutableList<FavoriteEntity>,
    private var selectedListener: FavesClickListener,
    private var deletedListener: FavesClickListener

) : RecyclerView.Adapter<FaveViewHolder>(), Filterable {

    private var favePubsListFiltered: MutableList<FavoriteEntity> = faveEntity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaveViewHolder {
        val binding = FavoritesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaveViewHolder, position: Int) {
       val fl = favePubsListFiltered[position]
        holder.pubNumber.text = fl.Number
        holder.pubTitle.text = fl.Title

        holder.buttonViewOption.setOnClickListener {
            // Setting Theme to Popup Menu
            // Creating a Popup Menu
            val wrapper: Context = ContextThemeWrapper(ct, R.style.AppTheme)
            val popup = PopupMenu(wrapper, holder.buttonViewOption)

            //inflating menu from xml resource
            popup.inflate(R.menu.popup_faves)
            popup.setOnMenuItemClickListener { item ->
                when(item.itemId){

                    // Copy to Clipboard
                    R.id.menuActionCopy -> {
                        Config.save(ct, fl.DocumentUrl)
                    }

                    // Share Publication URL
                    R.id.menuActionShare -> {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, fl.DocumentUrl)
                            type = "text/plain"
                        }

                        val shareIntent = Intent.createChooser(sendIntent, null)
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ContextCompat.startActivity(ct, shareIntent, null)
                    }

                    // Delete single item
                    R.id.menuActionDelete -> {
                        deletedListener.onFavesDeletedListener(fl, position)
                    }
                }
                false
            }
            popup.show()
        }
    }

    inner class FaveViewHolder(binding: FavoritesListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                faveEntity[bindingAdapterPosition].let {
                    selectedListener.onFavesSelectedListener(it)
                }
            }
        }

        var pubNumber: TextView = binding.pubNumber
        var pubTitle: TextView = binding.pubTitle
        var buttonViewOption: ImageView = binding.textViewOptions
    }

    override fun getItemCount() = favePubsListFiltered.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchTerm = constraint?.toString() ?: ""
                favePubsListFiltered = faveEntity.filter {
                    it.Title.contains(searchTerm, ignoreCase = true) || it.Number.contains(searchTerm, ignoreCase = true)
                } as MutableList<FavoriteEntity>

                return FilterResults().apply { values = favePubsListFiltered }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                favePubsListFiltered = results?.values as MutableList<FavoriteEntity>
                results.count = favePubsListFiltered.size
                notifyDataSetChanged()
            }
        }
    }

    // Filter Favorites By Title
    fun filterByTitle(){
        faveEntity.sortWith { pubTitle1, pubTitle2 ->
            pubTitle1?.Title!!.compareTo(pubTitle2?.Title!!)
        }
    }

    // Filter Favorites by Number
    fun filterByNumber(){
        faveEntity.sortWith { pubNumber1, pubNumber2 ->
            pubNumber1?.Number!!.compareTo(pubNumber2?.Number!!)
        }
    }
}