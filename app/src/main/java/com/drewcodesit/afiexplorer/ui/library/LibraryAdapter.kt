package com.drewcodesit.afiexplorer.ui.library

import android.content.Context
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.LibraryItemsViewBinding
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.FavesListenerItem


class LibraryAdapter(
    private val ct : Context,
    private val savedFavorites: MutableList<FavoriteEntity>,
    private var onSelectedListener: FavesListenerItem,
    private var onDeletedListener: FavesListenerItem
) : RecyclerView.Adapter<LibraryAdapter.LibraryVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryVH {
        val binding = LibraryItemsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LibraryVH(binding)
    }

    override fun onBindViewHolder(holder: LibraryVH, position: Int) {
        val saved = savedFavorites[position]
        holder.apply {
            pubNumber.text = saved.pubNumber
            pubTitle.text = saved.pubTitle

            buttonViewOption.setOnClickListener {
                val wrapper : Context = ContextThemeWrapper(ct, R.style.Theme_AFIExplorer)
                val popup = PopupMenu(wrapper, buttonViewOption)

                popup.inflate(R.menu.popup_faves)
                popup.setOnMenuItemClickListener {item ->
                    when(item.itemId){
                        R.id.menuActionCopy ->{
                            Config.save(ct, saved.pubDocumentUrl)
                        }

                        R.id.menuActionShare ->{
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, saved.pubDocumentUrl)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ContextCompat.startActivity(ct, shareIntent, null)
                        }

                        R.id.menuActionDelete ->{
                            onDeletedListener.onFavesDeletedListener(saved, position)
                        }
                    }
                    false
                }
                popup.show()
            }
        }
    }

    override fun getItemCount(): Int {
        return savedFavorites.size
    }

    // Sort by pubTitle, you can change the sorting criteria as per your requirement
    // Notify adapter that data set has changed
    fun sortFavorites() {
        savedFavorites.sortBy { it.pubTitle }
        notifyDataSetChanged()
    }

    // Sort by pubTitle, you can change the sorting criteria as per your requirement
    // Notify adapter that data set has changed
    fun sortFavoritesByNumber() {
        savedFavorites.sortBy { it.pubNumber }
        notifyDataSetChanged()
    }

    inner class LibraryVH(binding: LibraryItemsViewBinding) : RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener {
                savedFavorites[bindingAdapterPosition].let {
                    onSelectedListener.onFavesSelectedListener(it)
                }
            }
        }

        var pubNumber: TextView = binding.pubNumber
        var pubTitle: TextView = binding.pubTitle
        var buttonViewOption: ImageView = binding.textViewOptions
    }
}