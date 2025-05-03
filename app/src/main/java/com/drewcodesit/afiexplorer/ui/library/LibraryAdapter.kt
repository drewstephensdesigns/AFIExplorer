package com.drewcodesit.afiexplorer.ui.library

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.FavoriteEntity
import com.drewcodesit.afiexplorer.databinding.LibraryItemsViewBinding
import com.drewcodesit.afiexplorer.utils.Config


class LibraryAdapter(
    private val context: Context,
    private val savedFavorites: MutableList<FavoriteEntity>,
    private val onSelectItemClick: (FavoriteEntity) -> Unit,
    private val onDeleteClick: ((FavoriteEntity) -> Unit)? = null
) : RecyclerView.Adapter<LibraryAdapter.LibraryVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryVH {
        val binding = LibraryItemsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LibraryVH(binding)
    }

    override fun onBindViewHolder(holder: LibraryVH, position: Int) {
        holder.bind(savedFavorites[position])
    }

    override fun getItemCount(): Int = savedFavorites.size

    fun sortFavorites() {
        savedFavorites.sortBy { it.pubTitle }
        notifyDataSetChanged()
    }

    fun sortFavoritesByNumber() {
        savedFavorites.sortBy { it.pubNumber }
        notifyDataSetChanged()
    }

    inner class LibraryVH(private val binding: LibraryItemsViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: FavoriteEntity) {
            with(binding) {
                pubNumber.text = entity.pubNumber
                pubTitle.text = entity.pubTitle

                // Handle item click
                root.setOnClickListener { onSelectItemClick(entity) }

                // Handle popup menu
                textViewOptions.setOnClickListener { showPopupMenu(entity) }
            }
        }

        private fun showPopupMenu(entity: FavoriteEntity) {
            val wrapper = ContextThemeWrapper(context, R.style.Theme_AFIExplorer)
            val popup = PopupMenu(wrapper, binding.textViewOptions)
            popup.inflate(R.menu.popup_faves)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menuActionCopy -> { Config.save(context, entity.pubDocumentUrl); true }
                    R.id.menuActionShare -> {Config.sharePublication(context, entity.pubDocumentUrl); true }
                    R.id.menuActionDelete -> { onDeleteClick?.invoke(entity); true }
                    else -> false
                }
            }
            popup.show()
        }
    }
}