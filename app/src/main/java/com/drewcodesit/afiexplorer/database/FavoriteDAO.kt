package com.drewcodesit.afiexplorer.database

import androidx.room.*


@Dao
interface FavoriteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addData(favoriteEntity: FavoriteEntity?)

    @Query("SELECT * FROM favoritelist")
    fun getFavoriteData(): MutableList<FavoriteEntity?>?

    // Added 19 Mar 2022 @ 10:12 PM...
    @Update
    fun update(favoriteEntity: FavoriteEntity?)

    @Delete
    fun delete(favoriteEntity: FavoriteEntity?)

    @Query("DELETE FROM favoritelist")
    fun deleteAll()

    @Query("SELECT EXISTS (SELECT * FROM favoritelist WHERE id=:id)")
    fun isFavorite(id: Int): Int
}