package com.drewcodesit.afiexplorer.utils

import com.drewcodesit.afiexplorer.database.FavoriteEntity

interface FavesListenerItem {

    fun onFavesSelectedListener(onOpened : FavoriteEntity)
    fun onFavesDeletedListener(onDeleted : FavoriteEntity, position: Int)
}