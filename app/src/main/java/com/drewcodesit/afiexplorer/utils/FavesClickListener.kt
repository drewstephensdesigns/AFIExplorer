package com.drewcodesit.afiexplorer.utils

import com.drewcodesit.afiexplorer.database.FavoriteEntity

interface FavesClickListener {

    fun onFavesSelectedListener(faves: FavoriteEntity)
    fun onFavesDeletedListener(faveSingleItemDelete: FavoriteEntity, position: Int)
}