package com.drewcodesit.afiexplorer.database

import androidx.room.*
import com.drewcodesit.afiexplorer.utils.Config


@Dao
interface FavoriteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addData(favoriteEntity: FavoriteEntity?)

    @Query("SELECT * FROM ${Config.TABLE_NAME}")
    fun getFavoriteData(): MutableList<FavoriteEntity?>?

    // Added 19 Mar 2022 @ 10:12 PM...
    @Update
    fun update(favoriteEntity: FavoriteEntity?)

    @Delete
    fun delete(favoriteEntity: FavoriteEntity?)

    @Query("DELETE FROM ${Config.TABLE_NAME}")
    fun deleteAll()

    @Query("SELECT EXISTS (SELECT * FROM ${Config.TABLE_NAME} WHERE id=:id)")
    fun isFavorite(id: Int): Int
}